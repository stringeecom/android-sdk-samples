package com.stringee.apptoappcallsample.stringee.manager;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.stringee.apptoappcallsample.stringee.activity.CallActivity;
import com.stringee.apptoappcallsample.stringee.common.CallConstants;
import com.stringee.apptoappcallsample.stringee.common.CallEngine;
import com.stringee.apptoappcallsample.stringee.common.CallPermissions;
import com.stringee.apptoappcallsample.stringee.common.CallStatus;
import com.stringee.apptoappcallsample.stringee.common.CallUtils;
import com.stringee.apptoappcallsample.stringee.common.ConnectionState;
import com.stringee.apptoappcallsample.stringee.common.StringeeCallConfig;
import com.stringee.apptoappcallsample.stringee.common.TokenStore;
import com.stringee.apptoappcallsample.stringee.listener.StringeeCallListener;
import com.stringee.apptoappcallsample.stringee.service.InCallService;
import com.stringee.apptoappcallsample.stringee.service.IncomingCallService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.listener.StringeeConnectionListener;

import org.json.JSONObject;

/**
 * Application-scoped facade for all Stringee call operations in the Java sample.
 *
 * <p>The host activity uses this class to connect, make calls, inspect connection state, and
 * request incoming-call permissions. SDK call objects, notifications, push ownership, and
 * call-scoped foreground services remain implementation details.</p>
 */
public final class StringeeCallManager implements CallSession.Owner {
    public static final int ERROR_INVALID_TOKEN = 100;
    public static final int ERROR_NOT_CONNECTED = 101;
    public static final int ERROR_ALREADY_IN_CALL = 102;
    public static final int ERROR_INVALID_CONFIG = 103;

    private static volatile StringeeCallManager instance;

    private final Context context;
    private final TokenStore tokenStore;
    private final Application application;
    private StringeeClient client;
    private StringeeConnectionListener connectionListener;
    private StringeeCallListener listener;
    private String pendingAccessToken = "";
    private ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private boolean initialized;
    private boolean autoConnectAttempted;
    private int startedActivityCount;
    private long sessionGeneration;
    private CallSession session;
    private Application.ActivityLifecycleCallbacks lifecycleCallbacks;

    private StringeeCallManager(Context context) {
        this.context = context.getApplicationContext();
        tokenStore = new TokenStore(context);
        application = (Application) this.context;
    }

    /** Returns the process-wide manager using the application context. */
    public static StringeeCallManager getInstance(Context context) {
        if (instance == null) {
            synchronized (StringeeCallManager.class) {
                if (instance == null) {
                    instance = new StringeeCallManager(context);
                }
            }
        }
        return instance;
    }

    /** Registers the host listener and auto-connects the last successful token once per process. */
    public void initialize(StringeeCallListener listener) {
        this.listener = listener;
        if (!initialized) {
            initialized = true;
            registerActivityLifecycle();
        }
        if (!autoConnectAttempted) {
            autoConnectAttempted = true;
            String savedToken = tokenStore.getToken();
            if (!CallUtils.isEmpty(savedToken)) {
                connect(savedToken);
            }
        }
    }

    /** Starts a connection. The supplied token is persisted only after connection succeeds. */
    public void connect(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isEmpty()) {
            reportError("connect", new StringeeError(ERROR_INVALID_TOKEN,
                    "Access token is required"));
            return;
        }
        if (client != null && client.isConnected()) {
            if (normalized.equals(pendingAccessToken)
                    || normalized.equals(tokenStore.getToken())) {
                notifyConnection(ConnectionState.CONNECTED, client.getUserId());
                return;
            }
            disposeClient();
        }
        if (client != null && connectionState == ConnectionState.CONNECTING) {
            if (isSamePendingConnection(normalized, pendingAccessToken, connectionState)) {
                notifyConnection(ConnectionState.CONNECTING, "");
                return;
            }
            disposeClient();
        }
        pendingAccessToken = normalized;
        ensureClient();
        notifyConnection(ConnectionState.CONNECTING, "");
        if (!client.isConnected()) {
            try {
                client.connect(normalized);
            } catch (RuntimeException exception) {
                CallUtils.reportException(StringeeCallManager.class, exception);
                notifyConnection(ConnectionState.ERROR, "");
                reportError("connect", new StringeeError(ERROR_INVALID_TOKEN,
                        exception.getMessage() == null ? "Unable to connect"
                                : exception.getMessage()));
            }
        }
    }

    public void connectSavedTokenForPush() {
        if (client != null && client.isConnected()) {
            return;
        }
        String token = tokenStore.getToken();
        if (CallUtils.isEmpty(token)) {
            reportError("incomingPush", new StringeeError(ERROR_INVALID_TOKEN,
                    "No previously connected token is available"));
            IncomingCallService.clear(context, ActivePushCall.getCallId());
            return;
        }
        connect(token);
    }

    private void ensureClient() {
        if (client != null) {
            return;
        }
        client = new StringeeClient(context);
        connectionListener = new StringeeConnectionListener() {
            @Override
            public void onConnectionConnected(StringeeClient stringeeClient,
                                              boolean isReconnecting) {
                CallUtils.runOnUiThread(() -> {
                    if (client != stringeeClient) {
                        return;
                    }
                    if (!CallUtils.isEmpty(pendingAccessToken)) {
                        tokenStore.saveConnectedToken(pendingAccessToken);
                    }
                    notifyConnection(ConnectionState.CONNECTED, stringeeClient.getUserId());
                    registerCurrentPushToken();
                });
            }

            @Override
            public void onConnectionDisconnected(StringeeClient stringeeClient,
                                                 boolean isReconnecting) {
                CallUtils.runOnUiThread(() -> {
                    if (client == stringeeClient) {
                        notifyConnection(stateAfterDisconnect(isReconnecting), "");
                    }
                });
            }

            @Override
            public void onIncomingCall(StringeeCall call) {
                CallUtils.runOnUiThread(() -> prepareIncoming(call));
            }

            @Override
            public void onIncomingCall2(StringeeCall2 call) {
                CallUtils.runOnUiThread(() -> prepareIncoming(call));
            }

            @Override
            public void onConnectionError(StringeeClient stringeeClient, StringeeError error) {
                CallUtils.runOnUiThread(() -> {
                    if (client == stringeeClient) {
                        notifyConnection(ConnectionState.ERROR, "");
                        reportError("connect", error);
                    }
                });
            }

            @Override
            public void onRequestNewToken(StringeeClient stringeeClient) {
                CallUtils.runOnUiThread(() -> {
                    if (listener != null) {
                        listener.onRequestNewToken();
                    }
                });
            }

            @Override
            public void onCustomMessage(String from, JSONObject message) {
                Log.d(CallConstants.TAG, "onCustomMessage from " + from);
            }

            @Override
            public void onTopicMessage(String from, JSONObject message) {
                Log.d(CallConstants.TAG, "onTopicMessage from " + from);
            }
        };
        client.addConnectionListener(connectionListener);
    }

    /** Prepares an outgoing call and opens the permission-aware call activity. */
    public void makeCall(StringeeCallConfig config, StatusListener callback) {
        if (!isConnected()) {
            dispatchOperationError(callback, "makeCall", new StringeeError(ERROR_NOT_CONNECTED,
                    "StringeeClient is not connected"));
            return;
        }
        if (session != null) {
            dispatchOperationError(callback, "makeCall", new StringeeError(ERROR_ALREADY_IN_CALL,
                    "Another call is active"));
            return;
        }
        if (config == null || CallUtils.isEmpty(config.getTo())) {
            dispatchOperationError(callback, "makeCall", new StringeeError(ERROR_INVALID_CONFIG,
                    "A recipient is required"));
            return;
        }
        session = new CallSession(context, this, ++sessionGeneration, CallStatus.CALLING);
        session.prepareOutgoing(client, config);
        session.setOutgoingCallback(callback);
        InCallService.startOrUpdate(context, sessionGeneration);
        openCallActivity(false);
    }

    private void prepareIncoming(StringeeCall call) {
        if (session != null) {
            call.reject(new StatusListener() {
                @Override
                public void onSuccess() {
                }
            });
            IncomingCallService.clear(context, call.getCallId());
            return;
        }
        if (!claimIncoming(call.getCallId())) {
            call.reject(new StatusListener() {
                @Override
                public void onSuccess() {
                }
            });
            return;
        }
        session = new CallSession(context, this, ++sessionGeneration, CallStatus.INCOMING);
        session.prepareIncoming(call);
        finishPreparingIncoming();
    }

    private void prepareIncoming(StringeeCall2 call) {
        if (session != null) {
            call.reject(new StatusListener() {
                @Override
                public void onSuccess() {
                }
            });
            IncomingCallService.clear(context, call.getCallId());
            return;
        }
        if (!claimIncoming(call.getCallId())) {
            call.reject(new StatusListener() {
                @Override
                public void onSuccess() {
                }
            });
            return;
        }
        session = new CallSession(context, this, ++sessionGeneration, CallStatus.INCOMING);
        session.prepareIncoming(call);
        finishPreparingIncoming();
    }

    private boolean claimIncoming(String callId) {
        return ActivePushCall.isCurrent(callId) || ActivePushCall.claim(callId);
    }

    private void finishPreparingIncoming() {
        session.ringing();
        IncomingCallService.showFromSdk(context, session.getFrom(), "", session.isVideoCall(),
                session.getCallId(), session.getEngine(), sessionGeneration);
        ActivePushCall.PendingAction action = ActivePushCall.consumeAction(session.getCallId());
        if (action == ActivePushCall.PendingAction.REJECT) {
            session.reject();
        } else if (action == ActivePushCall.PendingAction.ANSWER) {
            openCallActivity(true);
        } else if (isAppForeground()) {
            openCallActivity(false);
        }
    }

    /** Consumes Answer/Reject metadata when the host is opened from a notification. */
    public void handleLaunchIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        String actionName = intent.getStringExtra(CallConstants.EXTRA_PENDING_ACTION);
        String callId = intent.getStringExtra(CallConstants.EXTRA_CALL_ID);
        long generation = intent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1);
        if (!CallUtils.isEmpty(actionName)) {
            try {
                ActivePushCall.PendingAction action = ActivePushCall.PendingAction.valueOf(
                        actionName);
                ActivePushCall.requestAction(callId, generation, action);
            } catch (IllegalArgumentException exception) {
                CallUtils.reportException(StringeeCallManager.class, exception);
            }
        }
        if (session != null && session.isIncoming()
                && (CallUtils.isEmpty(callId) || callId.equals(session.getCallId()))) {
            ActivePushCall.PendingAction action = ActivePushCall.consumeAction(
                    session.getCallId());
            openCallActivity(action == ActivePushCall.PendingAction.ANSWER);
        }
    }

    private void openCallActivity(boolean answer) {
        if (session == null) {
            return;
        }
        Intent intent = new Intent(context, CallActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(CallConstants.EXTRA_ANSWER, answer)
                .putExtra(CallConstants.EXTRA_SESSION_GENERATION, sessionGeneration);
        context.startActivity(intent);
    }

    public void startPreparedOutgoing() {
        if (session == null) {
            return;
        }
        session.startOutgoing();
    }

    /** Disconnects the current session while retaining the last successful token. */
    public void disconnect() {
        if (session != null) {
            session.endForDisconnect();
        }
        if (client != null) {
            disposeClient();
        }
        notifyConnection(ConnectionState.DISCONNECTED, "");
    }

    /** Releases lifecycle callbacks, the active call, client, and singleton instance. */
    public void release() {
        disconnect();
        if (lifecycleCallbacks != null) {
            application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks);
        }
        lifecycleCallbacks = null;
        listener = null;
        initialized = false;
        instance = null;
    }

    /** Returns whether the Stringee client is connected. */
    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    /** Returns the connected Stringee user ID, or an empty string. */
    public String getConnectedUserId() {
        return isConnected() ? client.getUserId() : "";
    }

    /** Returns the access token from the most recent successful connection. */
    public String getSavedToken() {
        return tokenStore.getToken();
    }

    private void disposeClient() {
        StringeeClient disposedClient = client;
        client = null;
        if (disposedClient == null) {
            return;
        }
        if (connectionListener != null) {
            disposedClient.removeConnectionListener(connectionListener);
            connectionListener = null;
        }
        disposedClient.disconnect();
    }

    public boolean canPostNotifications() {
        return CallPermissions.canPostNotifications(context);
    }

    public boolean canUseFullScreenIntent() {
        return CallPermissions.canUseFullScreenIntent(context);
    }

    public void requestNotificationPermission(Activity activity) {
        CallPermissions.requestNotificationPermission(activity);
    }

    public void openFullScreenIntentSettings(Activity activity) {
        CallPermissions.openFullScreenIntentSettings(activity);
    }

    public void openAppSettings(Activity activity) {
        CallPermissions.openAppSettings(activity);
    }

    public CallSession getSession() {
        return session;
    }

    public boolean hasActiveCall() {
        return session != null;
    }

    static boolean isSamePendingConnection(String requestedToken, String activeToken,
                                           ConnectionState state) {
        return state == ConnectionState.CONNECTING && requestedToken != null
                && requestedToken.equals(activeToken);
    }

    static ConnectionState stateAfterDisconnect(boolean reconnecting) {
        return reconnecting ? ConnectionState.CONNECTING : ConnectionState.DISCONNECTED;
    }

    public void handleTerminalPush(String callId) {
        if (CallUtils.isEmpty(callId) || !ActivePushCall.isCurrent(callId)) {
            return;
        }
        if (session != null && callId.equals(session.getCallId())) {
            session.release();
        } else {
            IncomingCallService.clear(context, callId);
        }
    }

    boolean isCurrentSession(CallSession candidate, long generation) {
        return session == candidate && sessionGeneration == generation;
    }

    public boolean ownsSessionGeneration(long generation) {
        return session != null && sessionGeneration == generation;
    }

    public boolean ownsSession(long generation, String callId) {
        return ownsSessionGeneration(generation) && session != null
                && (CallUtils.isEmpty(callId) || callId.equals(session.getCallId()));
    }

    public void onInCallServiceStartFailed(long generation) {
        if (ownsSessionGeneration(generation)) {
            session.onInCallServiceStartFailed();
        }
    }

    public void onIncomingNotificationStartFailed(long generation, String callId) {
        if (ownsSession(generation, callId) && session.isIncoming()) {
            session.reject();
        }
    }

    private void registerCurrentPushToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || CallUtils.isEmpty(task.getResult())) {
                return;
            }
            registerPushToken(task.getResult());
        });
    }

    public void registerPushToken(String token) {
        tokenStore.saveFcmToken(token);
        if (!isConnected()) {
            return;
        }
        client.registerPushToken(token, new StatusListener() {
            @Override
            public void onSuccess() {
                Log.d(CallConstants.TAG, "Push token registered");
            }

            @Override
            public void onError(StringeeError error) {
                reportError("registerPushToken", error);
            }
        });
    }

    private void registerActivityLifecycle() {
        lifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle state) {
            }

            @Override
            public void onActivityStarted(Activity activity) {
                startedActivityCount++;
            }

            @Override
            public void onActivityResumed(Activity activity) {
            }

            @Override
            public void onActivityPaused(Activity activity) {
            }

            @Override
            public void onActivityStopped(Activity activity) {
                startedActivityCount = Math.max(0, startedActivityCount - 1);
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle state) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        };
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks);
    }

    public boolean isAppForeground() {
        return startedActivityCount > 0;
    }

    private void notifyConnection(ConnectionState state, String userId) {
        connectionState = state;
        if (listener != null) {
            listener.onConnectionStateChanged(state, userId == null ? "" : userId);
        }
    }

    private void reportError(String action, StringeeError error) {
        if (listener != null) {
            listener.onError(action, error);
        }
    }

    private void dispatchOperationError(StatusListener callback, String action,
                                        StringeeError error) {
        if (callback != null) {
            callback.onError(error);
        }
        reportError(action, error);
    }

    @Override
    public void onSessionStateChanged(CallSession session, CallStatus status) {
        if (this.session == session && listener != null) {
            listener.onCallStateChanged(status);
        }
    }

    @Override
    public void onSessionError(CallSession session, String action, StringeeError error) {
        if (this.session == session) {
            reportError(action, error);
        }
    }

    @Override
    public void onSessionReleased(CallSession session) {
        if (this.session == session) {
            this.session = null;
        }
    }
}
