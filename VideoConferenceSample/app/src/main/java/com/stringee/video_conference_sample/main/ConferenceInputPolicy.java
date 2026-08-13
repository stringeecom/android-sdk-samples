package com.stringee.video_conference_sample.main;

/** Normalizes and validates access and room tokens before they reach the Stringee SDK. */
public final class ConferenceInputPolicy {
    private ConferenceInputPolicy() {
    }

    public static boolean isValidAccessToken(String token) {
        return !normalizeToken(token).isEmpty();
    }

    public static boolean isValidRoomToken(String token) {
        return !normalizeToken(token).isEmpty();
    }

    public static String normalizeToken(String token) {
        return token == null ? "" : token.trim();
    }
}
