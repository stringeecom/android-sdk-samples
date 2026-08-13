package com.stringee.softphone.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AccessTokenRefreshPolicyTest {

    @Test
    public void missingCachedTokenAlwaysRequiresRefresh() {
        assertTrue(AccessTokenRefreshPolicy.shouldRefresh(1_000L, 9_999L, "  "));
    }

    @Test
    public void tokenRemainsUsableBeforeExpiryInSeconds() {
        assertFalse(AccessTokenRefreshPolicy.shouldRefresh(
                9_999L,
                10L,
                "cached-token"));
    }

    @Test
    public void tokenRequiresRefreshExactlyAtExpiryBoundary() {
        assertTrue(AccessTokenRefreshPolicy.shouldRefresh(
                10_000L,
                10L,
                "cached-token"));
    }

    @Test
    public void serverAndFallbackTokensAreTrimmed() {
        assertEquals("server-token", AccessTokenRefreshPolicy.normalize(" server-token "));
        assertEquals("cached-token", AccessTokenRefreshPolicy.fallbackToken(" cached-token "));
        assertEquals("", AccessTokenRefreshPolicy.normalize(null));
    }
}
