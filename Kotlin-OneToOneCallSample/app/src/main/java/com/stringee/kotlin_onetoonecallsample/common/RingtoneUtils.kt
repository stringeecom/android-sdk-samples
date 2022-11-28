package com.stringee.kotlin_onetoonecallsample.common

import android.content.Context
import android.media.*
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.VibrationEffect
import android.os.Vibrator

class RingtoneUtils(context: Context) {
    private var am: AudioManager? = null
    private var incomingRingtone: MediaPlayer? = null
    private var incomingVibrator: Vibrator? = null
    private val context: Context
    private val incomingRingtoneUri: Uri
    private var previousAudioModel = 0
    private var previousSpeaker = false

    init {
        this.context = context.applicationContext
        incomingRingtoneUri =
            Uri.parse(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString())
    }

    fun startRingtoneAndVibration() {
        Utils.runOnUiThread {
            am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            previousAudioModel = am!!.mode
            previousSpeaker = am!!.isSpeakerphoneOn
            am!!.mode = AudioManager.MODE_RINGTONE
            am!!.isSpeakerphoneOn = true
            var isHeadsetPlugged = false
            if (VERSION.SDK_INT < VERSION_CODES.M) {
                isHeadsetPlugged = am!!.isWiredHeadsetOn
            } else {
                val devices =
                    am!!.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                for (device in devices) {
                    val type = device.type
                    if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || type == AudioDeviceInfo.TYPE_USB_DEVICE
                    ) {
                        isHeadsetPlugged = true
                        break
                    }
                }
            }
            val needRing = am!!.ringerMode == AudioManager.RINGER_MODE_NORMAL
            val needVibrate =
                am!!.ringerMode != AudioManager.RINGER_MODE_SILENT
            if (needRing) {
                incomingRingtone = MediaPlayer()
                incomingRingtone!!.setOnPreparedListener { mediaPlayer: MediaPlayer? -> incomingRingtone!!.start() }
                incomingRingtone!!.isLooping = true
                if (isHeadsetPlugged) {
                    incomingRingtone!!.setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
                } else {
                    incomingRingtone!!.setAudioStreamType(AudioManager.STREAM_RING)
                }
                try {
                    incomingRingtone!!.setDataSource(context, incomingRingtoneUri)
                    incomingRingtone!!.prepareAsync()
                } catch (e: Exception) {
                    if (incomingRingtone != null) {
                        incomingRingtone!!.release()
                        incomingRingtone = null
                    }
                }
            }
            if (needVibrate) {
                incomingVibrator =
                    context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    incomingVibrator!!.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 350, 500), 0),
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build()
                    )
                } else {
                    incomingVibrator!!.vibrate(longArrayOf(0, 350, 500), 0)
                }
            }
        }
    }

    fun stopRinging() {
        Utils.runOnUiThread {
            if (am != null) {
                am!!.mode = previousAudioModel
                am!!.isSpeakerphoneOn = previousSpeaker
                am = null
            }
            if (incomingRingtone != null) {
                incomingRingtone!!.stop()
                incomingRingtone!!.release()
                incomingRingtone = null
            }
            if (incomingVibrator != null) {
                incomingVibrator!!.cancel()
                incomingVibrator = null
            }
        }
    }

    companion object {
        @Volatile
        private var instance: RingtoneUtils? = null
        private val lock = Any()
        fun getInstance(context: Context): RingtoneUtils? {
            if (instance == null) {
                synchronized(lock) {
                    if (instance == null) {
                        instance = RingtoneUtils(context)
                    }
                }
            }
            return instance
        }
    }
}
