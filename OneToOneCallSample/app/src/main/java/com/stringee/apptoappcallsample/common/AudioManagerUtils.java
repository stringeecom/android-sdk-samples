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

import com.stringee.common.StringeeAudioManager;
import com.stringee.common.StringeeAudioManager.AudioDevice;

public class AudioManagerUtils {
    private static volatile AudioManagerUtils instance;
    private OnAudioEvents audioEvents;
    private StringeeAudioManager audioManager;
    private AudioManager am;

    private final MediaPlayer incomingRingtone;
    private Vibrator incomingVibrator;

    private final Context context;

    private final Uri incomingRingtoneUri;

    private int previousAudioModel;
    private boolean previousSpeaker;

    public static AudioManagerUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (AudioManagerUtils.class) {
                if (instance == null) {
                    instance = new AudioManagerUtils(context);
                }
            }
        }
        return instance;
    }

    public AudioManagerUtils(Context context) {
        this.context = context.getApplicationContext();
        this.incomingRingtoneUri = Uri.parse(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString());
        this.incomingRingtone = new MediaPlayer();
    }

    public void startAudioManager() {
        Utils.runOnUiThread(() -> {
            if (audioManager == null) {
                audioManager = StringeeAudioManager.create(context);
            }

            audioManager.start((selectedAudioDevice, availableAudioDevices) -> audioEvents.onAudioEvents(selectedAudioDevice));
        });
    }

    public void setAudioEvents(OnAudioEvents onAudioEvents) {
        Utils.runOnUiThread(() -> audioEvents = onAudioEvents);
    }

    public interface OnAudioEvents {
        void onAudioEvents(AudioDevice selectedAudioDevice);
    }

    public void stopAudioManager() {
        Utils.runOnUiThread(() -> {
            if (audioManager != null) {
                audioManager.stop();
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
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                    isHeadsetPlugged = true;
                    break;
                }
            }
            boolean needRing = am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
            boolean needVibrate = am.getRingerMode() != AudioManager.RINGER_MODE_SILENT;

            if (needRing) {
                if (incomingRingtone.isPlaying() || incomingRingtone.isLooping()) {
                    incomingRingtone.stop();
                    incomingRingtone.reset();
                }
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
                    incomingRingtone.stop();
                    incomingRingtone.reset();
                }
            }
            if (needVibrate) {
                incomingVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    incomingVibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 350, 500}, 0), new Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
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
            }
            if (incomingRingtone != null) {
                incomingRingtone.stop();
                incomingRingtone.reset();
            }
            if (incomingVibrator != null) {
                incomingVibrator.cancel();
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
