package com.stringee.video_conference_sample.stringee_wrapper.common;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;


public class PermissionsUtils {
    private static volatile PermissionsUtils instance;
    public static final int REQUEST_CONFERENCE_PERMISSION = 1;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 2;
    public String[] conferencePermission = {permission.RECORD_AUDIO, permission.CAMERA};
    @RequiresApi(api = VERSION_CODES.TIRAMISU)
    public String[] notificationPermission = {permission.POST_NOTIFICATIONS};

    public PermissionsUtils() {
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            conferencePermission = new String[]{permission.RECORD_AUDIO, permission.CAMERA, permission.BLUETOOTH_CONNECT};
        }
    }

    public static PermissionsUtils getInstance() {
        if (instance == null) {
            synchronized (PermissionsUtils.class) {
                if (instance == null) {
                    instance = new PermissionsUtils();
                }
            }
        }
        return instance;
    }

    public boolean verifyPermissions(int[] grantResults) {
        if (grantResults.length < 1) {
            return false;
        }

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void requestConferencePermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, conferencePermission, REQUEST_CONFERENCE_PERMISSION);
    }

    public boolean shouldRequestConferencePermissionRationale(Activity activity) {
        boolean showSetting = false;
        for (String perm : conferencePermission) {
            if (activity.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED && !activity.shouldShowRequestPermissionRationale(perm)) {
                showSetting = true;
                break;
            }
        }
        return showSetting;
    }

    public boolean checkSelfConferencePermission(Context context) {
        for (String perm : conferencePermission) {
            if (ActivityCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @RequiresApi(api = VERSION_CODES.TIRAMISU)
    public void requestNotificationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, notificationPermission, REQUEST_NOTIFICATION_PERMISSION);
    }

    @RequiresApi(api = VERSION_CODES.TIRAMISU)
    public boolean shouldRequestNotificationPermissionRationale(Activity activity) {
        boolean showSetting = false;
        for (String perm : notificationPermission) {
            if (activity.checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED && !activity.shouldShowRequestPermissionRationale(perm)) {
                showSetting = true;
                break;
            }
        }
        return showSetting;
    }

    @RequiresApi(api = VERSION_CODES.TIRAMISU)
    public boolean checkSelfNotificationPermission(Context context) {
        for (String perm : notificationPermission) {
            if (ActivityCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
}
