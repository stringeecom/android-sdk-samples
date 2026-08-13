package com.stringee.kotlin_onetoonecallsample.stringee.common

import com.stringee.common.StringeeAudioManager.AudioDevice
import com.stringee.kotlin_onetoonecallsample.stringee.common.AudioDeviceResolver.resolveBuiltInDevice
import com.stringee.kotlin_onetoonecallsample.stringee.common.AudioDeviceResolver.resolveInitialDevice
import com.stringee.kotlin_onetoonecallsample.stringee.common.AudioDeviceResolver.resolvePreferredBuiltIn
import com.stringee.kotlin_onetoonecallsample.stringee.common.AudioDeviceResolver.resolveRouteAction
import org.junit.Assert
import org.junit.Test
import java.util.EnumSet

class AudioDeviceResolverTest {
    @Test
    fun voiceDefaultsToEarpieceAndVideoDefaultsToSpeaker() {
        val available: MutableSet<AudioDevice?>? =
            EnumSet.of<AudioDevice?>(AudioDevice.EARPIECE, AudioDevice.SPEAKER_PHONE)

        Assert.assertEquals(AudioDevice.EARPIECE, resolveBuiltInDevice(false, available))
        Assert.assertEquals(AudioDevice.SPEAKER_PHONE, resolveBuiltInDevice(true, available))
    }

    @Test
    fun activePeripheralIsPreservedDuringInitialSelection() {
        val available: MutableSet<AudioDevice?>? = EnumSet.of<AudioDevice?>(
            AudioDevice.EARPIECE,
            AudioDevice.SPEAKER_PHONE,
            AudioDevice.BLUETOOTH,
            AudioDevice.WIRED_HEADSET
        )

        Assert.assertEquals(
            AudioDevice.WIRED_HEADSET,
            resolveInitialDevice(false, AudioDevice.WIRED_HEADSET, available)
        )
        Assert.assertEquals(
            AudioDevice.BLUETOOTH,
            resolveInitialDevice(true, AudioDevice.BLUETOOTH, available)
        )
    }

    @Test
    fun builtInSdkSelectionDoesNotOverrideCallTypeDefault() {
        val available: MutableSet<AudioDevice?>? = EnumSet.of<AudioDevice?>(
            AudioDevice.EARPIECE,
            AudioDevice.SPEAKER_PHONE,
            AudioDevice.BLUETOOTH
        )

        Assert.assertEquals(
            AudioDevice.EARPIECE,
            resolveInitialDevice(false, AudioDevice.SPEAKER_PHONE, available)
        )
        Assert.assertEquals(
            AudioDevice.SPEAKER_PHONE,
            resolveInitialDevice(true, AudioDevice.EARPIECE, available)
        )
        Assert.assertEquals(
            AudioDevice.WIRED_HEADSET,
            resolveInitialDevice(
                false, AudioDevice.WIRED_HEADSET,
                EnumSet.of<AudioDevice?>(
                    AudioDevice.EARPIECE,
                    AudioDevice.SPEAKER_PHONE,
                    AudioDevice.WIRED_HEADSET
                )
            )
        )
    }

    @Test
    fun returnsNoneWhenNoBuiltInRouteExists() {
        Assert.assertEquals(
            AudioDevice.NONE,
            resolveBuiltInDevice(false, EnumSet.of<AudioDevice?>(AudioDevice.BLUETOOTH))
        )
    }

    @Test
    fun peripheralRemovalRestoresTheUsersBuiltInChoice() {
        val available: MutableSet<AudioDevice?>? =
            EnumSet.of<AudioDevice?>(AudioDevice.EARPIECE, AudioDevice.SPEAKER_PHONE)

        Assert.assertEquals(
            AudioDevice.EARPIECE,
            resolvePreferredBuiltIn(AudioDevice.EARPIECE, available)
        )
        Assert.assertEquals(
            AudioDevice.SPEAKER_PHONE,
            resolvePreferredBuiltIn(AudioDevice.SPEAKER_PHONE, available)
        )
    }

    @Test
    fun automaticRoutingDoesNotBecomeAnSdkUserSelection() {
        Assert.assertEquals(
            AudioDeviceResolver.RouteAction.APPLY_BUILT_IN_ONLY,
            resolveRouteAction(false, AudioDevice.EARPIECE)
        )
        Assert.assertEquals(
            AudioDeviceResolver.RouteAction.OBSERVE_ONLY,
            resolveRouteAction(false, AudioDevice.BLUETOOTH)
        )
        Assert.assertEquals(
            AudioDeviceResolver.RouteAction.SELECT_DEVICE,
            resolveRouteAction(true, AudioDevice.SPEAKER_PHONE)
        )
        Assert.assertEquals(
            AudioDeviceResolver.RouteAction.SELECT_DEVICE,
            resolveRouteAction(true, AudioDevice.WIRED_HEADSET)
        )
    }
}
