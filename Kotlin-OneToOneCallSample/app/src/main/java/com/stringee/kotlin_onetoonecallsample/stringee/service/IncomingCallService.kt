package com.stringee.kotlin_onetoonecallsample.stringee.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallAudioManager
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallConstants
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallEngine
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallNotificationManager
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallPermissions
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallUtils
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall
import com.stringee.kotlin_onetoonecallsample.stringee.manager.StringeeCallManager

/** Call-scoped foreground service that posts and updates the incoming-call notification. */
class IncomingCallService : Service() {
    private var serviceCallId = ""
    private var servicePushGeneration: Long = -1
    private var foregroundStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.getAction() == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val action = intent.getAction()
        if (CallConstants.ACTION_HIDE_INCOMING == action) {
            hideOwnedNotification(intent, startId)
            return START_NOT_STICKY
        }
        if (CallConstants.ACTION_INCOMING_PUSH != action && CallConstants.ACTION_INCOMING_SDK != action) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val callId = intent.getStringExtra(CallConstants.EXTRA_CALL_ID)
        val generation = intent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1)
        if (!ActivePushCall.owns(callId, generation)) {
            stopIfNoActiveCall(startId)
            return START_NOT_STICKY
        }
        serviceCallId = if (callId == null) "" else callId
        servicePushGeneration = generation
        if (!CallPermissions.canPostNotifications(this)) {
            failSdkIncomingIfNeeded(action, intent, callId)
            clear(this, callId)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val sdkReady = CallConstants.ACTION_INCOMING_SDK == action
        val sessionGeneration = intent.getLongExtra(
            CallConstants.EXTRA_SESSION_GENERATION, -1
        )
        if (sdkReady && !StringeeCallManager.Companion.getInstance(this)
                .ownsSession(sessionGeneration, callId)
        ) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        val notificationManager: CallNotificationManager =
            CallNotificationManager.Companion.getInstance(this)
        val notification = if (sdkReady)
            notificationManager.buildIncomingFromSdk(
                intent.getStringExtra(CallConstants.EXTRA_CALLER),
                intent.getStringExtra(CallConstants.EXTRA_CALLER_ALIAS),
                intent.getBooleanExtra(CallConstants.EXTRA_VIDEO, false), callId,
                generation, sessionGeneration
            )
        else
            notificationManager.buildIncomingFromPush(
                intent.getStringExtra(CallConstants.EXTRA_CALLER),
                intent.getStringExtra(CallConstants.EXTRA_CALLER_ALIAS),
                intent.getBooleanExtra(CallConstants.EXTRA_VIDEO, false), callId,
                generation
            )
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
        else
            0
        try {
            ServiceCompat.startForeground(
                this, CallConstants.INCOMING_NOTIFICATION_ID,
                notification, type
            )
            foregroundStarted = true
        } catch (exception: RuntimeException) {
            CallUtils.reportException(IncomingCallService::class.java, exception)
            failSdkIncomingIfNeeded(action, intent, callId)
            clear(this, callId)
            stopSelf(startId)
            return START_NOT_STICKY
        }
        CallAudioManager.Companion.getInstance(this).startRinging()
        if (CallConstants.ACTION_INCOMING_PUSH == action) {
            StringeeCallManager.Companion.getInstance(this).connectSavedTokenForPush()
        }
        return START_NOT_STICKY
    }

    private fun failSdkIncomingIfNeeded(action: String?, intent: Intent, callId: String?) {
        if (CallConstants.ACTION_INCOMING_SDK == action) {
            StringeeCallManager.Companion.getInstance(this).onIncomingNotificationStartFailed(
                intent.getLongExtra(CallConstants.EXTRA_SESSION_GENERATION, -1), callId
            )
        }
    }

    private fun hideOwnedNotification(intent: Intent, startId: Int) {
        val callId = intent.getStringExtra(CallConstants.EXTRA_CALL_ID)
        val generation = intent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1)
        if (!ActivePushCall.owns(callId, generation)) {
            stopIfNoActiveCall(startId)
            return
        }
        CallAudioManager.Companion.getInstance(this).stopRinging()
        CallNotificationManager.Companion.getInstance(this).cancel(
            CallConstants.INCOMING_NOTIFICATION_ID
        )
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        // Reject was requested before the SDK callback. Keep ActivePushCall so that
        // finishPreparingIncoming() can consume the action exactly once.
        serviceCallId = ""
        servicePushGeneration = ActivePushCall.generation
        stopSelf(startId)
    }

    private fun stopIfNoActiveCall(startId: Int) {
        if (!ActivePushCall.hasActiveCall()) {
            stopSelf(startId)
        }
    }

    override fun onDestroy() {
        if (ActivePushCall.owns(serviceCallId, servicePushGeneration)) {
            ActivePushCall.clearIfMatches(serviceCallId)
            CallAudioManager.Companion.getInstance(this).stopRinging()
            CallNotificationManager.Companion.getInstance(this).cancel(
                CallConstants.INCOMING_NOTIFICATION_ID
            )
        }
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        fun showFromPush(
            context: Context, from: String?, alias: String?, videoCall: Boolean,
            callId: String?
        ) {
            val intent: Intent = baseIntent(
                context, CallConstants.ACTION_INCOMING_PUSH, from, alias,
                videoCall, callId, null
            )
            start(context, intent)
        }

        fun showFromSdk(
            context: Context, from: String?, alias: String?, videoCall: Boolean,
            callId: String?, engine: CallEngine?, sessionGeneration: Long
        ) {
            val intent: Intent = baseIntent(
                context, CallConstants.ACTION_INCOMING_SDK, from, alias,
                videoCall, callId, engine
            )
            intent.putExtra(CallConstants.EXTRA_SESSION_GENERATION, sessionGeneration)
            start(context, intent)
        }

        private fun baseIntent(
            context: Context?, action: String?, from: String?, alias: String?,
            videoCall: Boolean, callId: String?, engine: CallEngine?
        ): Intent {
            val intent = Intent(context, IncomingCallService::class.java)
                .setAction(action)
                .putExtra(CallConstants.EXTRA_CALLER, from)
                .putExtra(CallConstants.EXTRA_CALLER_ALIAS, alias)
                .putExtra(CallConstants.EXTRA_VIDEO, videoCall)
                .putExtra(CallConstants.EXTRA_CALL_ID, callId)
                .putExtra(CallConstants.EXTRA_CALL_GENERATION, ActivePushCall.generation)
            if (engine != null) {
                intent.putExtra(CallConstants.EXTRA_ENGINE, engine.name)
            }
            return intent
        }

        private fun start(context: Context, intent: Intent) {
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (exception: RuntimeException) {
                CallUtils.reportException(IncomingCallService::class.java, exception)
                handleStartFailure(context, intent)
            }
        }

        private fun handleStartFailure(context: Context, intent: Intent) {
            val callId = intent.getStringExtra(CallConstants.EXTRA_CALL_ID)
            if (CallConstants.ACTION_INCOMING_SDK == intent.getAction()) {
                StringeeCallManager.Companion.getInstance(context)
                    .onIncomingNotificationStartFailed(
                        intent.getLongExtra(CallConstants.EXTRA_SESSION_GENERATION, -1), callId
                    )
            }
            clear(context, callId)
        }

        fun hide(context: Context, callId: String?) {
            if (!ActivePushCall.isCurrent(callId)) {
                return
            }
            CallAudioManager.Companion.getInstance(context).stopRinging()
            CallNotificationManager.Companion.getInstance(context).cancel(
                CallConstants.INCOMING_NOTIFICATION_ID
            )
            val intent = Intent(context, IncomingCallService::class.java)
                .setAction(CallConstants.ACTION_HIDE_INCOMING)
                .putExtra(CallConstants.EXTRA_CALL_ID, callId)
                .putExtra(
                    CallConstants.EXTRA_CALL_GENERATION,
                    ActivePushCall.generation
                )
            try {
                context.startService(intent)
            } catch (exception: RuntimeException) {
                CallUtils.reportException(IncomingCallService::class.java, exception)
            }
        }

        fun clear(context: Context, callId: String?) {
            if (!CallUtils.isEmpty(callId)) {
                ActivePushCall.clearIfMatches(callId)
            }
            CallAudioManager.Companion.getInstance(context).stopRinging()
            CallNotificationManager.Companion.getInstance(context).cancel(
                CallConstants.INCOMING_NOTIFICATION_ID
            )
            context.stopService(Intent(context, IncomingCallService::class.java))
        }
    }
}
