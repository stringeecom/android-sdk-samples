package com.stringee.kotlin_onetoonecallsample.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.stringee.kotlin_onetoonecallsample.common.AudioManagerUtils
import com.stringee.kotlin_onetoonecallsample.common.Constant
import com.stringee.kotlin_onetoonecallsample.common.NotificationUtils
import com.stringee.kotlin_onetoonecallsample.manager.ClientManager
import com.stringee.listener.StatusListener
import org.json.JSONException
import org.json.JSONObject


class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        if (ClientManager.getInstance(this).stringeeClient == null) {
            ClientManager.getInstance(this).connect()
        }
        ClientManager.getInstance(this).stringeeClient?.registerPushToken(
            token,
            object : StatusListener() {
                override fun onSuccess() {}
            })
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(Constant.TAG, remoteMessage.data.toString())
        if (remoteMessage.data.isNotEmpty()) {
            val pushFromStringee = remoteMessage.data["stringeePushNotification"]
            if (pushFromStringee != null) {
                val data = remoteMessage.data["data"]
                if (data != null) {
                    try {
                        val jsonObject = JSONObject(data)
                        val callStatus = jsonObject.optString("callStatus")
                        if (callStatus == "started") {
                            ClientManager.getInstance(this).connect()
                        }
                        if (callStatus == "ended" || callStatus == "answered" || callStatus == "agentEnded") {
                            NotificationUtils.getInstance(this)
                                .cancelNotification(Constant.INCOMING_CALL_ID)
                            AudioManagerUtils.getInstance(this).stopRinging()
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}

