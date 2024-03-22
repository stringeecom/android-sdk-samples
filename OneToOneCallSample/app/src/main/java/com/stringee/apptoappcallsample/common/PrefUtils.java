package com.stringee.apptoappcallsample.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PrefUtils {
    private static SharedPreferences preferences;
    private static volatile PrefUtils instance;

    public PrefUtils(Context context) {
        preferences = context.getSharedPreferences(Constant.PREF_BASE, Context.MODE_PRIVATE);
    }

    public static synchronized PrefUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (PrefUtils.class) {
                if (instance == null) {
                    instance = new PrefUtils(context);
                }
            }
        }
        return instance;
    }

    public int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    public String getString(String key, String defValue) {
        return preferences.getString(key, defValue);
    }

    public boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    public long getLong(String key, long defValue) {
        return preferences.getLong(key, defValue);
    }

    public void putInt(String key, int defValue) {
        Editor editor = preferences.edit();
        editor.putInt(key, defValue);
        editor.apply();
    }

    public void putString(String key, String defValue) {
        Editor editor = preferences.edit();
        editor.putString(key, defValue);
        editor.apply();
    }

    public void putBoolean(String key, boolean defValue) {
        Editor editor = preferences.edit();
        editor.putBoolean(key, defValue);
        editor.apply();
    }

    public void putLong(String key, long defValue) {
        Editor editor = preferences.edit();
        editor.putLong(key, defValue);
        editor.apply();
    }

    public void clearData() {
        Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }
}
