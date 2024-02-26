package com.stringee.kotlin_onetoonecallsample.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stringee.kotlin_onetoonecallsample.common.Constant
import com.stringee.kotlin_onetoonecallsample.common.NotificationUtils
import com.stringee.kotlin_onetoonecallsample.manager.CallManager

class RejectCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        NotificationUtils.getInstance(context).cancelNotification(Constant.INCOMING_CALL_ID)
        CallManager.getInstance(context).endCall(false)
    }
}