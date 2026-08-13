package com.stringee.apptoappcallsample.stringee.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import com.stringee.apptoappcallsample.stringee.common.CallConstants;
import com.stringee.apptoappcallsample.stringee.common.CallUtils;
import com.stringee.apptoappcallsample.stringee.manager.ActivePushCall;
import com.stringee.apptoappcallsample.stringee.manager.StringeeCallManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/** Parses Stringee call pushes and delegates ownership decisions to the call manager. */
public class StringeeFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        StringeeCallManager.getInstance(this).registerPushToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> remoteData = remoteMessage.getData();
        if (remoteData.isEmpty() || remoteData.get("stringeePushNotification") == null) {
            return;
        }
        String data = remoteData.get("data");
        if (CallUtils.isEmpty(data)) {
            return;
        }
        try {
            JSONObject object = new JSONObject(data);
            String status = object.optString("callStatus", "");
            String callId = object.optString("callId", "");
            if ("ended".equals(status) || "busy".equals(status)
                    || "agentEnded".equals(status)) {
                StringeeCallManager.getInstance(this).handleTerminalPush(callId);
                return;
            }
            StringeeCallManager manager = StringeeCallManager.getInstance(this);
            if (!"started".equals(status) || CallUtils.isEmpty(callId)
                    || manager.hasActiveCall()
                    || !ActivePushCall.claim(callId)) {
                return;
            }
            String from = "";
            String alias = "";
            Object fromValue = object.opt("from");
            if (fromValue instanceof JSONObject) {
                JSONObject fromObject = (JSONObject) fromValue;
                from = fromObject.optString("number", "");
                alias = fromObject.optString("alias", "");
            } else {
                from = object.optString("from", "");
            }
            boolean video = object.optBoolean("isVideoCall",
                    object.optBoolean("videoCall", false));
            IncomingCallService.showFromPush(this, from, alias, video, callId);
        } catch (JSONException exception) {
            Log.e(CallConstants.TAG, "Invalid Stringee call push", exception);
        }
    }
}
