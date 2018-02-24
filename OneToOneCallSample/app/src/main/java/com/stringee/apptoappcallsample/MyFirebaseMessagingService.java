package com.stringee.apptoappcallsample;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by luannguyen on 2/7/2018.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private final String TAG = "Stringee";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Connect Stringee Server here then the client receives an incoming call.
        // In this sample, we only start MainActivity and connect Stringee Server in MainActivity.
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            String pushFromStringee = remoteMessage.getData().get("stringeePushNotification");
            if (pushFromStringee != null) {
                if (MainActivity.client == null) { // Check whether the app is not alive
                    startActivity(new Intent(this, MainActivity.class));
                }
            }
        }
    }
}
