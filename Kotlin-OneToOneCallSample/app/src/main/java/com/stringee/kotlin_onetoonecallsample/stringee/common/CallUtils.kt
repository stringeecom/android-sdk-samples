package com.stringee.kotlin_onetoonecallsample.stringee.common

import android.os.Handler
import android.os.Looper
import android.util.Log

/** Small threading, validation, and error-reporting helpers shared by call components. */
object CallUtils {
    private val MAIN_HANDLER = Handler(Looper.getMainLooper())

    fun runOnUiThread(runnable: Runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run()
        } else {
            MAIN_HANDLER.post(runnable)
        }
    }

    fun isEmpty(value: String?): Boolean {
        return value == null || value.trim { it <= ' ' }.isEmpty()
    }

    fun reportException(owner: Class<*>, throwable: Throwable?) {
        Log.e(CallConstants.TAG, owner.getSimpleName(), throwable)
    }
}
