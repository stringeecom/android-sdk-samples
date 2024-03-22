package com.stringee.apptoappcallsample.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.stringee.apptoappcallsample.common.Constant;
import com.stringee.apptoappcallsample.common.NotificationUtils;
import com.stringee.apptoappcallsample.manager.CallManager;

public class RejectCallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationUtils.getInstance(context).cancelNotification(Constant.INCOMING_CALL_ID);
        CallManager.getInstance(context).endCall(false);
    }
}