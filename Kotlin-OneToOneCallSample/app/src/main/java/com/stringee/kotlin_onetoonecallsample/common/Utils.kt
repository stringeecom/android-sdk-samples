package com.stringee.kotlin_onetoonecallsample.common

import android.os.Handler
import android.os.Looper


object Utils {
    fun runOnUiThread(runnable: Runnable?) {
        val handler = Handler(Looper.getMainLooper())
        handler.post(runnable!!)
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
}

