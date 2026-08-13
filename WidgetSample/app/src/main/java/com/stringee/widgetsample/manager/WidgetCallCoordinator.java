package com.stringee.widgetsample.manager;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.widget.StringeeListener;
import com.stringee.widget.StringeeWidget;
import com.stringee.widget.call.CallConfig;
import com.stringee.widget.call.CallType;
import com.stringee.widget.common.NotificationService;
import com.stringee.widgetsample.common.WidgetTokenStore;
import com.stringee.widgetsample.listener.WidgetCallListener;

/**
 * Single integration facade for {@link StringeeWidget}.
 *
 * <p>This coordinator owns connection lifecycle, successful-token persistence, push ownership,
 * outgoing calls, and foreground/background incoming-call presentation. Activities and services
 * call this class instead of accessing the widget singleton directly.</p>
 */
public final class WidgetCallCoordinator implements Application.ActivityLifecycleCallbacks {
    /** Connection states exposed to the sample UI. */
    public enum ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }

    private static volatile WidgetCallCoordinator instance;
    private final Application application;
    private final StringeeWidget widget;
    private final WidgetTokenStore tokenStore;
    private WidgetCallListener listener;
    private ConnectionState state = ConnectionState.DISCONNECTED;
    private String pendingToken = "";
    private boolean autoConnectAttempted;
    private int startedActivities;

    private WidgetCallCoordinator(Context context) {
        application = (Application) context.getApplicationContext();
        widget = StringeeWidget.getInstance(application);
        tokenStore = new WidgetTokenStore(application);
        application.registerActivityLifecycleCallbacks(this);
        widget.setListener(widgetListener);
    }

    /** Returns the application-scoped Widget coordinator. */
    public static WidgetCallCoordinator getInstance(Context context) {
        if (instance == null) {
            synchronized (WidgetCallCoordinator.class) {
                if (instance == null) {
                    instance = new WidgetCallCoordinator(context);
                }
            }
        }
        return instance;
    }

    /**
     * Registers the host listener and auto-connects the last successfully connected token once.
     *
     * @param listener listener receiving connection state and integration messages
     */
    public void initialize(@Nullable WidgetCallListener listener) {
        this.listener = listener;
        widget.setListener(widgetListener);
        notifyConnection();
        if (!autoConnectAttempted) {
            autoConnectAttempted = true;
            String savedToken = getSavedToken();
            if (!savedToken.isEmpty()) {
                connect(savedToken);
            }
        }
    }

    /** Connects with an access token and saves it only after Widget reports success. */
    public void connect(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isEmpty()) {
            message("Access token is empty");
            return;
        }
        if (widget.isConnected()) {
            if (normalized.equals(pendingToken) || normalized.equals(getSavedToken())) {
                state = ConnectionState.CONNECTED;
                notifyConnection();
                return;
            }
            widget.disconnect();
        }
        if (state == ConnectionState.CONNECTING && normalized.equals(pendingToken)) {
            return;
        }
        pendingToken = normalized;
        state = ConnectionState.CONNECTING;
        notifyConnection();
        widget.connect(normalized);
    }

    /** Restores the last successful connection when an incoming push creates the process. */
    public void connectSavedTokenForPush() {
        if (widget.isConnected()) {
            return;
        }
        String token = getSavedToken();
        if (token.isEmpty()) {
            message("Incoming push ignored: no successfully connected token");
            ActiveWidgetPush.clear();
            return;
        }
        connect(token);
    }

    /** Disconnects the current Widget session without deleting the saved token. */
    public void disconnect() {
        widget.disconnect();
        state = ConnectionState.DISCONNECTED;
        notifyConnection();
    }

    /** Releases the Widget singleton and unregisters application lifecycle callbacks. */
    public void release() {
        widget.release();
        application.unregisterActivityLifecycleCallbacks(this);
        listener = null;
        instance = null;
    }

    /** Makes an outgoing voice or video call through the Widget API. */
    public void makeCall(String from, String to, boolean videoCall, StatusListener callback) {
        if (!widget.isConnected()) {
            callback.onError(new StringeeError(-1, "Connect before making a call"));
            return;
        }
        if (to == null || to.trim().isEmpty()) {
            callback.onError(new StringeeError(-1, "Recipient is empty"));
            return;
        }
        String caller = from == null ? "" : from.trim();
        if (caller.isEmpty()) {
            caller = widget.getClient().getUserId();
        }
        CallConfig config = new CallConfig(caller, to.trim());
        config.setVideoCall(videoCall);
        widget.makeCall(config, callback);
    }

    /** Returns whether the underlying Widget client is connected. */
    public boolean isConnected() {
        return widget.isConnected();
    }

    /** Returns the token from the most recent successful Widget connection. */
    public String getSavedToken() {
        return tokenStore.getToken();
    }

    /** Claims a started push and reconnects so the SDK can prepare the incoming call wrapper. */
    public void handleStartedPush(String callId, String from, String alias, boolean video) {
        if (widget.isInCall()) {
            return;
        }
        if (ActiveWidgetPush.claim(callId, from, alias, video)) {
            connectSavedTokenForPush();
        }
    }

    /** Clears only the incoming notification owned by the matching terminal call ID. */
    public void handleTerminalPush(String callId) {
        if (!ActiveWidgetPush.clearIfMatches(callId)) {
            return;
        }
        cancelIncomingNotification();
    }

    /** Registers a non-empty Firebase token after the Widget connection is ready. */
    public void registerPushToken(String token) {
        if (!widget.isConnected() || token == null || token.trim().isEmpty()) {
            return;
        }
        widget.registerPushNotification(token, new StatusListener() {
            @Override public void onSuccess() { }
            @Override public void onError(StringeeError error) {
                message("registerPushNotification: " + error.getMessage());
            }
        });
    }

    private void cancelIncomingNotification() {
        NotificationManager manager = (NotificationManager) application.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(NotificationService.INCOMING_CALL_NOTIFICATION_ID);
        }
    }

    private final StringeeListener widgetListener = new StringeeListener() {
        @Override
        public void onConnectionConnected(StringeeWidget emittingWidget) {
            if (!pendingToken.isEmpty()) {
                tokenStore.saveConnectedToken(pendingToken);
            }
            state = ConnectionState.CONNECTED;
            notifyConnection();
            try {
                FirebaseMessaging.getInstance().getToken()
                        .addOnSuccessListener(WidgetCallCoordinator.this::registerPushToken)
                        .addOnFailureListener(error ->
                                message("Firebase token: " + error.getMessage()));
            } catch (IllegalStateException error) {
                message("Firebase is not configured: add app/google-services.json");
            }
        }

        @Override
        public void onConnectionDisconnected(StringeeWidget emittingWidget) {
            state = ConnectionState.DISCONNECTED;
            notifyConnection();
        }

        @Override
        public void onConnectionError(StringeeWidget emittingWidget, StringeeError error) {
            state = ConnectionState.ERROR;
            notifyConnection();
            message("Connection error: " + error.getMessage());
        }

        @Override
        public void onRequestNewToken(StringeeWidget emittingWidget) {
            message("A new Stringee access token is required");
        }

        @Override
        public void onCallStateChange(StringeeWidget emittingWidget, StringeeCall call,
                                      StringeeCall.SignalingState signalingState) {
            message("Call state: " + signalingState);
            clearTerminal(call.getCallId(), signalingState == StringeeCall.SignalingState.ENDED
                    || signalingState == StringeeCall.SignalingState.BUSY);
        }

        @Override
        public void onCallStateChange2(StringeeWidget emittingWidget, StringeeCall2 call,
                                       StringeeCall2.SignalingState signalingState) {
            message("Call2 state: " + signalingState);
            clearTerminal(call.getCallId(), signalingState == StringeeCall2.SignalingState.ENDED
                    || signalingState == StringeeCall2.SignalingState.BUSY);
        }

        @Override
        public void onDisplayCallingActivity(StringeeWidget emittingWidget, CallType callType) {
            if (callType != CallType.INCOMING || startedActivities > 0) {
                emittingWidget.openCallActivity();
                return;
            }
            ActiveWidgetPush.Snapshot push = ActiveWidgetPush.snapshot();
            String from = push == null ? "Stringee user" : push.from;
            String alias = push == null || push.alias.isEmpty() ? from : push.alias;
            emittingWidget.showIncomingCallNotification(from, alias, new StatusListener() {
                @Override public void onSuccess() { }
                @Override public void onError(StringeeError error) {
                    message("Incoming notification: " + error.getMessage());
                }
            });
        }

        @Override
        public void onCallError(StringeeWidget emittingWidget, StringeeError error) {
            message("Call error: " + error.getMessage());
        }
    };

    private void clearTerminal(String callId, boolean terminal) {
        if (terminal && ActiveWidgetPush.clearIfMatches(callId)) {
            cancelIncomingNotification();
        }
    }

    private void notifyConnection() {
        WidgetCallListener current = listener;
        if (current != null) {
            String userId = widget.isConnected() ? widget.getClient().getUserId() : "";
            current.onConnectionChanged(state, userId);
        }
    }

    private void message(String value) {
        WidgetCallListener current = listener;
        if (current != null) {
            current.onMessage(value);
        }
    }

    @Override public void onActivityStarted(@NonNull Activity activity) { startedActivities++; }
    @Override public void onActivityStopped(@NonNull Activity activity) {
        startedActivities = Math.max(0, startedActivities - 1);
    }
    @Override public void onActivityCreated(@NonNull Activity a, Bundle b) { }
    @Override public void onActivityResumed(@NonNull Activity a) { }
    @Override public void onActivityPaused(@NonNull Activity a) { }
    @Override public void onActivitySaveInstanceState(@NonNull Activity a, @NonNull Bundle b) { }
    @Override public void onActivityDestroyed(@NonNull Activity a) { }
}
