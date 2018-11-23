package com.stringee.apptoappcallsample;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.stringee.StringeeClient;
import com.stringee.listener.StatusListener;

/**
 * Created by luannguyen on 2/7/2018.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        // Register the token to Stringee Server
        if (MainActivity.client != null && MainActivity.client.isConnected()) {
            MainActivity.client.registerPushToken(refreshedToken, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        } else {
            // Handle your code here
        }
    }
}
