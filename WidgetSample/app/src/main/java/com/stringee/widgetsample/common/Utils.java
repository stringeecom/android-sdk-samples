package com.stringee.widgetsample.common;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Utils {
    public static <T> void reportException(@NonNull Class<T> clazz, Exception exception) {
        Log.e("Stringee exception", clazz.getName(), exception);
    }

    public static boolean isStringEmpty(@Nullable CharSequence text) {
        if (text != null) {
            return text.toString().trim().isEmpty();
        } else {
            return true;
        }
    }

    public static void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }
}
