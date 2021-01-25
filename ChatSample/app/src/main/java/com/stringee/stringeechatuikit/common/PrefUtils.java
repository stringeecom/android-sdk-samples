package com.stringee.stringeechatuikit.common;

import android.content.SharedPreferences.Editor;

public class PrefUtils {

    public static int getInt(String key, int defValue) {
        return Common.preferences.getInt(key, defValue);
    }

    public static String getString(String key, String defValue) {
        return Common.preferences.getString(key, defValue);
    }

    public static void putInt(String key, int defValue) {
        Editor editor = Common.preferences.edit();
        editor.putInt(key, defValue);
        editor.commit();
    }

    public static void putString(String key, String defValue) {
        Editor editor = Common.preferences.edit();
        editor.putString(key, defValue);
        editor.commit();
    }

    public static void putBoolean(String key, boolean defValue) {
        Editor editor = Common.preferences.edit();
        editor.putBoolean(key, defValue);
        editor.commit();
    }

    public static boolean getBoolean(String key, boolean defValue) {
        return Common.preferences.getBoolean(key, defValue);
    }

    public static void putLong(String key, long defValue) {
        Editor editor = Common.preferences.edit();
        editor.putLong(key, defValue);
        editor.commit();
    }

    public static long getLong(String key, long defValue) {
        return Common.preferences.getLong(key, defValue);
    }

    public static void clear() {
        Editor editor = Common.preferences.edit();
        editor.clear();
        editor.commit();
    }
}
