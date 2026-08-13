package com.stringee.kotlin_onetoonecallsample.stringee.common

import com.stringee.common.StringeeAudioManager.AudioDevice

/** Pure routing policy used to select peripheral and built-in audio devices. */
internal object AudioDeviceResolver {
    @JvmStatic fun resolveInitialDevice(
        videoCall: Boolean, selectedDevice: AudioDevice?,
        availableDevices: MutableSet<AudioDevice?>?
    ): AudioDevice? {
        if (isPeripheral(selectedDevice) && contains(availableDevices, selectedDevice)) {
            return selectedDevice
        }
        return resolveBuiltInDevice(videoCall, availableDevices)
    }

    @JvmStatic fun resolveBuiltInDevice(
        videoCall: Boolean,
        availableDevices: MutableSet<AudioDevice?>?
    ): AudioDevice {
        val preferred = if (videoCall) AudioDevice.SPEAKER_PHONE else AudioDevice.EARPIECE
        if (contains(availableDevices, preferred)) {
            return preferred
        }
        val fallback = if (videoCall) AudioDevice.EARPIECE else AudioDevice.SPEAKER_PHONE
        return if (contains(availableDevices, fallback)) fallback else AudioDevice.NONE
    }

    @JvmStatic fun resolvePreferredBuiltIn(
        preferred: AudioDevice?,
        availableDevices: MutableSet<AudioDevice?>?
    ): AudioDevice? {
        if (isBuiltIn(preferred) && contains(availableDevices, preferred)) {
            return preferred
        }
        return resolveBuiltInDevice(preferred == AudioDevice.SPEAKER_PHONE, availableDevices)
    }

    @JvmStatic fun isBuiltIn(device: AudioDevice?): Boolean {
        return device == AudioDevice.SPEAKER_PHONE || device == AudioDevice.EARPIECE
    }

    @JvmStatic fun isPeripheral(device: AudioDevice?): Boolean {
        return device == AudioDevice.BLUETOOTH || device == AudioDevice.WIRED_HEADSET
    }

    @JvmStatic fun resolveRouteAction(userInitiated: Boolean, device: AudioDevice?): RouteAction {
        if (userInitiated) {
            return RouteAction.SELECT_DEVICE
        }
        return if (isBuiltIn(device))
            RouteAction.APPLY_BUILT_IN_ONLY
        else
            RouteAction.OBSERVE_ONLY
    }

    private fun contains(
        availableDevices: MutableSet<AudioDevice?>?,
        device: AudioDevice?
    ): Boolean {
        return availableDevices != null && availableDevices.contains(device)
    }

    internal enum class RouteAction {
        OBSERVE_ONLY,
        APPLY_BUILT_IN_ONLY,
        SELECT_DEVICE
    }
}
