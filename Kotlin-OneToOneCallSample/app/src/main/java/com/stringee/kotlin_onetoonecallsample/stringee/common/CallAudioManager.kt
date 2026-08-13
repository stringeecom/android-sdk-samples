package com.stringee.kotlin_onetoonecallsample.stringee.common

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnPreparedListener
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.stringee.common.StringeeAudioManager
import com.stringee.common.StringeeAudioManager.AudioDevice
import com.stringee.common.StringeeAudioManager.AudioManagerEvents
import com.stringee.kotlin_onetoonecallsample.stringee.common.AudioDeviceResolver.RouteAction
import java.util.Collections
import kotlin.concurrent.Volatile

/** Owns call-scoped ringtone, audio focus, and Stringee audio-device routing. */
class CallAudioManager private constructor(context: Context) {
    fun interface Listener {
        fun onAudioDeviceChanged(selected: AudioDevice?, available: MutableSet<AudioDevice?>?)
    }

    private val context: Context
    private val ringtoneUri: Uri
    private var stringeeAudioManager: StringeeAudioManager? = null
    private var listener: Listener? = null
    private var ringtone: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var platformAudioManager: AudioManager? = null
    private var previousAudioMode = 0
    private var previousSpeaker = false
    private var availableDevices = mutableSetOf<AudioDevice?>()
    private var selectedDevice: AudioDevice? = AudioDevice.NONE
    private var preferredBuiltInDevice: AudioDevice? = AudioDevice.EARPIECE
    private var videoCall = false
    private var initializing = false
    private var isRinging = false

    init {
        this.context = context.getApplicationContext()
        ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun start(videoCall: Boolean) {
        this.videoCall = videoCall
        initializing = true
        CallUtils.runOnUiThread(Runnable {
            if (stringeeAudioManager == null) {
                stringeeAudioManager = StringeeAudioManager.create(context)
            }
            stringeeAudioManager!!.start(AudioManagerEvents { selected: AudioDevice?, available: MutableSet<AudioDevice?>? ->
                this.handleAudioDeviceChanged(
                    selected,
                    available
                )
            })
        })
    }

    private fun handleAudioDeviceChanged(
        selected: AudioDevice?,
        available: MutableSet<AudioDevice?>?
    ) {
        availableDevices = if (available == null) mutableSetOf<AudioDevice?>() else
            Collections.unmodifiableSet<AudioDevice?>(HashSet<AudioDevice?>(available))
        selectedDevice = if (selected == null) AudioDevice.NONE else selected
        if (initializing) {
            initializing = false
            preferredBuiltInDevice = AudioDeviceResolver.resolveBuiltInDevice(
                videoCall, availableDevices
            )
            val initial = AudioDeviceResolver.resolveInitialDevice(
                videoCall, selectedDevice, availableDevices
            )
            selectAutomatically(initial)
        } else {
            var preferred = AudioDeviceResolver.resolveInitialDevice(
                preferredBuiltInDevice == AudioDevice.SPEAKER_PHONE,
                selectedDevice, availableDevices
            )
            if (!AudioDeviceResolver.isPeripheral(preferred)) {
                preferred = AudioDeviceResolver.resolvePreferredBuiltIn(
                    preferredBuiltInDevice, availableDevices
                )
            }
            if (preferred != AudioDevice.NONE && preferred != selectedDevice) {
                selectAutomatically(preferred)
            }
        }
        if (listener != null) {
            listener!!.onAudioDeviceChanged(selectedDevice, availableDevices)
        }
    }

    fun toggleBuiltInDevice() {
        var target: AudioDevice? = if (preferredBuiltInDevice == AudioDevice.SPEAKER_PHONE)
            AudioDevice.EARPIECE
        else
            AudioDevice.SPEAKER_PHONE
        if (!availableDevices.contains(target)) {
            target = AudioDeviceResolver.resolvePreferredBuiltIn(
                preferredBuiltInDevice, availableDevices
            )
        }
        selectUserDevice(target)
    }

    private fun selectAutomatically(device: AudioDevice?) {
        if (device == null || device == AudioDevice.NONE || !availableDevices.contains(device)) {
            return
        }
        selectedDevice = device
        if (AudioDeviceResolver.resolveRouteAction(false, device)
            == RouteAction.APPLY_BUILT_IN_ONLY
        ) {
            preferredBuiltInDevice = device
            setBuiltInDevice(device)
        }
    }

    private fun selectUserDevice(device: AudioDevice?) {
        if (stringeeAudioManager == null || device == null || device == AudioDevice.NONE || !availableDevices.contains(
                device
            )
        ) {
            return
        }
        if (AudioDeviceResolver.isBuiltIn(device)) {
            preferredBuiltInDevice = device
        }
        CallUtils.runOnUiThread(Runnable {
            if (AudioDeviceResolver.isBuiltIn(device)) {
                setBuiltInDevice(device)
            }
            stringeeAudioManager!!.selectAudioDevice(device)
        })
    }

    private fun setBuiltInDevice(device: AudioDevice?) {
        if (stringeeAudioManager != null) {
            stringeeAudioManager!!.setSpeakerphoneOn(device == AudioDevice.SPEAKER_PHONE)
        }
    }

    fun getSelectedDevice(): AudioDevice {
        return selectedDevice!!
    }

    fun stop() {
        CallUtils.runOnUiThread(Runnable {
            if (stringeeAudioManager != null) {
                stringeeAudioManager!!.stop()
                stringeeAudioManager = null
            }
            listener = null
            availableDevices = mutableSetOf<AudioDevice?>()
            selectedDevice = AudioDevice.NONE
        })
    }

    fun startRinging() {
        CallUtils.runOnUiThread(Runnable {
            if (isRinging) {
                return@Runnable
            }
            stopRingingInternal()
            isRinging = true
            platformAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            previousAudioMode = platformAudioManager!!.getMode()
            previousSpeaker = platformAudioManager!!.isSpeakerphoneOn()
            platformAudioManager!!.setMode(AudioManager.MODE_RINGTONE)
            platformAudioManager!!.setSpeakerphoneOn(true)

            var wired = false
            for (device in platformAudioManager!!.getDevices(
                AudioManager.GET_DEVICES_OUTPUTS
            )) {
                val type = device.getType()
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                    wired = true
                    break
                }
            }
            if (platformAudioManager!!.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                ringtone = MediaPlayer()
                ringtone!!.setLooping(true)
                ringtone!!.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(
                            if (wired)
                                AudioAttributes.USAGE_VOICE_COMMUNICATION
                            else
                                AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                        )
                        .setContentType(
                            if (wired)
                                AudioAttributes.CONTENT_TYPE_SPEECH
                            else
                                AudioAttributes.CONTENT_TYPE_SONIFICATION
                        )
                        .build()
                )
                ringtone!!.setOnPreparedListener(OnPreparedListener { obj: MediaPlayer? -> obj!!.start() })
                try {
                    ringtone!!.setDataSource(context, ringtoneUri)
                    ringtone!!.prepareAsync()
                } catch (exception: Exception) {
                    CallUtils.reportException(CallAudioManager::class.java, exception)
                    releaseRingtone()
                }
            }
            if (platformAudioManager!!.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(
                        Context.VIBRATOR_MANAGER_SERVICE
                    ) as VibratorManager
                    vibrator = manager.getDefaultVibrator()
                } else {
                    vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator!!.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 350, 500), 0
                        )
                    )
                } else {
                    vibrator!!.vibrate(longArrayOf(0, 350, 500), 0)
                }
            }
        })
    }

    fun stopRinging() {
        CallUtils.runOnUiThread(Runnable { this.stopRingingInternal() })
    }

    private fun stopRingingInternal() {
        isRinging = false
        if (platformAudioManager != null) {
            platformAudioManager!!.setMode(previousAudioMode)
            platformAudioManager!!.setSpeakerphoneOn(previousSpeaker)
            platformAudioManager = null
        }
        releaseRingtone()
        if (vibrator != null) {
            vibrator!!.cancel()
            vibrator = null
        }
    }

    private fun releaseRingtone() {
        if (ringtone != null) {
            try {
                if (ringtone!!.isPlaying()) {
                    ringtone!!.stop()
                }
            } catch (ignored: RuntimeException) {
                // Player can transition asynchronously while a terminal push is being handled.
            }
            ringtone!!.release()
            ringtone = null
        }
    }

    companion object {
        @Volatile
        private var instance: CallAudioManager? = null
        fun getInstance(context: Context): CallAudioManager {
            if (instance == null) {
                synchronized(CallAudioManager::class.java) {
                    if (instance == null) {
                        instance = CallAudioManager(context)
                    }
                }
            }
            return requireNotNull(instance)
        }
    }
}
