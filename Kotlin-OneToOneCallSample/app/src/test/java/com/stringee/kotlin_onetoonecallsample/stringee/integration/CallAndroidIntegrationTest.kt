package com.stringee.kotlin_onetoonecallsample.stringee.integration

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import com.stringee.kotlin_onetoonecallsample.activity.MainActivity
import com.stringee.kotlin_onetoonecallsample.stringee.activity.CallActivity
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallConstants
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallNotificationManager.Companion.getInstance
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.claim
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.clear
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.generation
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.owns
import com.stringee.kotlin_onetoonecallsample.stringee.service.InCallService
import com.stringee.kotlin_onetoonecallsample.stringee.service.IncomingCallService
import com.stringee.kotlin_onetoonecallsample.stringee.service.MediaProjectionCallService
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Arrays

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CallAndroidIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Context>()

    @After
    fun tearDown() {
        clear()
    }

    @Test
    fun pushIncomingNotificationTargetsMainActivityAndCarriesPushOwnership() {
        Assert.assertTrue(claim("call-1"))
        val generation = generation

        val notification = getInstance(context)
            .buildIncomingFromPush("alice", "Alice", true, "call-1", generation)

        Assert.assertNotNull(notification.contentIntent)
        Assert.assertNotNull(notification.actions)
        Assert.assertEquals(2, notification.actions.size.toLong())
        var foundAnswer = false
        var foundReject = false
        for (action in notification.actions) {
            val pendingIntent = action.actionIntent
            val savedIntent = Shadows.shadowOf(pendingIntent).getSavedIntent()
            if (CallConstants.ACTION_REJECT == savedIntent.getAction()) {
                foundReject = owns(
                    savedIntent.getStringExtra(CallConstants.EXTRA_CALL_ID),
                    savedIntent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1)
                )
            }
            if (ActivePushCall.PendingAction.ANSWER.name == savedIntent.getStringExtra(CallConstants.EXTRA_PENDING_ACTION)) {
                foundAnswer = owns(
                    savedIntent.getStringExtra(CallConstants.EXTRA_CALL_ID),
                    savedIntent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1)
                )
            }
        }
        Assert.assertTrue(foundAnswer)
        Assert.assertTrue(foundReject)
        val contentIntent = Shadows.shadowOf(notification.contentIntent).getSavedIntent()
        Assert.assertEquals(
            MainActivity::class.java.getName(),
            contentIntent.getComponent()!!.getClassName()
        )
    }

    @Test
    fun sdkIncomingNotificationTargetsCallActivityAndCarriesBothOwners() {
        Assert.assertTrue(claim("call-2"))
        val pushGeneration = generation
        val sessionGeneration: Long = 81

        val notification = getInstance(context)
            .buildIncomingFromSdk(
                "bob", "Bob", false, "call-2",
                pushGeneration, sessionGeneration
            )

        val contentIntent = Shadows.shadowOf(notification.contentIntent).getSavedIntent()
        Assert.assertEquals(
            CallActivity::class.java.getName(),
            contentIntent.getComponent()!!.getClassName()
        )
        Assert.assertEquals(
            sessionGeneration, contentIntent.getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1
            )
        )
        Assert.assertNotNull(notification.actions)
        Assert.assertEquals(2, notification.actions.size.toLong())
        for (action in notification.actions) {
            val actionIntent = Shadows.shadowOf(action.actionIntent).getSavedIntent()
            Assert.assertEquals("call-2", actionIntent.getStringExtra(CallConstants.EXTRA_CALL_ID))
            Assert.assertEquals(
                pushGeneration, actionIntent.getLongExtra(
                    CallConstants.EXTRA_CALL_GENERATION, -1
                )
            )
            Assert.assertEquals(
                sessionGeneration, actionIntent.getLongExtra(
                    CallConstants.EXTRA_SESSION_GENERATION, -1
                )
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun manifestDeclaresRequiredForegroundServiceTypes() {
        val packageManager = context.getPackageManager()
        val incoming = packageManager.getServiceInfo(
            ComponentName(context, IncomingCallService::class.java), 0
        )
        val inCall = packageManager.getServiceInfo(
            ComponentName(context, InCallService::class.java), 0
        )
        val projection = packageManager.getServiceInfo(
            ComponentName(context, MediaProjectionCallService::class.java), 0
        )

        Assert.assertTrue(
            (incoming.getForegroundServiceType()
                    and ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL) != 0
        )
        Assert.assertTrue(
            (inCall.getForegroundServiceType()
                    and ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL) != 0
        )
        Assert.assertTrue(
            (inCall.getForegroundServiceType()
                    and ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE) != 0
        )
        Assert.assertTrue(
            (projection.getForegroundServiceType()
                    and ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION) != 0
        )
    }

    @Test
    @Throws(Exception::class)
    fun sampleDoesNotDeclareAConnectionKeepingForegroundService() {
        val packageInfo = context.getPackageManager().getPackageInfo(
            context.getPackageName(), PackageManager.GET_SERVICES
        )
        val callScopedForegroundServices: MutableSet<String?> = HashSet<String?>(
            Arrays.asList<String?>(
                IncomingCallService::class.java.getName(),
                InCallService::class.java.getName(),
                MediaProjectionCallService::class.java.getName()
            )
        )

        Assert.assertNotNull(packageInfo.services)
        for (service in packageInfo.services!!) {
            Assert.assertFalse(
                (service.getForegroundServiceType()
                        and ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) != 0
            )
            if (service.getForegroundServiceType() != 0) {
                Assert.assertTrue(callScopedForegroundServices.contains(service.name))
            }
        }
    }

    @Test
    fun ongoingNotificationCarriesTheOwningSessionGeneration() {
        val generation: Long = 42

        val notification = getInstance(context)
            .buildOngoing("Alice", false, 0, generation)

        Assert.assertNotNull(notification.actions)
        Assert.assertEquals(1, notification.actions.size.toLong())
        val hangUpIntent = Shadows.shadowOf(notification.actions[0].actionIntent).getSavedIntent()
        Assert.assertEquals(CallConstants.ACTION_HANG_UP, hangUpIntent.getAction())
        Assert.assertEquals(
            generation, hangUpIntent.getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1
            )
        )
        val contentIntent = Shadows.shadowOf(notification.contentIntent).getSavedIntent()
        Assert.assertEquals(
            generation, contentIntent.getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1
            )
        )
    }
}
