package com.stringee.kotlin_onetoonecallsample.manager

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.stringee.StringeeClient
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall2
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.common.Constant
import com.stringee.kotlin_onetoonecallsample.common.NotificationUtils
import com.stringee.kotlin_onetoonecallsample.common.Utils
import com.stringee.kotlin_onetoonecallsample.listener.OnConnectionListener
import com.stringee.listener.StatusListener
import com.stringee.listener.StringeeConnectionListener
import org.json.JSONObject


class ClientManager private constructor(private val applicationContext: Context) {
    var stringeeClient: StringeeClient? = null
        private set
    private var listener: OnConnectionListener? = null
    private val token =
        "eyJjdHkiOiJzdHJpbmdlZS1hcGk7dj0xIiwidHlwIjoiSldUIiwiYWxnIjoiSFMyNTYifQ.eyJqdGkiOiJTS3JTaWZRWlVJa3ZPY2Q0RHdZT2c1Y2lpQUJma01kTTJOLTE3MDk3OTMwMzMwMzYiLCJpc3MiOiJTS3JTaWZRWlVJa3ZPY2Q0RHdZT2c1Y2lpQUJma01kTTJOIiwidXNlcklkIjoidXNlcjIiLCJleHAiOjE3NDEzMjkwMzJ9.FbWVRai78WuOsJiruaNBlwdQyTmr-94jiHx_Bah_fNM"
    var isInCall = false
    var isPermissionGranted = true

    fun addOnConnectionListener(listener: OnConnectionListener?) {
        this.listener = listener
    }

    fun connect() {
        if (stringeeClient == null) {
            stringeeClient = StringeeClient(applicationContext)
            //            Set host
//            List<SocketAddress> socketAddressList = new ArrayList<>();
//            socketAddressList.add(new SocketAddress("YOUR_IP", YOUR_PORT));
//            stringeeClient.setHost(socketAddressList);
            stringeeClient?.setConnectionListener(object : StringeeConnectionListener {
                override fun onConnectionConnected(
                    stringeeClient: StringeeClient,
                    isReconnecting: Boolean
                ) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onConnectionConnected")
                        if (listener != null) {
                            listener!!.onConnect("Connected as: " + stringeeClient.userId)
                        }
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task: Task<String?> ->
                            if (!task.isSuccessful) {
                                Log.d(
                                    Constant.TAG,
                                    "getInstanceId failed",
                                    task.exception
                                )
                                return@addOnCompleteListener
                            }

                            // Get new token
                            val refreshedToken = task.result
                            stringeeClient.registerPushToken(
                                refreshedToken,
                                object : StatusListener() {
                                    override fun onSuccess() {
                                        Log.d(
                                            Constant.TAG,
                                            "registerPushToken success"
                                        )
                                    }

                                    override fun onError(error: StringeeError) {
                                        Log.d(
                                            Constant.TAG,
                                            "registerPushToken error: " + error.message
                                        )
                                    }
                                })
                        }
                    }
                }

                override fun onConnectionDisconnected(
                    stringeeClient: StringeeClient,
                    isReconnecting: Boolean
                ) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onConnectionDisconnected")
                        if (listener != null) {
                            listener!!.onConnect("Disconnected")
                        }
                    }
                }

                override fun onIncomingCall(stringeeCall: StringeeCall) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onIncomingCall: callId - " + stringeeCall.callId
                        )
                        if (isInCall) {
                            stringeeCall.reject(object : StatusListener() {
                                override fun onSuccess() {}
                            })
                        } else {
                            CallManager.getInstance(applicationContext)
                                .initializedIncomingCall(stringeeCall)
                            CallManager.getInstance(applicationContext).initAnswer()
                            NotificationUtils.getInstance(applicationContext)
                                .showIncomingCallNotification(
                                    stringeeCall.from,
                                    true,
                                    stringeeCall.isVideoCall
                                )
                        }
                    }
                }

                override fun onIncomingCall2(stringeeCall2: StringeeCall2) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onIncomingCall2: callId - " + stringeeCall2.callId
                        )
                        if (isInCall) {
                            stringeeCall2.reject(object : StatusListener() {
                                override fun onSuccess() {}
                            })
                        } else {
                            CallManager.getInstance(applicationContext)
                                .initializedIncomingCall(stringeeCall2)
                            CallManager.getInstance(applicationContext).initAnswer()
                            NotificationUtils.getInstance(applicationContext)
                                .showIncomingCallNotification(
                                    stringeeCall2.from,
                                    false,
                                    stringeeCall2.isVideoCall
                                )
                        }
                    }
                }

                override fun onConnectionError(
                    stringeeClient: StringeeClient,
                    stringeeError: StringeeError
                ) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onConnectionError: " + stringeeError.message
                        )
                        if (listener != null) {
                            listener!!.onConnect(stringeeError.message)
                        }
                    }
                }

                override fun onRequestNewToken(stringeeClient: StringeeClient) {
                    // Get new token here and connect to Stringee server
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onRequestNewToken")
                        if (listener != null) {
                            listener!!.onConnect("Request new token")
                        }
                    }
                }

                override fun onCustomMessage(from: String, msg: JSONObject) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onCustomMessage: from - $from - msg - $msg"
                        )
                    }
                }

                override fun onTopicMessage(from: String, msg: JSONObject) {}
            })
        }
        if (!stringeeClient?.isConnected!!) {
            stringeeClient!!.connect(token)
        }
    }

    companion object {
        @Volatile
        private var instance: ClientManager? = null
        fun getInstance(context: Context): ClientManager {
            return instance ?: synchronized(this) {
                instance ?: ClientManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

