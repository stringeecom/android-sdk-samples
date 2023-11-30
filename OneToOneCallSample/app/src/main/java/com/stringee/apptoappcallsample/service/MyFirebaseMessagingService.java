package com.stringee.apptoappcallsample.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.stringee.apptoappcallsample.common.AudioManagerUtils;
import com.stringee.apptoappcallsample.common.Constant;
import com.stringee.apptoappcallsample.common.NotificationUtils;
import com.stringee.apptoappcallsample.manager.ClientManager;
import com.stringee.listener.StatusListener;

import org.json.JSONException;
import org.json.JSONObject;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(@NonNull String token) {
        if (ClientManager.getInstance(this).getStringeeClient() == null) {
            ClientManager.getInstance(this).connect();
        }
        ClientManager.getInstance(this).getStringeeClient().registerPushToken(token, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(Constant.TAG, remoteMessage.getData().toString());
        if (remoteMessage.getData().size() > 0) {
            String pushFromStringee = remoteMessage.getData().get("stringeePushNotification");
            if (pushFromStringee != null) {
                String data = remoteMessage.getData().get("data");
                if (data != null) {
                    try {
                        JSONObject jsonObject = new JSONObject(data);
                        String callStatus = jsonObject.optString("callStatus");
                        if (callStatus.equals("started")) {
//                            if (ClientManager.getInstance(this).getStringeeClient() == null) {
                                ClientManager.getInstance(this).connect();
//                            }
                        }
                        if (callStatus.equals("ended") || callStatus.equals("answered") || callStatus.equals("agentEnded")) {
                            NotificationUtils.getInstance(this).cancelNotification(Constant.INCOMING_CALL_ID);
                            AudioManagerUtils.getInstance(this).stopRinging();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
