package com.stringee.kotlin_onetoonecallsample.stringee.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallConstants
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallUtils
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall
import com.stringee.kotlin_onetoonecallsample.stringee.manager.StringeeCallManager
import org.json.JSONException
import org.json.JSONObject

/** Parses Stringee call pushes and delegates ownership decisions to the call manager. */
class StringeeFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        StringeeCallManager.Companion.getInstance(this).registerPushToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val remoteData = remoteMessage.getData()
        if (remoteData.isEmpty() || remoteData.get("stringeePushNotification") == null) {
            return
        }
        val data = remoteData.get("data")
        if (CallUtils.isEmpty(data)) {
            return
        }
        try {
            val `object` = JSONObject(data)
            val status = `object`.optString("callStatus", "")
            val callId = `object`.optString("callId", "")
            if ("ended" == status || "busy" == status
                || "agentEnded" == status
            ) {
                StringeeCallManager.Companion.getInstance(this).handleTerminalPush(callId)
                return
            }
            val manager: StringeeCallManager = StringeeCallManager.Companion.getInstance(this)
            if (("started" != status) || CallUtils.isEmpty(callId)
                || manager.hasActiveCall()
                || !ActivePushCall.claim(callId)
            ) {
                return
            }
            var from = ""
            var alias = ""
            val fromValue = `object`.opt("from")
            if (fromValue is JSONObject) {
                val fromObject = fromValue
                from = fromObject.optString("number", "")
                alias = fromObject.optString("alias", "")
            } else {
                from = `object`.optString("from", "")
            }
            val video = `object`.optBoolean(
                "isVideoCall",
                `object`.optBoolean("videoCall", false)
            )
            IncomingCallService.Companion.showFromPush(this, from, alias, video, callId)
        } catch (exception: JSONException) {
            Log.e(CallConstants.TAG, "Invalid Stringee call push", exception)
        }
    }
}
