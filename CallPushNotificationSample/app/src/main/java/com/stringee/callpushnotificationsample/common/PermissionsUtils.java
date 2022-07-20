package com.stringee.callpushnotificationsample.common;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionsUtils {
    public static final int REQUEST_CALL_PERMISSION = 1;
    public static String[] PERMISSION_VOICE_CALL = {permission.RECORD_AUDIO};
    public static String[] PERMISSION_VOICE_CALL_ANDROID_12 = {permission.RECORD_AUDIO, permission.BLUETOOTH_CONNECT};
    public static String[] PERMISSION_VIDEO_CALL = {permission.RECORD_AUDIO, permission.CAMERA};
    public static String[] PERMISSION_VIDEO_CALL_ANDROID_12 = {permission.RECORD_AUDIO, permission.CAMERA, permission.BLUETOOTH_CONNECT};

    public static boolean isVoiceCallPermissionGranted(Context context) {
        int resultRecord = ContextCompat.checkSelfPermission(context, permission.RECORD_AUDIO);
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            int resultBluetooth = ContextCompat.checkSelfPermission(context, permission.BLUETOOTH_CONNECT);
            return (resultRecord == PackageManager.PERMISSION_GRANTED &&
                    resultBluetooth == PackageManager.PERMISSION_GRANTED);
        } else {
            return (resultRecord == PackageManager.PERMISSION_GRANTED);
        }
    }

    public static boolean isVideoCallPermissionGranted(Context context) {
        int resultMic = ContextCompat.checkSelfPermission(context, permission.RECORD_AUDIO);
        int resultRecord = ContextCompat.checkSelfPermission(context, permission.CAMERA);
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            int resultBluetooth = ContextCompat.checkSelfPermission(context, permission.BLUETOOTH_CONNECT);
            return (resultMic == PackageManager.PERMISSION_GRANTED &&
                    resultRecord == PackageManager.PERMISSION_GRANTED &&
                    resultBluetooth == PackageManager.PERMISSION_GRANTED);
        } else {
            return (resultMic == PackageManager.PERMISSION_GRANTED &&
                    resultRecord == PackageManager.PERMISSION_GRANTED);
        }
    }

    public static void requestVoiceCallPermission(Activity activity) {
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            requestPermissions(activity, PERMISSION_VOICE_CALL_ANDROID_12, REQUEST_CALL_PERMISSION);
        } else {
            requestPermissions(activity, PERMISSION_VOICE_CALL, REQUEST_CALL_PERMISSION);
        }
    }

    public static void requestVideoCallPermission(Activity activity) {
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            requestPermissions(activity, PERMISSION_VIDEO_CALL_ANDROID_12, REQUEST_CALL_PERMISSION);
        } else {
            requestPermissions(activity, PERMISSION_VIDEO_CALL, REQUEST_CALL_PERMISSION);
        }
    }

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }
}
