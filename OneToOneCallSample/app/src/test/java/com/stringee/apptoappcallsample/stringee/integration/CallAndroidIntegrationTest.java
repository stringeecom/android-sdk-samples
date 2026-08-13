package com.stringee.apptoappcallsample.stringee.integration;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;

import androidx.test.core.app.ApplicationProvider;

import com.stringee.apptoappcallsample.stringee.common.CallConstants;
import com.stringee.apptoappcallsample.stringee.common.CallNotificationManager;
import com.stringee.apptoappcallsample.activity.MainActivity;
import com.stringee.apptoappcallsample.stringee.activity.CallActivity;
import com.stringee.apptoappcallsample.stringee.manager.ActivePushCall;
import com.stringee.apptoappcallsample.stringee.service.InCallService;
import com.stringee.apptoappcallsample.stringee.service.IncomingCallService;
import com.stringee.apptoappcallsample.stringee.service.MediaProjectionCallService;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class CallAndroidIntegrationTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @After
    public void tearDown() {
        ActivePushCall.clear();
    }

    @Test
    public void pushIncomingNotificationTargetsMainActivityAndCarriesPushOwnership() {
        assertTrue(ActivePushCall.claim("call-1"));
        long generation = ActivePushCall.getGeneration();

        Notification notification = CallNotificationManager.getInstance(context)
                .buildIncomingFromPush("alice", "Alice", true, "call-1", generation);

        assertNotNull(notification.contentIntent);
        assertNotNull(notification.actions);
        assertEquals(2, notification.actions.length);
        boolean foundAnswer = false;
        boolean foundReject = false;
        for (Notification.Action action : notification.actions) {
            PendingIntent pendingIntent = action.actionIntent;
            Intent savedIntent = shadowOf(pendingIntent).getSavedIntent();
            if (CallConstants.ACTION_REJECT.equals(savedIntent.getAction())) {
                foundReject = ActivePushCall.owns(
                        savedIntent.getStringExtra(CallConstants.EXTRA_CALL_ID),
                        savedIntent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1));
            }
            if (ActivePushCall.PendingAction.ANSWER.name().equals(
                    savedIntent.getStringExtra(CallConstants.EXTRA_PENDING_ACTION))) {
                foundAnswer = ActivePushCall.owns(
                        savedIntent.getStringExtra(CallConstants.EXTRA_CALL_ID),
                        savedIntent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1));
            }
        }
        assertTrue(foundAnswer);
        assertTrue(foundReject);
        Intent contentIntent = shadowOf(notification.contentIntent).getSavedIntent();
        assertEquals(MainActivity.class.getName(),
                contentIntent.getComponent().getClassName());
    }

    @Test
    public void sdkIncomingNotificationTargetsCallActivityAndCarriesBothOwners() {
        assertTrue(ActivePushCall.claim("call-2"));
        long pushGeneration = ActivePushCall.getGeneration();
        long sessionGeneration = 81;

        Notification notification = CallNotificationManager.getInstance(context)
                .buildIncomingFromSdk("bob", "Bob", false, "call-2",
                        pushGeneration, sessionGeneration);

        Intent contentIntent = shadowOf(notification.contentIntent).getSavedIntent();
        assertEquals(CallActivity.class.getName(),
                contentIntent.getComponent().getClassName());
        assertEquals(sessionGeneration, contentIntent.getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1));
        assertNotNull(notification.actions);
        assertEquals(2, notification.actions.length);
        for (Notification.Action action : notification.actions) {
            Intent actionIntent = shadowOf(action.actionIntent).getSavedIntent();
            assertEquals("call-2", actionIntent.getStringExtra(CallConstants.EXTRA_CALL_ID));
            assertEquals(pushGeneration, actionIntent.getLongExtra(
                    CallConstants.EXTRA_CALL_GENERATION, -1));
            assertEquals(sessionGeneration, actionIntent.getLongExtra(
                    CallConstants.EXTRA_SESSION_GENERATION, -1));
        }
    }

    @Test
    public void manifestDeclaresRequiredForegroundServiceTypes() throws Exception {
        PackageManager packageManager = context.getPackageManager();
        ServiceInfo incoming = packageManager.getServiceInfo(
                new ComponentName(context, IncomingCallService.class), 0);
        ServiceInfo inCall = packageManager.getServiceInfo(
                new ComponentName(context, InCallService.class), 0);
        ServiceInfo projection = packageManager.getServiceInfo(
                new ComponentName(context, MediaProjectionCallService.class), 0);

        assertTrue((incoming.getForegroundServiceType()
                & FOREGROUND_SERVICE_TYPE_PHONE_CALL) != 0);
        assertTrue((inCall.getForegroundServiceType()
                & FOREGROUND_SERVICE_TYPE_PHONE_CALL) != 0);
        assertTrue((inCall.getForegroundServiceType()
                & FOREGROUND_SERVICE_TYPE_MICROPHONE) != 0);
        assertTrue((projection.getForegroundServiceType()
                & FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) != 0);
    }

    @Test
    public void sampleDoesNotDeclareAConnectionKeepingForegroundService() throws Exception {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(), PackageManager.GET_SERVICES);
        Set<String> callScopedForegroundServices = new HashSet<>(Arrays.asList(
                IncomingCallService.class.getName(),
                InCallService.class.getName(),
                MediaProjectionCallService.class.getName()));

        assertNotNull(packageInfo.services);
        for (ServiceInfo service : packageInfo.services) {
            assertFalse((service.getForegroundServiceType()
                    & FOREGROUND_SERVICE_TYPE_SPECIAL_USE) != 0);
            if (service.getForegroundServiceType() != 0) {
                assertTrue(callScopedForegroundServices.contains(service.name));
            }
        }
    }

    @Test
    public void ongoingNotificationCarriesTheOwningSessionGeneration() {
        long generation = 42;

        Notification notification = CallNotificationManager.getInstance(context)
                .buildOngoing("Alice", false, 0, generation);

        assertNotNull(notification.actions);
        assertEquals(1, notification.actions.length);
        Intent hangUpIntent = shadowOf(notification.actions[0].actionIntent).getSavedIntent();
        assertEquals(CallConstants.ACTION_HANG_UP, hangUpIntent.getAction());
        assertEquals(generation, hangUpIntent.getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1));
        Intent contentIntent = shadowOf(notification.contentIntent).getSavedIntent();
        assertEquals(generation, contentIntent.getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1));
    }
}
