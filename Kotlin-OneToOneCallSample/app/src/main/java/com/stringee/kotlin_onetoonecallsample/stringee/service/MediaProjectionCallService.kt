package com.stringee.kotlin_onetoonecallsample.stringee.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallConstants
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallNotificationManager
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallUtils
import com.stringee.kotlin_onetoonecallsample.stringee.manager.CallSession
import com.stringee.kotlin_onetoonecallsample.stringee.manager.StringeeCallManager

/** Foreground service that owns the MediaProjection lifecycle for Call2 screen sharing. */
class MediaProjectionCallService : Service() {
    private var stoppingFromSession = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || CallConstants.ACTION_START_IN_CALL != intent.getAction()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        try {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            else
                0
            ServiceCompat.startForeground(
                this, CallConstants.MEDIA_NOTIFICATION_ID,
                CallNotificationManager.Companion.getInstance(this).buildMediaProjection(), type
            )
        } catch (exception: RuntimeException) {
            CallUtils.reportException(MediaProjectionCallService::class.java, exception)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val permissionData: Intent?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionData =
                intent.getParcelableExtra<Intent?>(EXTRA_PERMISSION_DATA, Intent::class.java)
        } else {
            permissionData = getLegacyPermissionData(intent)
        }
        val session: CallSession? = StringeeCallManager.Companion.getInstance(this).session
        if (session == null || permissionData == null) {
            stopService()
            return START_NOT_STICKY
        }
        session.startScreenShare(this, permissionData)
        return START_NOT_STICKY
    }

    @Suppress("deprecation")
    private fun getLegacyPermissionData(intent: Intent): Intent? {
        return intent.getParcelableExtra<Intent?>(EXTRA_PERMISSION_DATA)
    }

    fun stopService() {
        stoppingFromSession = true
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        CallNotificationManager.Companion.getInstance(this)
            .cancel(CallConstants.MEDIA_NOTIFICATION_ID)
        stopSelf()
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        CallNotificationManager.Companion.getInstance(this)
            .cancel(CallConstants.MEDIA_NOTIFICATION_ID)
        if (!stoppingFromSession) {
            val session: CallSession? = StringeeCallManager.Companion.getInstance(this).session
            if (session != null) {
                session.onMediaProjectionServiceDestroyed(this)
            }
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val EXTRA_PERMISSION_DATA: String = "media_projection_permission_data"
    }
}
