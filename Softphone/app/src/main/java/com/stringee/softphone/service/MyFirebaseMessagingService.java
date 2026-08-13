package com.stringee.softphone.service;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.stringee.softphone.activity.MainActivity;
import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.PrefUtils;
import com.stringee.listener.StatusListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by luannguyen on 9/5/2017.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        PrefUtils.getInstance(this).putBoolean(Constant.PREF_TOKEN_REGISTERED, false);
        PrefUtils.getInstance(this).putString(Constant.PREF_FIREBASE_TOKEN, token);
        if (Common.client != null && Common.client.isConnected()) {
            Common.client.registerPushToken(token, new StatusListener() {
                @Override
                public void onSuccess() {
                    PrefUtils.getInstance(MyFirebaseMessagingService.this)
                            .putBoolean(Constant.PREF_TOKEN_REGISTERED, true);
                }
            });
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.e("Stringee", remoteMessage.getData().toString());
            String pushFromStringee = remoteMessage.getData().get("stringeePushNotification");
            if (pushFromStringee != null) {
                String data = remoteMessage.getData().get("data");
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    String callStatus = jsonObject.getString("callStatus");
                    if (callStatus != null && callStatus.equals("started")) {
                        if (Common.client == null || !Common.client.isConnected()) {
                            Intent intent = new Intent(this, MainActivity.class);
                            intent.putExtra(Constant.PARAM_FROM_PUSH, true);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
