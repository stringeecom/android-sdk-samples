package com.stringee.apptoappcallsample.stringee.common;

import com.stringee.common.StringeeAudioManager.AudioDevice;

import java.util.Set;

/** Pure routing policy used to select peripheral and built-in audio devices. */
final class AudioDeviceResolver {
    enum RouteAction {
        OBSERVE_ONLY,
        APPLY_BUILT_IN_ONLY,
        SELECT_DEVICE
    }

    private AudioDeviceResolver() {
    }

    static AudioDevice resolveInitialDevice(boolean videoCall, AudioDevice selectedDevice,
                                            Set<AudioDevice> availableDevices) {
        if (isPeripheral(selectedDevice) && contains(availableDevices, selectedDevice)) {
            return selectedDevice;
        }
        return resolveBuiltInDevice(videoCall, availableDevices);
    }

    static AudioDevice resolveBuiltInDevice(boolean videoCall,
                                            Set<AudioDevice> availableDevices) {
        AudioDevice preferred = videoCall ? AudioDevice.SPEAKER_PHONE : AudioDevice.EARPIECE;
        if (contains(availableDevices, preferred)) {
            return preferred;
        }
        AudioDevice fallback = videoCall ? AudioDevice.EARPIECE : AudioDevice.SPEAKER_PHONE;
        return contains(availableDevices, fallback) ? fallback : AudioDevice.NONE;
    }

    static AudioDevice resolvePreferredBuiltIn(AudioDevice preferred,
                                               Set<AudioDevice> availableDevices) {
        if (isBuiltIn(preferred) && contains(availableDevices, preferred)) {
            return preferred;
        }
        return resolveBuiltInDevice(preferred == AudioDevice.SPEAKER_PHONE, availableDevices);
    }

    static boolean isBuiltIn(AudioDevice device) {
        return device == AudioDevice.SPEAKER_PHONE || device == AudioDevice.EARPIECE;
    }

    static boolean isPeripheral(AudioDevice device) {
        return device == AudioDevice.BLUETOOTH || device == AudioDevice.WIRED_HEADSET;
    }

    static RouteAction resolveRouteAction(boolean userInitiated, AudioDevice device) {
        if (userInitiated) {
            return RouteAction.SELECT_DEVICE;
        }
        return isBuiltIn(device) ? RouteAction.APPLY_BUILT_IN_ONLY
                : RouteAction.OBSERVE_ONLY;
    }

    private static boolean contains(Set<AudioDevice> availableDevices, AudioDevice device) {
        return availableDevices != null && availableDevices.contains(device);
    }
}
