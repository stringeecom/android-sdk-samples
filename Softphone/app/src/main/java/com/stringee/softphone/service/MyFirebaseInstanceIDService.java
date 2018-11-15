package com.stringee.softphone.service;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.stringee.listener.StatusListener;
import com.stringee.softphone.common.Common;

/**
 * Created by luannguyen on 9/5/2017.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        if (Common.client != null) {
            Common.client.registerPushToken(refreshedToken, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        }
    }
}
