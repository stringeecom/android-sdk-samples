package com.stringee.kotlin_onetoonecallsample.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.stringee.kotlin_onetoonecallsample.common.Constant
import com.stringee.kotlin_onetoonecallsample.common.NotificationUtils
import com.stringee.kotlin_onetoonecallsample.common.PrefUtils
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
                val notification =
                    NotificationUtils.getInstance(this).createMediaNotification()
                var type = 0
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                }
                try {
                    ServiceCompat.startForeground(
                        this,
                        Constant.MEDIA_SERVICE_ID,
                        notification,
                        type
                    )
                } catch (e: Exception) {
                    Utils.reportException(CallManager::class.java, e)
                }
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