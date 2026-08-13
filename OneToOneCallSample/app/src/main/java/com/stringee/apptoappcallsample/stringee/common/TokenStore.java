package com.stringee.apptoappcallsample.stringee.common;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists tokens only after a successful Stringee or Firebase registration event. */
public final class TokenStore {
    public static final String PREFS = "stringee_call_sample";
    private static final String PREF_ACCESS_TOKEN = "last_connected_access_token";
    private static final String PREF_FCM_TOKEN = "last_fcm_token";

    private final SharedPreferences preferences;

    public TokenStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public String getToken() {
        return preferences.getString(PREF_ACCESS_TOKEN, "");
    }

    public void saveConnectedToken(String token) {
        String normalized = normalize(token);
        if (!normalized.isEmpty()) {
            preferences.edit().putString(PREF_ACCESS_TOKEN, normalized).apply();
        }
    }

    public String getFcmToken() {
        return preferences.getString(PREF_FCM_TOKEN, "");
    }

    public void saveFcmToken(String token) {
        String normalized = normalize(token);
        if (!normalized.isEmpty()) {
            preferences.edit().putString(PREF_FCM_TOKEN, normalized).apply();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
