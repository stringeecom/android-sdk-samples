package com.stringee.chat.ui.kit.commons.utils;

import android.Manifest;
import android.app.Activity;

import com.stringee.chat.ui.kit.activity.StringeeLocationActivity;

/**
 * Created by sunil on 22/sticker_icon_1/16.
 */
public class StringeePermissions {
    private Activity activity;

    public StringeePermissions(Activity activity) {
        this.activity = activity;
    }

    public void requestStoragePermissions() {
        PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSIONS_STORAGE, PermissionsUtils.REQUEST_STORAGE);
    }

    public void requestLocationPermissions() {
        PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSIONS_LOCATION, PermissionsUtils.REQUEST_LOCATION);
    }

    public void requestAudio() {
        PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSIONS_RECORD_AUDIO, PermissionsUtils.REQUEST_AUDIO_RECORD);
    }

    public void requestCameraPermission() {
        PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSION_CAMERA, PermissionsUtils.REQUEST_CAMERA);
    }

    public void requestContactPermission() {
        PermissionsUtils.requestPermissions(activity, PermissionsUtils.PERMISSION_CONTACT, PermissionsUtils.REQUEST_CONTACT);
    }

    public void requestCameraAndRecordPermission() {
        PermissionsUtils.requestPermissions(activity, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, PermissionsUtils.REQUEST_CAMERA_AUDIO);
    }

    public void checkRuntimePermissionForLocationActivity() {
        if (PermissionsUtils.checkSelfPermissionForLocation(activity)) {
            requestLocationPermissions();
        } else {
            ((StringeeLocationActivity) activity).processingLocation();
        }
    }
}
