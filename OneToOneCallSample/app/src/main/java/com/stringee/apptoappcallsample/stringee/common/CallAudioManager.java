package com.stringee.apptoappcallsample.stringee.common;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.stringee.common.StringeeAudioManager;
import com.stringee.common.StringeeAudioManager.AudioDevice;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Owns call-scoped ringtone, audio focus, and Stringee audio-device routing. */
public final class CallAudioManager {
    public interface Listener {
        void onAudioDeviceChanged(AudioDevice selected, Set<AudioDevice> available);
    }

    private static volatile CallAudioManager instance;
    private final Context context;
    private final Uri ringtoneUri;
    private StringeeAudioManager stringeeAudioManager;
    private Listener listener;
    private MediaPlayer ringtone;
    private Vibrator vibrator;
    private AudioManager platformAudioManager;
    private int previousAudioMode;
    private boolean previousSpeaker;
    private Set<AudioDevice> availableDevices = Collections.emptySet();
    private AudioDevice selectedDevice = AudioDevice.NONE;
    private AudioDevice preferredBuiltInDevice = AudioDevice.EARPIECE;
    private boolean videoCall;
    private boolean initializing;
    private boolean isRinging;

    private CallAudioManager(Context context) {
        this.context = context.getApplicationContext();
        ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
    }

    public static CallAudioManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CallAudioManager.class) {
                if (instance == null) {
                    instance = new CallAudioManager(context);
                }
            }
        }
        return instance;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void start(boolean videoCall) {
        this.videoCall = videoCall;
        initializing = true;
        CallUtils.runOnUiThread(() -> {
            if (stringeeAudioManager == null) {
                stringeeAudioManager = StringeeAudioManager.create(context);
            }
            stringeeAudioManager.start(this::handleAudioDeviceChanged);
        });
    }

    private void handleAudioDeviceChanged(AudioDevice selected, Set<AudioDevice> available) {
        availableDevices = available == null ? Collections.emptySet()
                : Collections.unmodifiableSet(new HashSet<>(available));
        selectedDevice = selected == null ? AudioDevice.NONE : selected;
        if (initializing) {
            initializing = false;
            preferredBuiltInDevice = AudioDeviceResolver.resolveBuiltInDevice(
                    videoCall, availableDevices);
            AudioDevice initial = AudioDeviceResolver.resolveInitialDevice(
                    videoCall, selectedDevice, availableDevices);
            selectAutomatically(initial);
        } else {
            AudioDevice preferred = AudioDeviceResolver.resolveInitialDevice(
                    preferredBuiltInDevice == AudioDevice.SPEAKER_PHONE,
                    selectedDevice, availableDevices);
            if (!AudioDeviceResolver.isPeripheral(preferred)) {
                preferred = AudioDeviceResolver.resolvePreferredBuiltIn(
                        preferredBuiltInDevice, availableDevices);
            }
            if (preferred != AudioDevice.NONE && preferred != selectedDevice) {
                selectAutomatically(preferred);
            }
        }
        if (listener != null) {
            listener.onAudioDeviceChanged(selectedDevice, availableDevices);
        }
    }

    public void toggleBuiltInDevice() {
        AudioDevice target = preferredBuiltInDevice == AudioDevice.SPEAKER_PHONE
                ? AudioDevice.EARPIECE : AudioDevice.SPEAKER_PHONE;
        if (!availableDevices.contains(target)) {
            target = AudioDeviceResolver.resolvePreferredBuiltIn(
                    preferredBuiltInDevice, availableDevices);
        }
        selectUserDevice(target);
    }

    private void selectAutomatically(AudioDevice device) {
        if (device == null || device == AudioDevice.NONE || !availableDevices.contains(device)) {
            return;
        }
        selectedDevice = device;
        if (AudioDeviceResolver.resolveRouteAction(false, device)
                == AudioDeviceResolver.RouteAction.APPLY_BUILT_IN_ONLY) {
            preferredBuiltInDevice = device;
            setBuiltInDevice(device);
        }
    }

    private void selectUserDevice(AudioDevice device) {
        if (stringeeAudioManager == null || device == null || device == AudioDevice.NONE
                || !availableDevices.contains(device)) {
            return;
        }
        if (AudioDeviceResolver.isBuiltIn(device)) {
            preferredBuiltInDevice = device;
        }
        CallUtils.runOnUiThread(() -> {
            if (AudioDeviceResolver.isBuiltIn(device)) {
                setBuiltInDevice(device);
            }
            stringeeAudioManager.selectAudioDevice(device);
        });
    }

    private void setBuiltInDevice(AudioDevice device) {
        if (stringeeAudioManager != null) {
            stringeeAudioManager.setSpeakerphoneOn(device == AudioDevice.SPEAKER_PHONE);
        }
    }

    public AudioDevice getSelectedDevice() {
        return selectedDevice;
    }

    public void stop() {
        CallUtils.runOnUiThread(() -> {
            if (stringeeAudioManager != null) {
                stringeeAudioManager.stop();
                stringeeAudioManager = null;
            }
            listener = null;
            availableDevices = Collections.emptySet();
            selectedDevice = AudioDevice.NONE;
        });
    }

    public void startRinging() {
        CallUtils.runOnUiThread(() -> {
            if (isRinging) {
                return;
            }
            stopRingingInternal();
            isRinging = true;
            platformAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            previousAudioMode = platformAudioManager.getMode();
            previousSpeaker = platformAudioManager.isSpeakerphoneOn();
            platformAudioManager.setMode(AudioManager.MODE_RINGTONE);
            platformAudioManager.setSpeakerphoneOn(true);

            boolean wired = false;
            for (AudioDeviceInfo device : platformAudioManager.getDevices(
                    AudioManager.GET_DEVICES_OUTPUTS)) {
                int type = device.getType();
                if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                        || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                    wired = true;
                    break;
                }
            }
            if (platformAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                ringtone = new MediaPlayer();
                ringtone.setLooping(true);
                ringtone.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(wired ? AudioAttributes.USAGE_VOICE_COMMUNICATION
                                : AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(wired ? AudioAttributes.CONTENT_TYPE_SPEECH
                                : AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build());
                ringtone.setOnPreparedListener(MediaPlayer::start);
                try {
                    ringtone.setDataSource(context, ringtoneUri);
                    ringtone.prepareAsync();
                } catch (Exception exception) {
                    CallUtils.reportException(CallAudioManager.class, exception);
                    releaseRingtone();
                }
            }
            if (platformAudioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    VibratorManager manager = (VibratorManager) context.getSystemService(
                            Context.VIBRATOR_MANAGER_SERVICE);
                    vibrator = manager.getDefaultVibrator();
                } else {
                    vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(
                            new long[]{0, 350, 500}, 0));
                } else {
                    vibrator.vibrate(new long[]{0, 350, 500}, 0);
                }
            }
        });
    }

    public void stopRinging() {
        CallUtils.runOnUiThread(this::stopRingingInternal);
    }

    private void stopRingingInternal() {
        isRinging = false;
        if (platformAudioManager != null) {
            platformAudioManager.setMode(previousAudioMode);
            platformAudioManager.setSpeakerphoneOn(previousSpeaker);
            platformAudioManager = null;
        }
        releaseRingtone();
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
    }

    private void releaseRingtone() {
        if (ringtone != null) {
            try {
                if (ringtone.isPlaying()) {
                    ringtone.stop();
                }
            } catch (RuntimeException ignored) {
                // Player can transition asynchronously while a terminal push is being handled.
            }
            ringtone.release();
            ringtone = null;
        }
    }
}
