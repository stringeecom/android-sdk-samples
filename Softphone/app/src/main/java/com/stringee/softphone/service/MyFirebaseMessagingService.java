package com.stringee.softphone.service;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.stringee.softphone.activity.MainActivity;
import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by luannguyen on 9/5/2017.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            Log.e("Stringee", remoteMessage.toString());
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
