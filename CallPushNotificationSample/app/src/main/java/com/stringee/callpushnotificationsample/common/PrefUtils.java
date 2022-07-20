package com.stringee.callpushnotificationsample.common;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefUtils {
    private static volatile PrefUtils instance;
    private static final Object lock = new Object();
    private static SharedPreferences preferences;

    public static PrefUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new PrefUtils(context);
                }
            }
        }
        return instance;
    }

    public PrefUtils(Context context) {
        if (preferences == null) {
            preferences = context.getSharedPreferences(Constant.PREF_BASE, Context.MODE_PRIVATE);
        }
    }

    public int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    public String getString(String key, String defValue) {
        return preferences.getString(key, defValue);
    }

    public void putInt(String key, int defValue) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(key, defValue);
        editor.commit();
    }

    public void putString(String key, String defValue) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(key, defValue);
        editor.commit();
    }

    public void putBoolean(String key, boolean defValue) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(key, defValue);
        editor.commit();
    }

    public boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    public void putLong(String key, long defValue) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(key, defValue);
        editor.commit();
    }

    public long getLong(String key, long defValue) {
        return preferences.getLong(key, defValue);
    }

    public void clearData() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }
}
