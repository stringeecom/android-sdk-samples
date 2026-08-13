package com.stringee.apptoappcallsample.stringee.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.NotificationManager;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32)
public class CallPermissionsTest {
    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void notificationAccessIncludesTheSystemNotificationToggle() {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        shadowOf(manager).setNotificationsEnabled(true);
        assertTrue(CallPermissions.canPostNotifications(context));

        shadowOf(manager).setNotificationsEnabled(false);
        assertFalse(CallPermissions.canPostNotifications(context));
    }
}
