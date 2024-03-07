package com.stringee.kotlin_onetoonecallsample.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.stringee.kotlin_onetoonecallsample.common.Constant
import com.stringee.kotlin_onetoonecallsample.common.Utils
import com.stringee.kotlin_onetoonecallsample.manager.CallManager

class MyMediaProjectionService : Service() {
    private lateinit var callManager: CallManager
    override fun onCreate() {
        super.onCreate()
        callManager = CallManager.getInstance(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val action = intent.action
        if (!Utils.isStringEmpty(action)) {
            if (action == Constant.ACTION_START_FOREGROUND_SERVICE) {
                callManager.startCapture(this)
            }
        }
        return START_NOT_STICKY
    }

    fun stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}