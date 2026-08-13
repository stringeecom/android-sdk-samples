package com.stringee.kotlin_onetoonecallsample.stringee.common

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallPermissions.canPostNotifications
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [32])
class CallPermissionsTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun notificationAccessIncludesTheSystemNotificationToggle() {
        val manager =
            context.getSystemService<NotificationManager?>(NotificationManager::class.java)
        Shadows.shadowOf(manager).setNotificationsEnabled(true)
        Assert.assertTrue(canPostNotifications(context))

        Shadows.shadowOf(manager).setNotificationsEnabled(false)
        Assert.assertFalse(canPostNotifications(context))
    }
}
