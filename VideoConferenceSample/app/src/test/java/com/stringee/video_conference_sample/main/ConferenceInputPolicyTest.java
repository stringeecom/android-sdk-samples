package com.stringee.video_conference_sample.main;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConferenceInputPolicyTest {

    @Test
    public void accessAndRoomTokensMustNotBeBlank() {
        assertFalse(ConferenceInputPolicy.isValidAccessToken("  "));
        assertFalse(ConferenceInputPolicy.isValidRoomToken(null));
    }

    @Test
    public void validTokensAreTrimmedBeforeSdkUse() {
        assertTrue(ConferenceInputPolicy.isValidAccessToken(" access "));
        assertEquals("access", ConferenceInputPolicy.normalizeToken(" access "));
        assertEquals("room", ConferenceInputPolicy.normalizeToken(" room "));
    }
}
