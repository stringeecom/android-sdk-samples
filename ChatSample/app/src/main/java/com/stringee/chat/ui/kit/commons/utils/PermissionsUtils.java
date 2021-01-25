package com.stringee.chat.ui.kit.commons.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by sunil on 20/sticker_icon_1/16.
 */
public class PermissionsUtils {

    public static final int REQUEST_STORAGE = 0;
    public static final int REQUEST_LOCATION = 1;
    public static final int REQUEST_CAMERA = 2;
    public static final int REQUEST_AUDIO_RECORD = 3;
    public static final int REQUEST_CONTACT = 4;
    public static final int REQUEST_CAMERA_AUDIO = 5;
    public static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_FINE_LOCATION};
    public static String[] PERMISSIONS_STORAGE = {Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE};
    public static String[] PERMISSIONS_RECORD_AUDIO = {Manifest.permission.RECORD_AUDIO};
    public static String[] PERMISSION_CAMERA = {Manifest.permission.CAMERA};
    public static String[] PERMISSION_CONTACT = {Manifest.permission.READ_CONTACTS};

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

    public static boolean shouldShowRequestForLocationPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION));
    }

    public static boolean shouldShowRequestForAudioPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.RECORD_AUDIO));
    }


    public static boolean shouldShowRequestForStoragePermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE));
    }

    public static boolean shouldShowRequestForCallPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.CALL_PHONE));
    }

    public static boolean shouldShowRequestForCameraPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.CAMERA));
    }

    public static boolean shouldShowRequestForContactPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.READ_CONTACTS));
    }

    public static boolean shouldShowRequestForVideoPermission(Activity activity) {
        return (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.CAMERA)
                && ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.RECORD_AUDIO));
    }

    public static boolean checkSelfForStoragePermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED);
    }


    public static boolean checkSelfPermissionForLocation(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfPermissionForAudioRecording(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfForCallPermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfForCameraPermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkSelfForContactPermission(Activity activity) {
        return (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED);
    }

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    public static boolean isAudioRecordingPermissionGranted(Context context) {
        String permission = "android.permission.RECORD_AUDIO";
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isCameraPermissionGranted(Context context) {
        int res = context.checkCallingOrSelfPermission(Manifest.permission.CAMERA);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isCallPermissionGranted(Context context) {
        int res = context.checkCallingOrSelfPermission(Manifest.permission.CALL_PHONE);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean checkPermissionForCameraAndMicrophone(Context context) {
        int resultCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
        return (!(resultCamera == PackageManager.PERMISSION_GRANTED &&
                resultMic == PackageManager.PERMISSION_GRANTED));
    }

    public static boolean isStoragePermissionGranted(Context context) {
        int resWrite = context.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int resRead = context.checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        return (resWrite == PackageManager.PERMISSION_GRANTED && resRead == PackageManager.PERMISSION_GRANTED);
    }
}
