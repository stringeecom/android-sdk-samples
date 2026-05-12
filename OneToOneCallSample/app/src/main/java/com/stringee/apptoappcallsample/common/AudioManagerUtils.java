package com.stringee.apptoappcallsample.common;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioAttributes.Builder;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.stringee.common.StringeeAudioManager;
import com.stringee.common.StringeeAudioManager.AudioManagerEvents;

public class AudioManagerUtils {
    private static volatile AudioManagerUtils instance;
    private AudioManagerEvents audioEvents;
    private StringeeAudioManager audioManager;
    private AudioManager am;

    private MediaPlayer incomingRingtone;
    private Vibrator incomingVibrator;

    private final Context context;

    private final Uri incomingRingtoneUri;

    private int previousAudioModel;
    private boolean previousSpeaker;

    public static AudioManagerUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (AudioManagerUtils.class) {
                if (instance == null) {
                    instance = new AudioManagerUtils(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public AudioManagerUtils(Context context) {
        this.context = context.getApplicationContext();
        this.incomingRingtoneUri = Uri.parse(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
    }

    public void startAudioManager() {
        Utils.runOnUiThread(() -> {
            if (audioManager == null) {
                audioManager = StringeeAudioManager.create(context);
            }

            audioManager.start((selectedAudioDevice, availableAudioDevices) -> {
                if (audioEvents != null) {
                    audioEvents.onAudioDeviceChanged(selectedAudioDevice, availableAudioDevices);
                }
            });
        });
    }

    public void setAudioEvents(AudioManagerEvents onAudioEvents) {
        Utils.runOnUiThread(() -> audioEvents = onAudioEvents);
    }

    public void stopAudioManager() {
        Utils.runOnUiThread(() -> {
            if (audioManager != null) {
                audioManager.stop();
                audioManager = null;
            }
        });
    }

    public void startRingtoneAndVibration() {
        Utils.runOnUiThread(() -> {
            am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            previousAudioModel = am.getMode();
            previousSpeaker = am.isSpeakerphoneOn();
            am.setMode(AudioManager.MODE_RINGTONE);
            am.setSpeakerphoneOn(true);
            boolean isHeadsetPlugged = false;
            final AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo device : devices) {
                final int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                        type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                        type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                    isHeadsetPlugged = true;
                    break;
                }
            }
            boolean needRing = am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
            boolean needVibrate = am.getRingerMode() != AudioManager.RINGER_MODE_SILENT;

            if (needRing) {
                incomingRingtone = new MediaPlayer();
                incomingRingtone.setOnPreparedListener(mediaPlayer -> {
                    if (incomingRingtone != null) {
                        incomingRingtone.start();
                    }
                });
                incomingRingtone.setLooping(true);
                AudioAttributes attrs;
                if (isHeadsetPlugged) {
                    attrs = new Builder().setUsage(
                            AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(
                            AudioAttributes.CONTENT_TYPE_SPEECH).build();
                } else {
                    attrs = new Builder().setUsage(
                            AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(
                            AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
                }
                incomingRingtone.setAudioAttributes(attrs);
                try {
                    incomingRingtone.setDataSource(context, incomingRingtoneUri);
                    incomingRingtone.prepareAsync();
                } catch (Exception e) {
                    if (incomingRingtone != null) {
                        incomingRingtone.stop();
                        incomingRingtone.release();
                        incomingRingtone = null;
                    }
                }
            }
            if (needVibrate) {
                if (VERSION.SDK_INT >= VERSION_CODES.S) {
                    VibratorManager vm = (VibratorManager) context.getSystemService(
                            Context.VIBRATOR_MANAGER_SERVICE);
                    incomingVibrator = vm.getDefaultVibrator();
                } else {
                    incomingVibrator = (Vibrator) context.getSystemService(
                            Context.VIBRATOR_SERVICE);
                }
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    incomingVibrator.vibrate(
                            VibrationEffect.createWaveform(new long[]{0, 350, 500}, 0),
                            new Builder().setContentType(
                                    AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(
                                    AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build()
                    );
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

    public void setSpeakerphoneOn(boolean on) {
        Utils.runOnUiThread(() -> {
            if (audioManager != null) {
                audioManager.setSpeakerphoneOn(on);
            }
        });
    }
}
