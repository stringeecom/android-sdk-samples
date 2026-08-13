package com.stringee.kotlin_onetoonecallsample.stringee.common

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Centralizes runtime call permissions and incoming-call notification access checks. */
object CallPermissions {
    const val REQUEST_CALL_PERMISSIONS: Int = 1001
    const val REQUEST_NOTIFICATION_PERMISSION: Int = 1002

    fun getCallPermissions(videoCall: Boolean): Array<String> {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.RECORD_AUDIO)
        if (videoCall) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        return permissions.toTypedArray()
    }

    fun hasCallPermissions(context: Context, videoCall: Boolean): Boolean {
        for (permission in getCallPermissions(videoCall)) {
            if (ContextCompat.checkSelfPermission(context, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    fun requestCallPermissions(activity: Activity, videoCall: Boolean) {
        ActivityCompat.requestPermissions(
            activity, getCallPermissions(videoCall),
            REQUEST_CALL_PERMISSIONS
        )
    }

    fun verify(grantResults: IntArray?): Boolean {
        if (grantResults == null || grantResults.size == 0) {
            return false
        }
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    @JvmStatic fun canPostNotifications(context: Context): Boolean {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            return false
        }
        try {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (exception: RuntimeException) {
            CallUtils.reportException(CallPermissions::class.java, exception)
            return false
        }
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf<String>(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATION_PERMISSION
            )
        }
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true
        }
        val manager =
            context.getSystemService<NotificationManager?>(NotificationManager::class.java)
        return manager != null && manager.canUseFullScreenIntent()
    }

    fun openFullScreenIntentSettings(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                Uri.parse("package:" + activity.getPackageName())
            )
            activity.startActivity(intent)
        }
    }

    fun openAppSettings(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:" + activity.getPackageName())
        )
        activity.startActivity(intent)
    }
}
