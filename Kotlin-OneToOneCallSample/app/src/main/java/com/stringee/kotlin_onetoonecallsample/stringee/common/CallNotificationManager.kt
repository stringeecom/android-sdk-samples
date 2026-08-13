package com.stringee.kotlin_onetoonecallsample.stringee.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.stringee.kotlin_onetoonecallsample.R
import com.stringee.kotlin_onetoonecallsample.activity.MainActivity
import com.stringee.kotlin_onetoonecallsample.stringee.activity.CallActivity
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.PendingAction
import com.stringee.kotlin_onetoonecallsample.stringee.receiver.CallActionReceiver
import kotlin.concurrent.Volatile

/** Builds and updates incoming, ongoing-call, and screen-sharing notifications. */
class CallNotificationManager private constructor(context: Context) {
    private val context: Context
    private val notificationManager: NotificationManager?

    init {
        this.context = context.getApplicationContext()
        notificationManager = this.context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager?
    }

    fun buildIncomingFromPush(
        from: String?, alias: String?, videoCall: Boolean,
        callId: String?, pushGeneration: Long
    ): Notification {
        return buildIncoming(from, alias, videoCall, callId, pushGeneration, -1, false)
    }

    fun buildIncomingFromSdk(
        from: String?, alias: String?, videoCall: Boolean,
        callId: String?, pushGeneration: Long,
        sessionGeneration: Long
    ): Notification {
        return buildIncoming(
            from, alias, videoCall, callId, pushGeneration,
            sessionGeneration, true
        )
    }

    private fun buildIncoming(
        from: String?, alias: String?, videoCall: Boolean,
        callId: String?, pushGeneration: Long,
        sessionGeneration: Long, sdkReady: Boolean
    ): Notification {
        createChannels()
        val displayName = if (CallUtils.isEmpty(alias)) from else alias
        val person = Person.Builder()
            .setName(displayName)
            .setImportant(true)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .build()

        val content = if (sdkReady)
            callActivityIntent(false, callId, pushGeneration, sessionGeneration, 2101)
        else
            mainActivityIntent(false, callId, pushGeneration, 2101)
        val answer = if (sdkReady)
            callActivityIntent(true, callId, pushGeneration, sessionGeneration, 2102)
        else
            mainActivityIntent(true, callId, pushGeneration, 2102)
        val rejectIntent = Intent(context, CallActionReceiver::class.java)
            .setAction(CallConstants.ACTION_REJECT)
            .putExtra(CallConstants.EXTRA_CALL_ID, callId)
            .putExtra(CallConstants.EXTRA_CALL_GENERATION, pushGeneration)
            .putExtra(CallConstants.EXTRA_SESSION_GENERATION, sessionGeneration)
        if (!sdkReady) {
            rejectIntent.putExtra(
                CallConstants.EXTRA_PENDING_ACTION,
                PendingAction.REJECT.name
            )
        }
        val reject = PendingIntent.getBroadcast(
            context, 2103, rejectIntent,
            pendingIntentFlags()
        )

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(
            context, INCOMING_CHANNEL
        )
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(person, reject, answer)
                    .setIsVideo(videoCall)
            )
            .addPerson(person)
            .setSmallIcon(R.mipmap.icon)
            .setContentTitle(displayName)
            .setContentText("Incoming call from " + from)
            .setContentIntent(content)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVibrate(LongArray(0))
            .setShowWhen(false)
        builder.setFullScreenIntent(content, true)
        return builder.build()
    }

    fun buildOngoing(
        name: String?, videoCall: Boolean, startedAt: Long,
        generation: Long
    ): Notification {
        createChannels()
        val displayName: String = (if (CallUtils.isEmpty(name)) "Stringee call" else name)!!
        val person = Person.Builder().setName(displayName).setImportant(true).build()
        val contentIntent = Intent(context, CallActivity::class.java)
            .addFlags(
                (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            .putExtra(CallConstants.EXTRA_SESSION_GENERATION, generation)
        val content = PendingIntent.getActivity(
            context, 2201, contentIntent,
            pendingIntentFlags()
        )
        val hangUpIntent = Intent(context, CallActionReceiver::class.java)
            .setAction(CallConstants.ACTION_HANG_UP)
            .putExtra(CallConstants.EXTRA_SESSION_GENERATION, generation)
        val hangUp = PendingIntent.getBroadcast(
            context, 2202, hangUpIntent,
            pendingIntentFlags()
        )
        val builder: NotificationCompat.Builder = NotificationCompat.Builder(
            context, ONGOING_CHANNEL
        )
            .setStyle(
                NotificationCompat.CallStyle.forOngoingCall(person, hangUp)
                    .setIsVideo(videoCall)
            )
            .addPerson(person)
            .setSmallIcon(R.mipmap.icon)
            .setContentTitle(displayName)
            .setContentText("Ongoing call")
            .setContentIntent(content)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setUsesChronometer(startedAt > 0)
            .setShowWhen(startedAt > 0)
        if (startedAt > 0) {
            builder.setWhen(startedAt)
        }
        return builder.build()
    }

    fun buildMediaProjection(): Notification {
        createChannels()
        val contentIntent = Intent(context, CallActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val content = PendingIntent.getActivity(
            context, 2301, contentIntent,
            pendingIntentFlags()
        )
        return NotificationCompat.Builder(context, MEDIA_CHANNEL)
            .setSmallIcon(R.mipmap.icon)
            .setContentTitle("Capturing screen")
            .setContentIntent(content)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .build()
    }

    fun cancel(notificationId: Int) {
        if (notificationManager != null) {
            notificationManager.cancel(notificationId)
        }
    }

    private fun mainActivityIntent(
        answer: Boolean, callId: String?, generation: Long,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(
                (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            .putExtra(CallConstants.EXTRA_CALL_ID, callId)
            .putExtra(CallConstants.EXTRA_CALL_GENERATION, generation)
        if (answer) {
            intent.putExtra(
                CallConstants.EXTRA_PENDING_ACTION,
                PendingAction.ANSWER.name
            )
        }
        return PendingIntent.getActivity(context, requestCode, intent, pendingIntentFlags())
    }

    private fun callActivityIntent(
        answer: Boolean, callId: String?, pushGeneration: Long,
        sessionGeneration: Long, requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, CallActivity::class.java)
            .addFlags(
                (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            .putExtra(CallConstants.EXTRA_CALL_ID, callId)
            .putExtra(CallConstants.EXTRA_CALL_GENERATION, pushGeneration)
            .putExtra(CallConstants.EXTRA_SESSION_GENERATION, sessionGeneration)
            .putExtra(CallConstants.EXTRA_ANSWER, answer)
        return PendingIntent.getActivity(context, requestCode, intent, pendingIntentFlags())
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager == null) {
            return
        }
        val incoming = NotificationChannel(
            INCOMING_CHANNEL,
            "Incoming calls", NotificationManager.IMPORTANCE_HIGH
        )
        incoming.setSound(null, null)
        incoming.setDescription("Incoming Stringee calls")
        notificationManager.createNotificationChannel(incoming)

        val ongoing = NotificationChannel(
            ONGOING_CHANNEL,
            "Ongoing calls", NotificationManager.IMPORTANCE_DEFAULT
        )
        ongoing.setSound(null, null)
        ongoing.setDescription("Ongoing Stringee calls")
        notificationManager.createNotificationChannel(ongoing)

        val media = NotificationChannel(
            MEDIA_CHANNEL,
            "Screen sharing", NotificationManager.IMPORTANCE_DEFAULT
        )
        media.setSound(null, null)
        media.setDescription("Stringee screen sharing")
        notificationManager.createNotificationChannel(media)
    }

    companion object {
        private const val INCOMING_CHANNEL = "com.stringee.onetoonecallsample.incoming"
        private const val ONGOING_CHANNEL = "com.stringee.onetoonecallsample.ongoing"
        private const val MEDIA_CHANNEL = "com.stringee.onetoonecallsample.media"

        @Volatile
        private var instance: CallNotificationManager? = null
        @JvmStatic fun getInstance(context: Context): CallNotificationManager {
            if (instance == null) {
                synchronized(CallNotificationManager::class.java) {
                    if (instance == null) {
                        instance = CallNotificationManager(context)
                    }
                }
            }
            return requireNotNull(instance)
        }
    }
}
