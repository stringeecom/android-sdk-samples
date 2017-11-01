/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.stringee.conferencecallsample.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * VCAudioManager manages all audio related parts of the AppRTC demo.
 */
public class StringeeAudioManager {
    private static final String TAG = "Stringee";
    private static final String SPEAKERPHONE_AUTO = "auto";
    private static final String SPEAKERPHONE_TRUE = "true";
    private static final String SPEAKERPHONE_FALSE = "false";

    /**
     * AudioDevice is the names of possible audio devices that we currently
     * support.
     */
    public enum AudioDevice {
        SPEAKER_PHONE, WIRED_HEADSET, EARPIECE, BLUETOOTH, NONE
    }

    /**
     * AudioManager state.
     */
    public enum AudioManagerState {
        UNINITIALIZED,
        PREINITIALIZED,
        RUNNING,
    }

    /**
     * Selected audio device change event.
     */
    public static interface AudioManagerEvents {
        // Callback fired once audio device is changed or list of available audio devices changed.
        void onAudioDeviceChanged(
                AudioDevice selectedAudioDevice, Set<AudioDevice> availableAudioDevices);
    }

    private final Context apprtcContext;
    private AudioManager audioManager;

    private AudioManagerEvents audioManagerEvents;
    private AudioManagerState amState;
    private int savedAudioMode = AudioManager.MODE_INVALID;
    private boolean savedIsSpeakerPhoneOn = false;
    //    private boolean savedIsMicrophoneMute = false;
    private boolean hasWiredHeadset = false;
    private boolean lastMicrophoneMute;
    private boolean lastSpeakerphoneOn;


    // Default audio device; speaker phone for video calls or earpiece for audio
    // only calls.
    private AudioDevice defaultAudioDevice;

    // Contains the currently selected audio device.
    // This device is changed automatically using a certain scheme where e.g.
    // a wired headset "wins" over speaker phone. It is also possible for a
    // user to explicitly select a device (and overrid any predefined scheme).
    // See |userSelectedAudioDevice| for details.
    private AudioDevice selectedAudioDevice;

    // Contains the user-selected audio device which overrides the predefined
    // selection scheme.
    // TODO(henrika): always set to AudioDevice.NONE today. Add support for
    // explicit selection based on choice by userSelectedAudioDevice.
    private AudioDevice userSelectedAudioDevice;

    // Contains speakerphone setting: auto, true or false
    private final String useSpeakerphone;

    // Proximity sensor object. It measures the proximity of an object in cm
    // relative to the view screen of a device and can therefore be used to
    // assist device switching (close to ear <=> use headset earpiece if
    // available, far from ear <=> use speaker phone).
//    private VCProximitySensor proximitySensor = null;

    // Handles all tasks related to Bluetooth headset devices.
    private final StringeeBluetoothManager bluetoothManager;

    // Contains a list of available audio devices. A Set collection is used to
    // avoid duplicate elements.
    private Set<AudioDevice> audioDevices = new HashSet<AudioDevice>();

    // Broadcast receiver for wired headset intent broadcasts.
    private BroadcastReceiver wiredHeadsetReceiver;

    // Callback method for changes in audio focus.
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    /**
     * This method is called when the proximity sensor reports a state change,
     * e.g. from "NEAR to FAR" or from "FAR to NEAR".
     */
//    private void onProximitySensorChangedState() {
//        if (!useSpeakerphone.equals(SPEAKERPHONE_AUTO)) {
//            return;
//        }
//
//        // The proximity sensor should only be activated when there are exactly two
//        // available audio devices.
//        if (audioDevices.size() == 2 && audioDevices.contains(VCAudioManager.AudioDevice.EARPIECE)
//                && audioDevices.contains(VCAudioManager.AudioDevice.SPEAKER_PHONE)) {
//            if (proximitySensor.sensorReportsNearState()) {
//                // Sensor reports that a "handset is being held up to a person's ear",
//                // or "something is covering the light sensor".
//                setAudioDeviceInternal(VCAudioManager.AudioDevice.EARPIECE);
//            } else {
//                // Sensor reports that a "handset is removed from a person's ear", or
//                // "the light sensor is no longer covered".
//                setAudioDeviceInternal(VCAudioManager.AudioDevice.SPEAKER_PHONE);
//            }
//        }
//    }

    /* Receiver which handles changes in wired headset availability. */
    private class WiredHeadsetReceiver extends BroadcastReceiver {
        private static final int STATE_UNPLUGGED = 0;
        private static final int STATE_PLUGGED = 1;
        private static final int HAS_NO_MIC = 0;
        private static final int HAS_MIC = 1;

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("state", STATE_UNPLUGGED);
            int microphone = intent.getIntExtra("microphone", HAS_NO_MIC);
            String name = intent.getStringExtra("name");
            Log.e(TAG, "WiredHeadsetReceiver.onReceive" + StringeeUtils.getThreadInfo() + ": "
                    + "a=" + intent.getAction() + ", s="
                    + (state == STATE_UNPLUGGED ? "unplugged" : "plugged") + ", m="
                    + (microphone == HAS_MIC ? "mic" : "no mic") + ", n=" + name + ", sb="
                    + isInitialStickyBroadcast());
            hasWiredHeadset = (state == STATE_PLUGGED);
            updateAudioDeviceState();
        }
    }

    ;

    /**
     * Construction.
     */
    public static StringeeAudioManager create(Context context, boolean isSpeakerOn) {
        return new StringeeAudioManager(context, isSpeakerOn);
    }

    private StringeeAudioManager(Context context, boolean isSpeakerOn) {
        apprtcContext = context;
        audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
        bluetoothManager = StringeeBluetoothManager.create(context, this);
        wiredHeadsetReceiver = new WiredHeadsetReceiver();
        amState = AudioManagerState.UNINITIALIZED;

        if (isSpeakerOn) {
            useSpeakerphone = SPEAKERPHONE_AUTO;
        } else {
            useSpeakerphone = SPEAKERPHONE_FALSE;
        }
        Log.e(TAG, "useSpeakerphone: " + useSpeakerphone);
        if (useSpeakerphone.equals(SPEAKERPHONE_FALSE)) {
            defaultAudioDevice = AudioDevice.EARPIECE;
        } else {
            defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
        }

        Log.e(TAG, "defaultAudioDevice: " + defaultAudioDevice);
        StringeeUtils.logDeviceInfo(TAG);
    }

    public void start(AudioManagerEvents audioManagerEvents, AudioManager.OnAudioFocusChangeListener audioFocusChangeListener) {
        if (amState == AudioManagerState.RUNNING) {
            Log.e(TAG, "AudioManager is already active");
            return;
        }

        Log.e(TAG, "AudioManager starts...");
        this.audioManagerEvents = audioManagerEvents;
        this.audioFocusChangeListener = audioFocusChangeListener;
        amState = AudioManagerState.RUNNING;

        // Store current audio state so we can restore it when stop() is called.
        savedAudioMode = audioManager.getMode();
        savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
//        savedIsMicrophoneMute = audioManager.isMicrophoneMute();
        hasWiredHeadset = hasWiredHeadset();

        // Request audio playout focus (without ducking) and install listener for changes in focus.
        int result = audioManager.requestAudioFocus(audioFocusChangeListener,
                AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e(TAG, "Audio focus request granted for VOICE_CALL streams");
        } else {
            Log.e(TAG, "Audio focus request failed");
        }

        // Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
        // required to be in this mode when playout and/or recording starts for
        // best possible VoIP performance.
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        // Set initial device states.
        userSelectedAudioDevice = AudioDevice.NONE;
        selectedAudioDevice = AudioDevice.NONE;
        audioDevices.clear();

        // Initialize and start Bluetooth if a BT device is available or initiate
        // detection of new (enabled) BT devices.
        bluetoothManager.start();

        // Do initial selection of audio device. This setting can later be changed
        // either by adding/removing a BT or wired headset or by covering/uncovering
        // the proximity sensor.
        updateAudioDeviceState();

        // Register receiver for broadcast intents related to adding/removing a
        // wired headset.
        registerReceiver(wiredHeadsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        Log.e(TAG, "AudioManager started");
    }

    public void stop() {
        Log.e(TAG, "stop");

        if (amState != AudioManagerState.RUNNING) {
            Log.e(TAG, "Trying to stop AudioManager in incorrect state: " + amState);
            return;
        }
        amState = AudioManagerState.UNINITIALIZED;

        unregisterReceiver(wiredHeadsetReceiver);

        bluetoothManager.stop();

        // Restore previously stored audio states.
        setSpeakerphoneOn(savedIsSpeakerPhoneOn);
//        setMicrophoneMute(savedIsMicrophoneMute);
        audioManager.setMode(savedAudioMode);

        // Abandon audio focus. Gives the previous focus owner, if any, focus.
        audioManager.abandonAudioFocus(audioFocusChangeListener);
        audioFocusChangeListener = null;
        Log.e(TAG, "Abandoned audio focus for VOICE_CALL streams");

        audioManagerEvents = null;
        Log.e(TAG, "AudioManager stopped");
    }

    /**
     * Changes selection of the currently active audio device.
     */
    public void setAudioDeviceInternal(AudioDevice device) {
        Log.e(TAG, "setAudioDeviceInternal(device=" + device + ")");
        StringeeUtils.assertIsTrue(audioDevices.contains(device));

        switch (device) {
            case SPEAKER_PHONE:
                setSpeakerphoneOn(true);
                break;
            case EARPIECE:
                setSpeakerphoneOn(false);
                break;
            case WIRED_HEADSET:
                setSpeakerphoneOn(false);
                break;
            case BLUETOOTH:
                setSpeakerphoneOn(false);
                break;
            default:
                Log.e(TAG, "Invalid audio device selection");
                break;
        }
        selectedAudioDevice = device;
    }

    /**
     * Changes default audio device.
     * TODO(henrika): add usage of this method in the AppRTCMobile client.
     */
    public void setDefaultAudioDevice(AudioDevice defaultDevice) {

        switch (defaultDevice) {
            case SPEAKER_PHONE:
                defaultAudioDevice = defaultDevice;
                break;
            case EARPIECE:
                if (hasEarpiece()) {
                    defaultAudioDevice = defaultDevice;
                } else {
                    defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
                }
                break;
            default:
                Log.e(TAG, "Invalid default audio device selection");
                break;
        }
        Log.e(TAG, "setDefaultAudioDevice(device=" + defaultAudioDevice + ")");
        updateAudioDeviceState();
    }

    /**
     * Changes selection of the currently active audio device.
     */
    public void selectAudioDevice(AudioDevice device) {

        if (!audioDevices.contains(device)) {
            Log.e(TAG, "Can not select " + device + " from available " + audioDevices);
        }
        userSelectedAudioDevice = device;
        updateAudioDeviceState();
    }

    /**
     * Returns current set of available/selectable audio devices.
     */
    public Set<AudioDevice> getAudioDevices() {

        return Collections.unmodifiableSet(new HashSet<AudioDevice>(audioDevices));
    }

    /**
     * Returns the currently selected audio device.
     */
    public AudioDevice getSelectedAudioDevice() {

        return selectedAudioDevice;
    }

    /**
     * Helper method for receiver registration.
     */
    private void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        try {
            apprtcContext.registerReceiver(receiver, filter);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Helper method for unregistration of an existing receiver.
     */
    private void unregisterReceiver(BroadcastReceiver receiver) {
        try {
            apprtcContext.unregisterReceiver(receiver);
        } catch (Exception ex) {

        }
    }

    /**
     * Sets the speaker phone mode.
     */
    public void setSpeakerphoneOn(boolean on) {
        boolean wasOn = audioManager.isSpeakerphoneOn();
        if (wasOn == on) {
            return;
        }
        audioManager.setSpeakerphoneOn(on);
    }

//    /**
//     * Sets the microphone mute state.
//     */
//    public void setMicrophoneMute(boolean on) {
//        boolean wasMuted = audioManager.isMicrophoneMute();
//        if (wasMuted == on) {
//            return;
//        }
//        audioManager.setMicrophoneMute(on);
//    }

    /**
     * Gets the current earpiece state.
     */
    private boolean hasEarpiece() {
        return apprtcContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    /**
     * Checks whether a wired headset is connected or not.
     * This is not a valid indication that audio playback is actually over
     * the wired headset as audio routing depends on other conditions. We
     * only use it as an early indicator (during initialization) of an attached
     * wired headset.
     */
    @Deprecated
    private boolean hasWiredHeadset() {
        return audioManager.isWiredHeadsetOn();
    }

    /**
     * Updates list of possible audio devices and make new device selection.
     * TODO(henrika): add unit test to verify all state transitions.
     */
    public void updateAudioDeviceState() {

        Log.e(TAG, "--- updateAudioDeviceState: "
                + "wired headset=" + hasWiredHeadset + ", "
                + "BT state=" + bluetoothManager.getState());
        Log.e(TAG, "Device status: "
                + "available=" + audioDevices + ", "
                + "selected=" + selectedAudioDevice + ", "
                + "user selected=" + userSelectedAudioDevice);

        // Check if any Bluetooth headset is connected. The internal BT state will
        // change accordingly.
        // TODO(henrika): perhaps wrap required state into BT manager.
        if (bluetoothManager.getState() == StringeeBluetoothManager.State.HEADSET_AVAILABLE
                || bluetoothManager.getState() == StringeeBluetoothManager.State.HEADSET_UNAVAILABLE
                || bluetoothManager.getState() == StringeeBluetoothManager.State.SCO_DISCONNECTING) {
            bluetoothManager.updateDevice();
        }

        // Update the set of available audio devices.
        Set<AudioDevice> newAudioDevices = new HashSet<>();

        if (bluetoothManager.getState() == StringeeBluetoothManager.State.SCO_CONNECTED
                || bluetoothManager.getState() == StringeeBluetoothManager.State.SCO_CONNECTING
                || bluetoothManager.getState() == StringeeBluetoothManager.State.HEADSET_AVAILABLE) {
            newAudioDevices.add(AudioDevice.BLUETOOTH);
        }

        if (hasWiredHeadset) {
            // If a wired headset is connected, then it is the only possible option.
            newAudioDevices.add(AudioDevice.WIRED_HEADSET);
        } else {
            // No wired headset, hence the audio-device list can contain speaker
            // phone (on a tablet), or speaker phone and earpiece (on mobile phone).
            newAudioDevices.add(AudioDevice.SPEAKER_PHONE);
            if (hasEarpiece()) {
                newAudioDevices.add(AudioDevice.EARPIECE);
            }
        }
        // Store state which is set to true if the device list has changed.
        boolean audioDeviceSetUpdated = !audioDevices.equals(newAudioDevices);
        // Update the existing audio device set.
        audioDevices = newAudioDevices;
        // Correct user selected audio devices if needed.
        if (bluetoothManager.getState() == StringeeBluetoothManager.State.HEADSET_UNAVAILABLE
                && userSelectedAudioDevice == AudioDevice.BLUETOOTH) {
            // If BT is not available, it can't be the user selection.
            userSelectedAudioDevice = AudioDevice.NONE;
        }
        if (hasWiredHeadset && userSelectedAudioDevice == AudioDevice.SPEAKER_PHONE) {
            // If user selected speaker phone, but then plugged wired headset then make
            // wired headset as user selected device.
            userSelectedAudioDevice = AudioDevice.WIRED_HEADSET;
        }
        if (!hasWiredHeadset && userSelectedAudioDevice == AudioDevice.WIRED_HEADSET) {
            // If user selected wired headset, but then unplugged wired headset then make
            // speaker phone as user selected device.
            userSelectedAudioDevice = AudioDevice.SPEAKER_PHONE;
        }

        // Need to start Bluetooth if it is available and user either selected it explicitly or
        // user did not select any output device.
        boolean needBluetoothAudioStart =
                bluetoothManager.getState() == StringeeBluetoothManager.State.HEADSET_AVAILABLE
                        && (userSelectedAudioDevice == AudioDevice.NONE
                        || userSelectedAudioDevice == AudioDevice.BLUETOOTH);

        // Need to stop Bluetooth audio if user selected different device and
        // Bluetooth SCO connection is established or in the process.
        boolean needBluetoothAudioStop =
                (bluetoothManager.getState() == StringeeBluetoothManager.State.SCO_CONNECTED
                        || bluetoothManager.getState() == StringeeBluetoothManager.State.SCO_CONNECTING)
                        && (userSelectedAudioDevice != AudioDevice.NONE
                        && userSelectedAudioDevice != AudioDevice.BLUETOOTH);

        if (bluetoothManager.getState() == StringeeBluetoothManager.State.HEADSET_AVAILABLE
                || bluetoothManager.getState() == StringeeBluetoothManager.State.SCO_CONNECTING
                || bluetoothManager.getState() == StringeeBluetoothManager.State.SCO_CONNECTED) {
            Log.e(TAG, "Need BT audio: start=" + needBluetoothAudioStart + ", "
                    + "stop=" + needBluetoothAudioStop + ", "
                    + "BT state=" + bluetoothManager.getState());
        }

        // Start or stop Bluetooth SCO connection given states set earlier.
        if (needBluetoothAudioStop) {
            bluetoothManager.stopScoAudio();
            bluetoothManager.updateDevice();
        }

        if (needBluetoothAudioStart && !needBluetoothAudioStop) {
            // Attempt to start Bluetooth SCO audio (takes a few second to start).
            if (!bluetoothManager.startScoAudio()) {
                // Remove BLUETOOTH from list of available devices since SCO failed.
                audioDevices.remove(AudioDevice.BLUETOOTH);
                audioDeviceSetUpdated = true;
            }
        }

        // Update selected audio device.
        AudioDevice newAudioDevice = selectedAudioDevice;

        if (bluetoothManager.getState() == StringeeBluetoothManager.State.SCO_CONNECTED) {
            // If a Bluetooth is connected, then it should be used as output audio
            // device. Note that it is not sufficient that a headset is available;
            // an active SCO channel must also be up and running.
            newAudioDevice = AudioDevice.BLUETOOTH;
        } else if (hasWiredHeadset) {
            // If a wired headset is connected, but Bluetooth is not, then wired headset is used as
            // audio device.
            newAudioDevice = AudioDevice.WIRED_HEADSET;
        } else {
            // No wired headset and no Bluetooth, hence the audio-device list can contain speaker
            // phone (on a tablet), or speaker phone and earpiece (on mobile phone).
            // |defaultAudioDevice| contains either AudioDevice.SPEAKER_PHONE or AudioDevice.EARPIECE
            // depending on the user's selection.
            newAudioDevice = defaultAudioDevice;
        }
        // Switch to new device but only if there has been any changes.
        if (newAudioDevice != selectedAudioDevice || audioDeviceSetUpdated) {
            // Do the required device switch.
            setAudioDeviceInternal(newAudioDevice);
            Log.e(TAG, "New device status: "
                    + "available=" + audioDevices + ", "
                    + "selected=" + newAudioDevice);
            if (audioManagerEvents != null) {
                // Notify a listening client that audio device has been changed.
                audioManagerEvents.onAudioDeviceChanged(selectedAudioDevice, audioDevices);
            }
        }
        Log.e(TAG, "--- updateAudioDeviceState done");
    }

    /**
     * Check microphone mute or not
     *
     * @return
     */
    public boolean isMicrophoneMute() {
        return audioManager.isMicrophoneMute();
    }

    /**
     * Check speaker phone on or not
     *
     * @return
     */
    public boolean isSpeakerPhoneOn() {
        return audioManager.isSpeakerphoneOn();
    }

    public boolean getLastMicrophoneMute() {
        return lastMicrophoneMute;
    }

    public void setLastMicrophoneMute(boolean lastMicrophoneMute) {
        this.lastMicrophoneMute = lastMicrophoneMute;
    }

    public boolean getLastSpeakerphoneOn() {
        return lastSpeakerphoneOn;
    }

    public void setLastSpeakerphoneOn(boolean lastSpeakerphoneOn) {
        this.lastSpeakerphoneOn = lastSpeakerphoneOn;
    }

    public void saveSpeakerPhoneOn() {
        lastSpeakerphoneOn = audioManager.isSpeakerphoneOn();
    }
}