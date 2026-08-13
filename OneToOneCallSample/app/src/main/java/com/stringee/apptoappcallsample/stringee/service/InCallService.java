package com.stringee.apptoappcallsample.stringee.service;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;

import com.stringee.apptoappcallsample.stringee.common.CallConstants;
import com.stringee.apptoappcallsample.stringee.common.CallNotificationManager;
import com.stringee.apptoappcallsample.stringee.common.CallUtils;
import com.stringee.apptoappcallsample.stringee.manager.CallSession;
import com.stringee.apptoappcallsample.stringee.manager.StringeeCallManager;

/** Call-scoped foreground service used only while an owned call is active. */
public class InCallService extends Service {
    private static final String EXTRA_START_ATTEMPT = "in_call_start_attempt";
    private static final long[] RETRY_DELAYS_MS = {500L, 1_500L, 3_000L};
    private static final long FINAL_START_GRACE_MS = 500L;
    private static final Handler START_HANDLER = new Handler(Looper.getMainLooper());
    private static final CallServiceOwnership OWNERSHIP = new CallServiceOwnership();
    private static Runnable pendingRetry;

    private long ownedGeneration = -1;

    public static void startOrUpdate(Context context, long generation) {
        if (context == null || generation < 0) {
            return;
        }
        if (OWNERSHIP.isRequested(generation)) {
            return;
        }
        if (!OWNERSHIP.isRunning(generation) && !OWNERSHIP.request(generation)) {
            return;
        }
        if (pendingRetry != null) {
            START_HANDLER.removeCallbacks(pendingRetry);
            pendingRetry = null;
        }
        startAttempt(context.getApplicationContext(), generation, 0);
    }

    private static void startAttempt(Context context, long generation, int attempt) {
        Intent intent = new Intent(context, InCallService.class)
                .setAction(CallConstants.ACTION_START_IN_CALL)
                .putExtra(CallConstants.EXTRA_SESSION_GENERATION, generation)
                .putExtra(EXTRA_START_ATTEMPT, attempt);
        try {
            ContextCompat.startForegroundService(context, intent);
            scheduleStartVerification(context, generation, attempt);
        } catch (RuntimeException exception) {
            CallUtils.reportException(InCallService.class, exception);
            handleStartFailure(context, generation, attempt);
        }
    }

    private static void scheduleStartVerification(Context context, long generation,
                                                  int attempt) {
        if (pendingRetry != null) {
            START_HANDLER.removeCallbacks(pendingRetry);
        }
        pendingRetry = () -> {
            pendingRetry = null;
            if (OWNERSHIP.isRunning(generation)) {
                return;
            }
            handleStartFailure(context, generation, attempt);
        };
        long delay = attempt < RETRY_DELAYS_MS.length
                ? RETRY_DELAYS_MS[attempt] : FINAL_START_GRACE_MS;
        START_HANDLER.postDelayed(pendingRetry, delay);
    }

    private static void handleStartFailure(Context context, long generation, int attempt) {
        OWNERSHIP.release(generation);
        StringeeCallManager manager = StringeeCallManager.getInstance(context);
        boolean sessionOwned = manager.ownsSessionGeneration(generation);
        if (CallServiceRetryPolicy.shouldRetry(
                manager.isAppForeground(), sessionOwned, attempt)) {
            if (pendingRetry != null) {
                START_HANDLER.removeCallbacks(pendingRetry);
            }
            pendingRetry = () -> {
                pendingRetry = null;
                if (manager.ownsSessionGeneration(generation)
                        && OWNERSHIP.request(generation)) {
                    startAttempt(context, generation, attempt + 1);
                }
            };
            START_HANDLER.postDelayed(pendingRetry, RETRY_DELAYS_MS[attempt]);
            return;
        }
        manager.onInCallServiceStartFailed(generation);
    }

    public static void stop(Context context, long generation) {
        if (!OWNERSHIP.release(generation)) {
            return;
        }
        if (pendingRetry != null) {
            START_HANDLER.removeCallbacks(pendingRetry);
            pendingRetry = null;
        }
        context.stopService(new Intent(context, InCallService.class));
        CallNotificationManager.getInstance(context).cancel(
                CallConstants.ONGOING_NOTIFICATION_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !CallConstants.ACTION_START_IN_CALL.equals(intent.getAction())) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        long generation = intent.getLongExtra(CallConstants.EXTRA_SESSION_GENERATION, -1);
        int attempt = intent.getIntExtra(EXTRA_START_ATTEMPT, 0);
        StringeeCallManager manager = StringeeCallManager.getInstance(this);
        CallSession session = manager.getSession();
        if (session == null || !manager.ownsSessionGeneration(generation)) {
            OWNERSHIP.release(generation);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        Notification notification = CallNotificationManager.getInstance(this).buildOngoing(
                session.getFrom(), session.isVideoCall(), session.getStartedAt(), generation);
        if (!promote(notification)) {
            stopSelf(startId);
            handleStartFailure(this, generation, attempt);
            return START_NOT_STICKY;
        }
        if (!OWNERSHIP.isRunning(generation) && !OWNERSHIP.markRunning(generation)) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        ownedGeneration = generation;
        if (pendingRetry != null) {
            START_HANDLER.removeCallbacks(pendingRetry);
            pendingRetry = null;
        }
        return START_NOT_STICKY;
    }

    private boolean promote(Notification notification) {
        int type = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
        }
        try {
            ServiceCompat.startForeground(this, CallConstants.ONGOING_NOTIFICATION_ID,
                    notification, type);
            return true;
        } catch (RuntimeException exception) {
            CallUtils.reportException(InCallService.class, exception);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    && type != ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL) {
                try {
                    ServiceCompat.startForeground(this, CallConstants.ONGOING_NOTIFICATION_ID,
                            notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL);
                    return true;
                } catch (RuntimeException fallbackException) {
                    CallUtils.reportException(InCallService.class, fallbackException);
                }
            }
            return false;
        }
    }

    @Override
    public void onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        if (OWNERSHIP.release(ownedGeneration)) {
            CallNotificationManager.getInstance(this).cancel(
                    CallConstants.ONGOING_NOTIFICATION_ID);
        }
        ownedGeneration = -1;
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
