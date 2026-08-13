package com.stringee.softphone.common;

/** Determines whether the cached access token can be reused before contacting the token server. */
public final class AccessTokenRefreshPolicy {
    private AccessTokenRefreshPolicy() {
    }

    public static boolean shouldRefresh(long nowMillis, long expirySeconds, String cachedToken) {
        return normalize(cachedToken).isEmpty() || nowMillis >= expirySeconds * 1000L;
    }

    public static String normalize(String token) {
        return token == null ? "" : token.trim();
    }

    public static String fallbackToken(String cachedToken) {
        return normalize(cachedToken);
    }
}
