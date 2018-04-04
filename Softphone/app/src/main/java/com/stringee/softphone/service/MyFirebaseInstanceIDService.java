package com.stringee.softphone.service;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.stringee.StringeeClient;
import com.stringee.softphone.common.Common;

/**
 * Created by luannguyen on 9/5/2017.
 */

public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        if (Common.client != null) {
            Common.client.registerPushToken(refreshedToken, new StringeeClient.RegisterPushTokenListener() {
                @Override
                public void onPushTokenRegistered(boolean success, String desc) {

                }

                @Override
                public void onPushTokenUnRegistered(boolean success, String desc) {

                }
            });
        }
    }
}
