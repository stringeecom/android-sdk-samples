package com.stringee.callpushnotificationsample.service;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.stringee.callpushnotificationsample.common.Common;
import com.stringee.callpushnotificationsample.common.NotificationUtils;
import com.stringee.callpushnotificationsample.common.RingtoneUtils;
import com.stringee.callpushnotificationsample.common.Utils;
import com.stringee.listener.StatusListener;

import org.json.JSONException;
import org.json.JSONObject;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "Stringee";

    @Override
    public void onNewToken(String token) {
        if (Common.client != null) {
            Common.client.registerPushToken(token, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        }
    }

    @Override
    public void onMessageReceived(com.google.firebase.messaging.RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            String pushFromStringee = remoteMessage.getData().get("stringeePushNotification");
            if (pushFromStringee != null) {
                String data = remoteMessage.getData().get("data");
                Log.d(TAG, data);

                if (data != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(data);
                        String callId = jsonObject.optString("callId", "");
                        JSONObject fromObject = jsonObject.optJSONObject("from");
                        String from = "";
                        if (fromObject != null) {
                            from = fromObject.optString("alias", "");
                        }

                        if (!Utils.isTextEmpty(callId) && !Utils.isTextEmpty(from)) {
                            String callStatus = jsonObject.optString("callStatus");
                            if (!Utils.isTextEmpty(callStatus)) {
                                if (callStatus.equals("started")) {
                                    NotificationUtils.getInstance(this).createIncomingCallNotification(from);
                                }
                                if (callStatus.equals("ended") || callStatus.equals("answered")) {
                                    NotificationUtils.getInstance(this).cancelNotification(NotificationUtils.INCOMING_CALL_ID);
                                    RingtoneUtils.getInstance(this).stopRinging();
                                }
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
