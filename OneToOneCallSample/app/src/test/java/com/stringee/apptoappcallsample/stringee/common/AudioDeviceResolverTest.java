package com.stringee.apptoappcallsample.stringee.common;

import static com.stringee.common.StringeeAudioManager.AudioDevice.BLUETOOTH;
import static com.stringee.common.StringeeAudioManager.AudioDevice.EARPIECE;
import static com.stringee.common.StringeeAudioManager.AudioDevice.NONE;
import static com.stringee.common.StringeeAudioManager.AudioDevice.SPEAKER_PHONE;
import static com.stringee.common.StringeeAudioManager.AudioDevice.WIRED_HEADSET;
import static org.junit.Assert.assertEquals;

import com.stringee.common.StringeeAudioManager.AudioDevice;

import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

public class AudioDeviceResolverTest {
    @Test
    public void voiceDefaultsToEarpieceAndVideoDefaultsToSpeaker() {
        Set<AudioDevice> available = EnumSet.of(EARPIECE, SPEAKER_PHONE);

        assertEquals(EARPIECE, AudioDeviceResolver.resolveBuiltInDevice(false, available));
        assertEquals(SPEAKER_PHONE, AudioDeviceResolver.resolveBuiltInDevice(true, available));
    }

    @Test
    public void activePeripheralIsPreservedDuringInitialSelection() {
        Set<AudioDevice> available = EnumSet.of(
                EARPIECE, SPEAKER_PHONE, BLUETOOTH, WIRED_HEADSET);

        assertEquals(WIRED_HEADSET,
                AudioDeviceResolver.resolveInitialDevice(false, WIRED_HEADSET, available));
        assertEquals(BLUETOOTH,
                AudioDeviceResolver.resolveInitialDevice(true, BLUETOOTH, available));
    }

    @Test
    public void builtInSdkSelectionDoesNotOverrideCallTypeDefault() {
        Set<AudioDevice> available = EnumSet.of(EARPIECE, SPEAKER_PHONE, BLUETOOTH);

        assertEquals(EARPIECE,
                AudioDeviceResolver.resolveInitialDevice(false, SPEAKER_PHONE, available));
        assertEquals(SPEAKER_PHONE,
                AudioDeviceResolver.resolveInitialDevice(true, EARPIECE, available));
        assertEquals(WIRED_HEADSET,
                AudioDeviceResolver.resolveInitialDevice(false, WIRED_HEADSET,
                        EnumSet.of(EARPIECE, SPEAKER_PHONE, WIRED_HEADSET)));
    }

    @Test
    public void returnsNoneWhenNoBuiltInRouteExists() {
        assertEquals(NONE,
                AudioDeviceResolver.resolveBuiltInDevice(false, EnumSet.of(BLUETOOTH)));
    }

    @Test
    public void peripheralRemovalRestoresTheUsersBuiltInChoice() {
        Set<AudioDevice> available = EnumSet.of(EARPIECE, SPEAKER_PHONE);

        assertEquals(EARPIECE,
                AudioDeviceResolver.resolvePreferredBuiltIn(EARPIECE, available));
        assertEquals(SPEAKER_PHONE,
                AudioDeviceResolver.resolvePreferredBuiltIn(SPEAKER_PHONE, available));
    }

    @Test
    public void automaticRoutingDoesNotBecomeAnSdkUserSelection() {
        assertEquals(AudioDeviceResolver.RouteAction.APPLY_BUILT_IN_ONLY,
                AudioDeviceResolver.resolveRouteAction(false, EARPIECE));
        assertEquals(AudioDeviceResolver.RouteAction.OBSERVE_ONLY,
                AudioDeviceResolver.resolveRouteAction(false, BLUETOOTH));
        assertEquals(AudioDeviceResolver.RouteAction.SELECT_DEVICE,
                AudioDeviceResolver.resolveRouteAction(true, SPEAKER_PHONE));
        assertEquals(AudioDeviceResolver.RouteAction.SELECT_DEVICE,
                AudioDeviceResolver.resolveRouteAction(true, WIRED_HEADSET));
    }
}
