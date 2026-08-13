package com.stringee.apptoappcallsample.stringee.common;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/** Small threading, validation, and error-reporting helpers shared by call components. */
public final class CallUtils {
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private CallUtils() {
    }

    public static void runOnUiThread(Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            MAIN_HANDLER.post(runnable);
        }
    }

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static void reportException(Class<?> owner, Throwable throwable) {
        Log.e(CallConstants.TAG, owner.getSimpleName(), throwable);
    }
}
