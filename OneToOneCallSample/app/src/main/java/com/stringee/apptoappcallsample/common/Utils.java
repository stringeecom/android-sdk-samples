package com.stringee.apptoappcallsample.common;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;

public class Utils {
    public static void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }

    public static boolean isStringEmpty(@Nullable CharSequence text) {
        if (text != null) {
            if (text.toString().equalsIgnoreCase("null")) {
                return true;
            } else {
                return text.toString().trim().length() == 0;
            }
        } else {
            return true;
        }
    }
}
