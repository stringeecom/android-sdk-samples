package com.stringee.kotlin_onetoonecallsample.common

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.app.ActivityCompat


class PermissionsUtils {
    private var permissions = arrayOf(permission.RECORD_AUDIO, permission.CAMERA)

    init {
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            permissions =
                arrayOf(permission.RECORD_AUDIO, permission.CAMERA, permission.BLUETOOTH_CONNECT)
        }
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            permissions = arrayOf(
                permission.RECORD_AUDIO,
                permission.CAMERA,
                permission.BLUETOOTH_CONNECT,
                permission.POST_NOTIFICATIONS
            )
        }
    }

    fun verifyPermissions(grantResults: IntArray): Boolean {
        if (grantResults.isEmpty()) {
            return false
        }
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun requestPermissions(activity: Activity?) {
        ActivityCompat.requestPermissions(activity!!, permissions, REQUEST_PERMISSION)
    }

    fun shouldRequestPermissionRationale(activity: Activity): Boolean {
        var showSetting = false
        for (perm in permissions) {
            if (activity.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED && !activity.shouldShowRequestPermissionRationale(
                    perm
                )
            ) {
                showSetting = true
                break
            }
        }
        return showSetting
    }

    fun checkSelfPermission(context: Context?): Boolean {
        for (perm in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    context!!,
                    perm
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    companion object {
        @Volatile
        private var instance: PermissionsUtils? = null
        fun getInstance(): PermissionsUtils {
            return instance ?: synchronized(this) {
                instance ?: PermissionsUtils().also {
                    instance = it
                }
            }
        }


        const val REQUEST_PERMISSION = 1
    }
}

