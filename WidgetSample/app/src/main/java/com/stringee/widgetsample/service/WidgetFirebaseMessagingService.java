package com.stringee.widgetsample.service;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import org.json.JSONException;
import org.json.JSONObject;
import com.stringee.widgetsample.manager.WidgetCallCoordinator;

/**
 * Receives Firebase token updates and Stringee call pushes for the Widget sample.
 *
 * <p>The service parses only call ownership metadata and delegates all lifecycle decisions to
 * {@link WidgetCallCoordinator}; it never keeps the Widget connection alive.</p>
 */
public class WidgetFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        WidgetCallCoordinator.getInstance(this).registerPushToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        String data = message.getData().get("data");
        if (!message.getData().containsKey("stringeePushNotification") || data == null) {
            return;
        }
        try {
            JSONObject payload = new JSONObject(data);
            String status = payload.optString("callStatus", "");
            String callId = payload.optString("callId", "");
            WidgetCallCoordinator coordinator = WidgetCallCoordinator.getInstance(this);
            if ("started".equals(status)) {
                JSONObject fromObject = payload.optJSONObject("from");
                String from = fromObject == null ? payload.optString("from", "")
                        : fromObject.optString("number", "");
                String alias = fromObject == null ? "" : fromObject.optString("alias", "");
                boolean video = payload.optBoolean("isVideoCall",
                        payload.optBoolean("videoCall", false));
                coordinator.handleStartedPush(callId, from, alias, video);
            } else if ("ended".equals(status) || "busy".equals(status)
                    || "agentEnded".equals(status)) {
                coordinator.handleTerminalPush(callId);
            }
        } catch (JSONException ignored) {
            // Ignore payloads that are not Stringee call data.
        }
    }
}
