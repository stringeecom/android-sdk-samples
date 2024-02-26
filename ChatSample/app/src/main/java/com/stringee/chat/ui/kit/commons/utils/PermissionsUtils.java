package com.stringee.chat.ui.kit.commons.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by sunil on 20/sticker_icon_1/16.
 */
public class PermissionsUtils {
    private static volatile PermissionsUtils instance;
    public static String[] PERMISSIONS_LOCATION;
    public static String[] PERMISSIONS_STORAGE;
    public static String[] PERMISSIONS_RECORD_AUDIO;
    public static String[] PERMISSION_CAPTURE_IMAGE;
    public static String[] PERMISSION_CAPTURE_VIDEO;
    public static String[] PERMISSION_CONTACT;
    public static final int REQUEST_STORAGE = 0;
    public static final int REQUEST_CAPTURE_IMAGE = 1;
    public static final int REQUEST_CAPTURE_VIDEO = 2;
    public static final int REQUEST_RECORD_AUDIO = 3;
    public static final int REQUEST_CONTACT = 4;
    public static final int REQUEST_CONTACT_SHOW = 5;
    public static final int REQUEST_LOCATION = 6;

    public PermissionsUtils() {
        PERMISSIONS_LOCATION = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        PERMISSIONS_STORAGE = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PERMISSIONS_STORAGE = new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO};
        }
        PERMISSIONS_RECORD_AUDIO = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PERMISSIONS_RECORD_AUDIO = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_AUDIO};
        }
        PERMISSION_CAPTURE_IMAGE = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PERMISSION_CAPTURE_IMAGE = new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_MEDIA_IMAGES};
        }
        PERMISSION_CAPTURE_VIDEO = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PERMISSION_CAPTURE_VIDEO = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_VIDEO};
        }
        PERMISSION_CONTACT = new String[]{Manifest.permission.READ_CONTACTS};
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

    public void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    public static boolean verifyPermissions(int[] grantResults) {
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

    public boolean shouldShowRequestForLocationPermission(Activity activity) {
        return shouldShowRequestListPermissionRationale(activity, PERMISSIONS_LOCATION);
    }

    public boolean shouldShowRequestForRecordAudioPermission(Activity activity) {
        return shouldShowRequestListPermissionRationale(activity, PERMISSIONS_RECORD_AUDIO);
    }

    public boolean shouldShowRequestForStoragePermission(Activity activity) {
        return shouldShowRequestListPermissionRationale(activity, PERMISSIONS_STORAGE);
    }

    public boolean shouldShowRequestForCaptureImagePermission(Activity activity) {
        return shouldShowRequestListPermissionRationale(activity, PERMISSION_CAPTURE_IMAGE);
    }

    public boolean shouldShowRequestForCaptureVideoPermission(Activity activity) {
        return shouldShowRequestListPermissionRationale(activity, PERMISSION_CAPTURE_VIDEO);
    }

    public boolean shouldShowRequestForContactPermission(Activity activity) {
        return shouldShowRequestListPermissionRationale(activity, PERMISSION_CONTACT);
    }

    public boolean checkSelfForStoragePermission(Context context) {
        return checkSelfListPermission(context, PERMISSIONS_STORAGE);
    }

    public boolean checkSelfForLocationPermission(Context context) {
        return checkSelfListPermission(context, PERMISSIONS_LOCATION);
    }

    public boolean checkSelfForRecordAudioPermission(Context context) {
        return checkSelfListPermission(context, PERMISSIONS_RECORD_AUDIO);
    }

    public boolean checkSelfForCaptureImagePermission(Context context) {
        return checkSelfListPermission(context, PERMISSION_CAPTURE_IMAGE);
    }

    public boolean checkSelfForCaptureVideoPermission(Context context) {
        return checkSelfListPermission(context, PERMISSION_CAPTURE_VIDEO);
    }

    public boolean checkSelfForContactPermission(Context context) {
        return checkSelfListPermission(context, PERMISSION_CONTACT);
    }

    public boolean checkSelfListPermission(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public boolean shouldShowRequestListPermissionRationale(Activity activity, String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }
}
