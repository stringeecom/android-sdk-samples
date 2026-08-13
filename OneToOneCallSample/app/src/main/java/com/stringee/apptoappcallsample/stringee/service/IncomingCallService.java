package com.stringee.apptoappcallsample.stringee.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.stringee.apptoappcallsample.stringee.common.CallAudioManager;
import com.stringee.apptoappcallsample.stringee.common.CallConstants;
import com.stringee.apptoappcallsample.stringee.common.CallEngine;
import com.stringee.apptoappcallsample.stringee.common.CallNotificationManager;
import com.stringee.apptoappcallsample.stringee.common.CallPermissions;
import com.stringee.apptoappcallsample.stringee.common.CallUtils;
import com.stringee.apptoappcallsample.stringee.manager.ActivePushCall;
import com.stringee.apptoappcallsample.stringee.manager.StringeeCallManager;

/** Call-scoped foreground service that posts and updates the incoming-call notification. */
public class IncomingCallService extends Service {
    private String serviceCallId = "";
    private long servicePushGeneration = -1;
    private boolean foregroundStarted;

    public static void showFromPush(Context context, String from, String alias, boolean videoCall,
                             String callId) {
        Intent intent = baseIntent(context, CallConstants.ACTION_INCOMING_PUSH, from, alias,
                videoCall, callId, null);
        start(context, intent);
    }

    public static void showFromSdk(Context context, String from, String alias, boolean videoCall,
                           String callId, CallEngine engine, long sessionGeneration) {
        Intent intent = baseIntent(context, CallConstants.ACTION_INCOMING_SDK, from, alias,
                videoCall, callId, engine);
        intent.putExtra(CallConstants.EXTRA_SESSION_GENERATION, sessionGeneration);
        start(context, intent);
    }

    private static Intent baseIntent(Context context, String action, String from, String alias,
                                     boolean videoCall, String callId, CallEngine engine) {
        Intent intent = new Intent(context, IncomingCallService.class)
                .setAction(action)
                .putExtra(CallConstants.EXTRA_CALLER, from)
                .putExtra(CallConstants.EXTRA_CALLER_ALIAS, alias)
                .putExtra(CallConstants.EXTRA_VIDEO, videoCall)
                .putExtra(CallConstants.EXTRA_CALL_ID, callId)
                .putExtra(CallConstants.EXTRA_CALL_GENERATION, ActivePushCall.getGeneration());
        if (engine != null) {
            intent.putExtra(CallConstants.EXTRA_ENGINE, engine.name());
        }
        return intent;
    }

    private static void start(Context context, Intent intent) {
        try {
            ContextCompat.startForegroundService(context, intent);
        } catch (RuntimeException exception) {
            CallUtils.reportException(IncomingCallService.class, exception);
            handleStartFailure(context, intent);
        }
    }

    private static void handleStartFailure(Context context, Intent intent) {
        String callId = intent.getStringExtra(CallConstants.EXTRA_CALL_ID);
        if (CallConstants.ACTION_INCOMING_SDK.equals(intent.getAction())) {
            StringeeCallManager.getInstance(context).onIncomingNotificationStartFailed(
                    intent.getLongExtra(CallConstants.EXTRA_SESSION_GENERATION, -1), callId);
        }
        clear(context, callId);
    }

    public static void hide(Context context, String callId) {
        if (!ActivePushCall.isCurrent(callId)) {
            return;
        }
        CallAudioManager.getInstance(context).stopRinging();
        CallNotificationManager.getInstance(context).cancel(
                CallConstants.INCOMING_NOTIFICATION_ID);
        Intent intent = new Intent(context, IncomingCallService.class)
                .setAction(CallConstants.ACTION_HIDE_INCOMING)
                .putExtra(CallConstants.EXTRA_CALL_ID, callId)
                .putExtra(CallConstants.EXTRA_CALL_GENERATION,
                        ActivePushCall.getGeneration());
        try {
            context.startService(intent);
        } catch (RuntimeException exception) {
            CallUtils.reportException(IncomingCallService.class, exception);
        }
    }

    public static void clear(Context context, String callId) {
        if (!CallUtils.isEmpty(callId)) {
            ActivePushCall.clearIfMatches(callId);
        }
        CallAudioManager.getInstance(context).stopRinging();
        CallNotificationManager.getInstance(context).cancel(
                CallConstants.INCOMING_NOTIFICATION_ID);
        context.stopService(new Intent(context, IncomingCallService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (CallConstants.ACTION_HIDE_INCOMING.equals(action)) {
            hideOwnedNotification(intent, startId);
            return START_NOT_STICKY;
        }
        if (!CallConstants.ACTION_INCOMING_PUSH.equals(action)
                && !CallConstants.ACTION_INCOMING_SDK.equals(action)) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        String callId = intent.getStringExtra(CallConstants.EXTRA_CALL_ID);
        long generation = intent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1);
        if (!ActivePushCall.owns(callId, generation)) {
            stopIfNoActiveCall(startId);
            return START_NOT_STICKY;
        }
        serviceCallId = callId == null ? "" : callId;
        servicePushGeneration = generation;
        if (!CallPermissions.canPostNotifications(this)) {
            failSdkIncomingIfNeeded(action, intent, callId);
            clear(this, callId);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        boolean sdkReady = CallConstants.ACTION_INCOMING_SDK.equals(action);
        long sessionGeneration = intent.getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1);
        if (sdkReady && !StringeeCallManager.getInstance(this)
                .ownsSession(sessionGeneration, callId)) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        CallNotificationManager notificationManager = CallNotificationManager.getInstance(this);
        Notification notification = sdkReady
                ? notificationManager.buildIncomingFromSdk(
                        intent.getStringExtra(CallConstants.EXTRA_CALLER),
                        intent.getStringExtra(CallConstants.EXTRA_CALLER_ALIAS),
                        intent.getBooleanExtra(CallConstants.EXTRA_VIDEO, false), callId,
                        generation, sessionGeneration)
                : notificationManager.buildIncomingFromPush(
                        intent.getStringExtra(CallConstants.EXTRA_CALLER),
                        intent.getStringExtra(CallConstants.EXTRA_CALLER_ALIAS),
                        intent.getBooleanExtra(CallConstants.EXTRA_VIDEO, false), callId,
                        generation);
        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL : 0;
        try {
            ServiceCompat.startForeground(this, CallConstants.INCOMING_NOTIFICATION_ID,
                    notification, type);
            foregroundStarted = true;
        } catch (RuntimeException exception) {
            CallUtils.reportException(IncomingCallService.class, exception);
            failSdkIncomingIfNeeded(action, intent, callId);
            clear(this, callId);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        CallAudioManager.getInstance(this).startRinging();
        if (CallConstants.ACTION_INCOMING_PUSH.equals(action)) {
            StringeeCallManager.getInstance(this).connectSavedTokenForPush();
        }
        return START_NOT_STICKY;
    }

    private void failSdkIncomingIfNeeded(String action, Intent intent, String callId) {
        if (CallConstants.ACTION_INCOMING_SDK.equals(action)) {
            StringeeCallManager.getInstance(this).onIncomingNotificationStartFailed(
                    intent.getLongExtra(CallConstants.EXTRA_SESSION_GENERATION, -1), callId);
        }
    }

    private void hideOwnedNotification(Intent intent, int startId) {
        String callId = intent.getStringExtra(CallConstants.EXTRA_CALL_ID);
        long generation = intent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1);
        if (!ActivePushCall.owns(callId, generation)) {
            stopIfNoActiveCall(startId);
            return;
        }
        CallAudioManager.getInstance(this).stopRinging();
        CallNotificationManager.getInstance(this).cancel(
                CallConstants.INCOMING_NOTIFICATION_ID);
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
            foregroundStarted = false;
        }
        // Reject was requested before the SDK callback. Keep ActivePushCall so that
        // finishPreparingIncoming() can consume the action exactly once.
        serviceCallId = "";
        servicePushGeneration = ActivePushCall.getGeneration();
        stopSelf(startId);
    }

    private void stopIfNoActiveCall(int startId) {
        if (!ActivePushCall.hasActiveCall()) {
            stopSelf(startId);
        }
    }

    @Override
    public void onDestroy() {
        if (ActivePushCall.owns(serviceCallId, servicePushGeneration)) {
            ActivePushCall.clearIfMatches(serviceCallId);
            CallAudioManager.getInstance(this).stopRinging();
            CallNotificationManager.getInstance(this).cancel(
                    CallConstants.INCOMING_NOTIFICATION_ID);
        }
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
            foregroundStarted = false;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
