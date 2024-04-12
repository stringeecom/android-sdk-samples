package com.stringee.video_conference_sample.stringee_wrapper.common;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.AnimRes;
import androidx.annotation.AnimatorRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.stringee.video_conference_sample.R;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
