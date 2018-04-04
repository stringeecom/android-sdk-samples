package com.stringee.softphone.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.PrefUtils;

/**
 * Created by luannguyen on 8/29/2017.
 */

public class ConnectionChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetInfo != null && activeNetInfo.isConnected();
        if (isConnected) {
            if (!Common.alreadyConnected) {
                if (Common.client != null) {
                    Common.client.connect(PrefUtils.getInstance(context).getString(Constant.PREF_ACCESS_TOKEN, ""));
                }
            }
        }
    }
}
