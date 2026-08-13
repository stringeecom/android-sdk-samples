package com.stringee.kotlin_onetoonecallsample.stringee.service

import android.Manifest
import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallConstants
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallNotificationManager
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallUtils
import com.stringee.kotlin_onetoonecallsample.stringee.manager.StringeeCallManager

/** Call-scoped foreground service used only while an owned call is active. */
class InCallService : Service() {
    private var ownedGeneration: Long = -1

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || CallConstants.ACTION_START_IN_CALL != intent.getAction()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val generation = intent.getLongExtra(CallConstants.EXTRA_SESSION_GENERATION, -1)
        val attempt = intent.getIntExtra(EXTRA_START_ATTEMPT, 0)
        val manager: StringeeCallManager = StringeeCallManager.Companion.getInstance(this)
        val session = manager.session
        if (session == null || !manager.ownsSessionGeneration(generation)) {
            OWNERSHIP.release(generation)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val notification: Notification =
            CallNotificationManager.Companion.getInstance(this).buildOngoing(
                session.from, session.isVideoCall, session.startedAt, generation
            )
        if (!promote(notification)) {
            stopSelf(startId)
            handleStartFailure(this, generation, attempt)
            return START_NOT_STICKY
        }
        if (!OWNERSHIP.isRunning(generation) && !OWNERSHIP.markRunning(generation)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        ownedGeneration = generation
        if (pendingRetry != null) {
            START_HANDLER.removeCallbacks(pendingRetry!!)
            pendingRetry = null
        }
        return START_NOT_STICKY
    }

    private fun promote(notification: Notification): Boolean {
        var type = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
            && (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED)
        ) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        try {
            ServiceCompat.startForeground(
                this, CallConstants.ONGOING_NOTIFICATION_ID,
                notification, type
            )
            return true
        } catch (exception: RuntimeException) {
            CallUtils.reportException(InCallService::class.java, exception)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && type != ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            ) {
                try {
                    ServiceCompat.startForeground(
                        this, CallConstants.ONGOING_NOTIFICATION_ID,
                        notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                    )
                    return true
                } catch (fallbackException: RuntimeException) {
                    CallUtils.reportException(InCallService::class.java, fallbackException)
                }
            }
            return false
        }
    }

    override fun onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        if (OWNERSHIP.release(ownedGeneration)) {
            CallNotificationManager.Companion.getInstance(this).cancel(
                CallConstants.ONGOING_NOTIFICATION_ID
            )
        }
        ownedGeneration = -1
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        private const val EXTRA_START_ATTEMPT = "in_call_start_attempt"
        private val RETRY_DELAYS_MS = longArrayOf(500L, 1500L, 3000L)
        private const val FINAL_START_GRACE_MS = 500L
        private val START_HANDLER = Handler(Looper.getMainLooper())
        private val OWNERSHIP = CallServiceOwnership()
        private var pendingRetry: Runnable? = null

        fun startOrUpdate(context: Context?, generation: Long) {
            if (context == null || generation < 0) {
                return
            }
            if (OWNERSHIP.isRequested(generation)) {
                return
            }
            if (!OWNERSHIP.isRunning(generation) && !OWNERSHIP.request(generation)) {
                return
            }
            if (pendingRetry != null) {
                START_HANDLER.removeCallbacks(pendingRetry!!)
                pendingRetry = null
            }
            startAttempt(context.getApplicationContext(), generation, 0)
        }

        private fun startAttempt(context: Context, generation: Long, attempt: Int) {
            val intent = Intent(context, InCallService::class.java)
                .setAction(CallConstants.ACTION_START_IN_CALL)
                .putExtra(CallConstants.EXTRA_SESSION_GENERATION, generation)
                .putExtra(EXTRA_START_ATTEMPT, attempt)
            try {
                ContextCompat.startForegroundService(context, intent)
                scheduleStartVerification(context, generation, attempt)
            } catch (exception: RuntimeException) {
                CallUtils.reportException(InCallService::class.java, exception)
                handleStartFailure(context, generation, attempt)
            }
        }

        private fun scheduleStartVerification(
            context: Context, generation: Long,
            attempt: Int
        ) {
            if (pendingRetry != null) {
                START_HANDLER.removeCallbacks(pendingRetry!!)
            }
            pendingRetry = Runnable {
                pendingRetry = null
                if (OWNERSHIP.isRunning(generation)) {
                    return@Runnable
                }
                handleStartFailure(context, generation, attempt)
            }
            val delay: Long = if (attempt < RETRY_DELAYS_MS.size)
                RETRY_DELAYS_MS[attempt]
            else
                FINAL_START_GRACE_MS
            START_HANDLER.postDelayed(pendingRetry!!, delay)
        }

        private fun handleStartFailure(context: Context, generation: Long, attempt: Int) {
            OWNERSHIP.release(generation)
            val manager: StringeeCallManager = StringeeCallManager.Companion.getInstance(context)
            val sessionOwned = manager.ownsSessionGeneration(generation)
            if (CallServiceRetryPolicy.shouldRetry(
                    manager.isAppForeground, sessionOwned, attempt
                )
            ) {
                if (pendingRetry != null) {
                    START_HANDLER.removeCallbacks(pendingRetry!!)
                }
                pendingRetry = Runnable {
                    pendingRetry = null
                    if (manager.ownsSessionGeneration(generation)
                        && OWNERSHIP.request(generation)
                    ) {
                        startAttempt(context, generation, attempt + 1)
                    }
                }
                START_HANDLER.postDelayed(pendingRetry!!, RETRY_DELAYS_MS[attempt])
                return
            }
            manager.onInCallServiceStartFailed(generation)
        }

        fun stop(context: Context, generation: Long) {
            if (!OWNERSHIP.release(generation)) {
                return
            }
            if (pendingRetry != null) {
                START_HANDLER.removeCallbacks(pendingRetry!!)
                pendingRetry = null
            }
            context.stopService(Intent(context, InCallService::class.java))
            CallNotificationManager.Companion.getInstance(context).cancel(
                CallConstants.ONGOING_NOTIFICATION_ID
            )
        }
    }
}
