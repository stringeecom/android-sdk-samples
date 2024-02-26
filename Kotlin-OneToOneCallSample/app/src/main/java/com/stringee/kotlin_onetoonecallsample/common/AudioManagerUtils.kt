package com.stringee.kotlin_onetoonecallsample.common

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.VibrationEffect
import android.os.Vibrator
import com.stringee.common.StringeeAudioManager
import com.stringee.common.StringeeAudioManager.AudioDevice


@Suppress("DEPRECATION")
class AudioManagerUtils private constructor(private val applicationContext: Context) {
    private var audioEvents: OnAudioEvents? = null
    private var audioManager: StringeeAudioManager? = null
    private var am: AudioManager? = null
    private val incomingRingtone: MediaPlayer = MediaPlayer()
    private var incomingVibrator: Vibrator? = null
    private val incomingRingtoneUri: Uri =
        Uri.parse(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString())
    private var previousAudioModel = 0
    private var previousSpeaker = false

    companion object {
        @Volatile
        private var instance: AudioManagerUtils? = null
        fun getInstance(context: Context): AudioManagerUtils {
            return instance ?: synchronized(this) {
                instance
                    ?: AudioManagerUtils(context.applicationContext).also {
                        instance = it
                    }
            }
        }
    }

    fun startAudioManager() {
        Utils.runOnUiThread {
            if (audioManager == null) {
                audioManager = StringeeAudioManager.create(applicationContext)
            }
            audioManager?.start { selectedAudioDevice: AudioDevice, _: Set<AudioDevice> ->
                audioEvents?.onAudioEvents(
                    selectedAudioDevice
                )
            }
        }
    }

    fun setAudioEvents(onAudioEvents: OnAudioEvents) {
        Utils.runOnUiThread { audioEvents = onAudioEvents }
    }

    interface OnAudioEvents {
        fun onAudioEvents(selectedAudioDevice: AudioDevice)
    }

    fun stopAudioManager() {
        Utils.runOnUiThread {
            audioManager?.stop()
        }
    }

    fun startRingtoneAndVibration() {
        Utils.runOnUiThread {
            am = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            previousAudioModel = am?.mode!!
            previousSpeaker = am?.isSpeakerphoneOn == true
            am?.mode = AudioManager.MODE_RINGTONE
            am?.isSpeakerphoneOn = true
            var isHeadsetPlugged = false
            val devices =
                am?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            if (!devices.isNullOrEmpty()) {
                for (device in devices) {
                    val type = device.type
                    if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                        isHeadsetPlugged = true
                        break
                    }
                }
            }
            val needRing = am?.ringerMode == AudioManager.RINGER_MODE_NORMAL
            val needVibrate =
                am?.ringerMode != AudioManager.RINGER_MODE_SILENT
            if (needRing) {
                if (incomingRingtone.isPlaying || incomingRingtone.isLooping) {
                    incomingRingtone.stop()
                    incomingRingtone.reset()
                }
                incomingRingtone.setOnPreparedListener { incomingRingtone.start() }
                incomingRingtone.isLooping = true
                if (isHeadsetPlugged) {
                    incomingRingtone.setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
                } else {
                    incomingRingtone.setAudioStreamType(AudioManager.STREAM_RING)
                }
                try {
                    incomingRingtone.setDataSource(applicationContext, incomingRingtoneUri)
                    incomingRingtone.prepareAsync()
                } catch (e: Exception) {
                    incomingRingtone.stop()
                    incomingRingtone.reset()
                }
            }
            if (needVibrate) {
                incomingVibrator =
                    applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    incomingVibrator?.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(
                                0,
                                350,
                                500
                            ), 0
                        ),
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build()
                    )
                } else {
                    incomingVibrator?.vibrate(longArrayOf(0, 350, 500), 0)
                }
            }
        }
    }

    fun stopRinging() {
        Utils.runOnUiThread {
            am?.mode = previousAudioModel
            am?.isSpeakerphoneOn = previousSpeaker
            incomingRingtone.stop()
            incomingRingtone.reset()
            incomingVibrator?.cancel()
        }
    }

    fun setSpeakerphoneOn(on: Boolean) {
        Utils.runOnUiThread {
            audioManager?.setSpeakerphoneOn(on)
        }
    }
}
