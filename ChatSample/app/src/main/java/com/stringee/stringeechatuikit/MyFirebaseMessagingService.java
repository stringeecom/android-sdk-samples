package com.stringee.stringeechatuikit;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.stringee.chat.ui.kit.notification.NotificationService;
import com.stringee.listener.StatusListener;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Constant;
import com.stringee.stringeechatuikit.common.PrefUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Created by luannguyen on 2/6/2018.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(final String token) {
        if (Common.client != null) {
            Common.client.registerPushToken(token, new StatusListener() {
                @Override
                public void onSuccess() {
                    PrefUtils.putString(Constant.PREF_PUSH_TOKEN, token);
                }
            });
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (Common.isChangeListenerSet) {
            return;
        }
        Map<String, String> remoteData = remoteMessage.getData();
        if (remoteData != null) {
            String flag = remoteData.get("stringeePushNotification");
            if (flag != null) { // Check whether the push notification is from Stringee
                String type = remoteData.get("type");
                if (type != null && type.equals("CHAT_EVENT")) {
                    String data = remoteData.get("data");
                    try {
                        JSONObject dataObject = new JSONObject(data);
                        String convId = dataObject.getString("convId");
                        String convName = dataObject.getString("convName");
                        String senderName = dataObject.optString("displayName");
                        if (senderName == null || senderName.length() == 0) {
                            senderName = dataObject.getString("from");
                        }
                        boolean isGroup = dataObject.getBoolean("isGroup");
                        int msgType = dataObject.getInt("type");
                        JSONObject messageObject = dataObject.getJSONObject("message");
                        String text;
                        if (msgType == 7) {
                            text = messageObject.getString("creator") + " create the group chat";
                        } else {
                            text = messageObject.optString("content");
                        }

                        NotificationService.showNotification(getApplicationContext(), convId, convName, senderName, isGroup, text);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
