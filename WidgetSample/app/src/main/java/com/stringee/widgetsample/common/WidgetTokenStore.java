package com.stringee.widgetsample.common;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists only the access token from the most recent successful Widget connection. */
public final class WidgetTokenStore {
    static final String KEY_TOKEN = "connected_token";
    private final SharedPreferences preferences;

    /** Creates a token store backed by the application's private preferences. */
    public WidgetTokenStore(Context context) {
        preferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }

    /** Returns the last successfully connected access token, or an empty string. */
    public String getToken() {
        return preferences.getString(KEY_TOKEN, "");
    }

    /** Saves a non-empty token after the Widget reports a successful connection. */
    public void saveConnectedToken(String token) {
        String normalized = token == null ? "" : token.trim();
        if (!normalized.isEmpty()) {
            preferences.edit().putString(KEY_TOKEN, normalized).apply();
        }
    }
}
