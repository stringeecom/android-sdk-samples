package com.stringee.callpushnotificationsample.common;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class RingtoneUtils {
    private static volatile RingtoneUtils instance;
    private static final Object lock = new Object();

    private AudioManager am;

    private MediaPlayer incomingRingtone;
    private Vibrator incomingVibrator;

    private final Context context;

    private final Uri incomingRingtoneUri;

    private int previousAudioModel;
    private boolean previousSpeaker;

    public static RingtoneUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new RingtoneUtils(context);
                }
            }
        }
        return instance;
    }

    public RingtoneUtils(Context context) {
        this.context = context.getApplicationContext();
        this.incomingRingtoneUri = Uri.parse(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE).toString());
    }


    public void startRingtoneAndVibration() {
        Utils.runOnUiThread(() -> {
            am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            previousAudioModel = am.getMode();
            previousSpeaker = am.isSpeakerphoneOn();
            am.setMode(AudioManager.MODE_RINGTONE);
            am.setSpeakerphoneOn(true);
            boolean isHeadsetPlugged = false;
            if (VERSION.SDK_INT < VERSION_CODES.M) {
                isHeadsetPlugged = am.isWiredHeadsetOn();
            } else {
                final AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
                for (AudioDeviceInfo device : devices) {
                    final int type = device.getType();
                    if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                            || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                            || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                        isHeadsetPlugged = true;
                        break;
                    }
                }
            }
            boolean needRing = am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
            boolean needVibrate = am.getRingerMode() != AudioManager.RINGER_MODE_SILENT;

            if (needRing) {
                incomingRingtone = new MediaPlayer();
                incomingRingtone.setOnPreparedListener(mediaPlayer -> incomingRingtone.start());
                incomingRingtone.setLooping(true);
                if (isHeadsetPlugged) {
                    incomingRingtone.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
                } else {
                    incomingRingtone.setAudioStreamType(AudioManager.STREAM_RING);
                }
                try {
                    incomingRingtone.setDataSource(context, incomingRingtoneUri);
                    incomingRingtone.prepareAsync();
                } catch (Exception e) {
                    if (incomingRingtone != null) {
                        incomingRingtone.release();
                        incomingRingtone = null;
                    }
                }
            }
            if (needVibrate) {
                incomingVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    incomingVibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 350, 500}, 0),
                            new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                    .build());
                } else {
                    incomingVibrator.vibrate(new long[]{0, 350, 500}, 0);
                }
            }
        });
    }

    public void stopRinging() {
        Utils.runOnUiThread(() -> {
            if (am != null) {
                am.setMode(previousAudioModel);
                am.setSpeakerphoneOn(previousSpeaker);
                am = null;
            }
            if (incomingRingtone != null) {
                incomingRingtone.stop();
                incomingRingtone.release();
                incomingRingtone = null;
            }
            if (incomingVibrator != null) {
                incomingVibrator.cancel();
                incomingVibrator = null;
            }
        });
    }
}
