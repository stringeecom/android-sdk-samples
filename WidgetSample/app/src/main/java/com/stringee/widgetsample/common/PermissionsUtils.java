package com.stringee.widgetsample.common;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION_CODES;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;


public class PermissionsUtils {
    private static volatile PermissionsUtils instance;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 1;
    @RequiresApi(api = VERSION_CODES.TIRAMISU)
    public String[] notificationPermission = {permission.POST_NOTIFICATIONS};

    public PermissionsUtils() {
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
