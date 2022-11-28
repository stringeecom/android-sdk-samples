package com.stringee.kotlin_onetoonecallsample.common

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionsUtils {
    var PERMISSION_VOICE_CALL = arrayOf(permission.RECORD_AUDIO)
    var PERMISSION_VOICE_CALL_ANDROID_12 =
        arrayOf(permission.RECORD_AUDIO, permission.BLUETOOTH_CONNECT)
    var PERMISSION_VIDEO_CALL = arrayOf(permission.RECORD_AUDIO, permission.CAMERA)
    var PERMISSION_VIDEO_CALL_ANDROID_12 =
        arrayOf(permission.RECORD_AUDIO, permission.CAMERA, permission.BLUETOOTH_CONNECT)

    fun isVoiceCallPermissionGranted(context: Context?): Boolean {
        val resultRecord = ContextCompat.checkSelfPermission(context!!, permission.RECORD_AUDIO)
        return if (VERSION.SDK_INT >= VERSION_CODES.S) {
            val resultBluetooth =
                ContextCompat.checkSelfPermission(context, permission.BLUETOOTH_CONNECT)
            resultRecord == PackageManager.PERMISSION_GRANTED &&
                    resultBluetooth == PackageManager.PERMISSION_GRANTED
        } else {
            resultRecord == PackageManager.PERMISSION_GRANTED
        }
    }

    fun isVideoCallPermissionGranted(context: Context?): Boolean {
        val resultMic = ContextCompat.checkSelfPermission(context!!, permission.RECORD_AUDIO)
        val resultRecord = ContextCompat.checkSelfPermission(context, permission.CAMERA)
        return if (VERSION.SDK_INT >= VERSION_CODES.S) {
            val resultBluetooth =
                ContextCompat.checkSelfPermission(context, permission.BLUETOOTH_CONNECT)
            resultMic == PackageManager.PERMISSION_GRANTED && resultRecord == PackageManager.PERMISSION_GRANTED && resultBluetooth == PackageManager.PERMISSION_GRANTED
        } else {
            resultMic == PackageManager.PERMISSION_GRANTED &&
                    resultRecord == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestVoiceCallPermission(activity: Activity?) {
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            requestPermissions(
                activity,
                PERMISSION_VOICE_CALL_ANDROID_12,
                Constant.REQUEST_PERMISSION_CALL
            )
        } else {
            requestPermissions(activity, PERMISSION_VOICE_CALL, Constant.REQUEST_PERMISSION_CALL)
        }
    }

    fun requestVideoCallPermission(activity: Activity?) {
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            requestPermissions(
                activity,
                PERMISSION_VIDEO_CALL_ANDROID_12,
                Constant.REQUEST_PERMISSION_CALL
            )
        } else {
            requestPermissions(activity, PERMISSION_VIDEO_CALL, Constant.REQUEST_PERMISSION_CALL)
        }
    }

    fun requestPermissions(activity: Activity?, permissions: Array<String>?, requestCode: Int) {
        ActivityCompat.requestPermissions(activity!!, permissions!!, requestCode)
    }
}