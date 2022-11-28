package com.stringee.kotlin_onetoonecallsample.common

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Toast

object Utils {
    fun reportMessage(context: Context?, message: String?) {
        val toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
        toast.setGravity(Gravity.CENTER, 0, 0)
        toast.show()
    }

    fun postDelay(runnable: Runnable?, delayMillis: Long) {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(runnable!!, delayMillis)
    }

    fun runOnUiThread(runnable: Runnable?) {
        val handler = Handler(Looper.getMainLooper())
        handler.post(runnable!!)
    }
}