package com.stringee.kotlin_onetoonecallsample.common

import android.os.Handler
import android.os.Looper
import android.util.Log


object Utils {
    fun runOnUiThread(runnable: Runnable) {
        val handler = Handler(Looper.getMainLooper())
        handler.post(runnable)
    }

    fun postDelay(runnable: Runnable, millis: Long) {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(runnable, millis)
    }

    fun isStringEmpty(text: CharSequence?): Boolean {
        return if (text != null) {
            if (text.toString().equals("null", ignoreCase = true)) {
                true
            } else {
                text.toString().trim { it <= ' ' }.isEmpty()
            }
        } else {
            true
        }
    }

    fun <T> reportException(clazz: Class<T>, exception: Exception?) {
        Log.e("Stringee exception", clazz.getName(), exception)
    }
}

