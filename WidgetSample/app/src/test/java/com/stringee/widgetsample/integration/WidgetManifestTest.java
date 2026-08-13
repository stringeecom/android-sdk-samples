package com.stringee.widgetsample.integration;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;

import com.stringee.widget.call.StringeeCallActivity;
import com.stringee.widget.call.receiver.CallActionReceiver;
import com.stringee.widget.call.receiver.GSMCallStateReceiver;
import com.stringee.widget.common.CallForegroundService;
import com.stringee.widgetsample.service.WidgetFirebaseMessagingService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WidgetManifestTest {
    @Test
    public void widgetCallComponentsArePrivateAndCallScoped() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        PackageManager packageManager = context.getPackageManager();

        ActivityInfo activity = packageManager.getActivityInfo(
                new ComponentName(context, StringeeCallActivity.class), 0);
        assertFalse(activity.exported);

        ActivityInfo callAction = packageManager.getReceiverInfo(
                new ComponentName(context, CallActionReceiver.class), 0);
        ActivityInfo gsmState = packageManager.getReceiverInfo(
                new ComponentName(context, GSMCallStateReceiver.class), 0);
        assertFalse(callAction.exported);
        assertFalse(gsmState.exported);

        ServiceInfo callService = packageManager.getServiceInfo(
                new ComponentName(context, CallForegroundService.class), 0);
        assertFalse(callService.exported);
        int expectedTypes = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                | ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
        assertEquals(expectedTypes, callService.getForegroundServiceType());

        ServiceInfo messagingService = packageManager.getServiceInfo(
                new ComponentName(context, WidgetFirebaseMessagingService.class), 0);
        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE,
                messagingService.getForegroundServiceType());
    }

    @Test
    public void permissionsMatchWidgetContractWithoutOverlayPermission() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_PERMISSIONS);
        assertNotNull(packageInfo.requestedPermissions);
        assertTrue(Arrays.asList(packageInfo.requestedPermissions)
                .contains(Manifest.permission.FOREGROUND_SERVICE_PHONE_CALL));
        assertTrue(Arrays.asList(packageInfo.requestedPermissions)
                .contains(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE));
        assertTrue(Arrays.asList(packageInfo.requestedPermissions)
                .contains(Manifest.permission.MANAGE_OWN_CALLS));
        assertFalse(Arrays.asList(packageInfo.requestedPermissions)
                .contains(Manifest.permission.SYSTEM_ALERT_WINDOW));
    }
}
