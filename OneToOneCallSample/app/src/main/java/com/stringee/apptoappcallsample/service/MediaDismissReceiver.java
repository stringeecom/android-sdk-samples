package com.stringee.apptoappcallsample.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.stringee.apptoappcallsample.manager.CallManager;

public class MediaDismissReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        CallManager.getInstance(context).stopSharing();
    }
}
