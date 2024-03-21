package com.stringee.apptoappcallsample.manager;

import android.content.Context;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessaging;
import com.stringee.StringeeClient;
import com.stringee.apptoappcallsample.common.Constant;
import com.stringee.apptoappcallsample.common.NotificationUtils;
import com.stringee.apptoappcallsample.common.Utils;
import com.stringee.apptoappcallsample.listener.OnConnectionListener;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.common.SocketAddress;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.listener.StringeeConnectionListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ClientManager {
    private static volatile ClientManager instance;
    private final Context context;

    public ClientManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static ClientManager getInstance(Context context) {
        if (instance == null) {
            synchronized (ClientManager.class) {
                if (instance == null) {
                    instance = new ClientManager(context);
                }
            }
        }
        return instance;
    }

    private StringeeClient stringeeClient;
    private OnConnectionListener listener;
    private static final String TOKEN = "eyJjdHkiOiJzdHJpbmdlZS1hcGk7dj0xIiwidHlwIjoiSldUIiwiYWxnIjoiSFMyNTYifQ.eyJqdGkiOiJTS0UxUmRVdFVhWXhOYVFRNFdyMTVxRjF6VUp1UWRBYVZULTE3MTA5ODgyNjU2MjUiLCJpc3MiOiJTS0UxUmRVdFVhWXhOYVFRNFdyMTVxRjF6VUp1UWRBYVZUIiwidXNlcklkIjoidXNlcjMiLCJleHAiOjE3NDI1MjQyNjV9.Z--CKOqKL37dw4_S3ybDwkfdNVKES1G_XJqu-ArGIbE";
    public boolean isInCall = false;
    public boolean isPermissionGranted = true;

    public StringeeClient getStringeeClient() {
        return stringeeClient;
    }

    public void addOnConnectionListener(OnConnectionListener listener) {
        this.listener = listener;
    }

    public void connect() {
        if (stringeeClient == null) {
            stringeeClient = new StringeeClient(context);
//            Set host
//            List<SocketAddress> socketAddressList = new ArrayList<>();
//            socketAddressList.add(new SocketAddress("YOUR_IP", YOUR_PORT));
//            stringeeClient.setHost(socketAddressList);

            stringeeClient.setConnectionListener(new StringeeConnectionListener() {
                @Override
                public void onConnectionConnected(final StringeeClient stringeeClient, boolean isReconnecting) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onConnectionConnected");
                        if (listener != null) {
                            listener.onConnect("Connected as: " + stringeeClient.getUserId());
                        }
                        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
                            if (!task.isSuccessful()) {
                                Log.d(Constant.TAG, "getInstanceId failed", task.getException());
                                return;
                            }

                            // Get new token
                            String refreshedToken = task.getResult();
                            stringeeClient.registerPushToken(refreshedToken, new StatusListener() {
                                @Override
                                public void onSuccess() {
                                    Log.d(Constant.TAG, "registerPushToken success");
                                }

                                @Override
                                public void onError(StringeeError error) {
                                    Log.d(Constant.TAG, "registerPushToken error: " + error.getMessage());
                                }
                            });
                        });
                    });
                }

                @Override
                public void onConnectionDisconnected(StringeeClient stringeeClient, boolean isReconnecting) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onConnectionDisconnected");
                        if (listener != null) {
                            listener.onConnect("Disconnected");
                        }
                    });
                }

                @Override
                public void onIncomingCall(final StringeeCall stringeeCall) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onIncomingCall: callId - " + stringeeCall.getCallId());
                        if (isInCall) {
                            stringeeCall.reject(new StatusListener() {
                                @Override
                                public void onSuccess() {

                                }
                            });
                        } else {
                            CallManager.getInstance(context).initializedIncomingCall(stringeeCall);
                            CallManager.getInstance(context).initAnswer();
                            NotificationUtils.getInstance(context).showIncomingCallNotification(stringeeCall.getFrom(), true, stringeeCall.isVideoCall());
                        }
                    });
                }

                @Override
                public void onIncomingCall2(StringeeCall2 stringeeCall2) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onIncomingCall2: callId - " + stringeeCall2.getCallId());
                        if (isInCall) {
                            stringeeCall2.reject(new StatusListener() {
                                @Override
                                public void onSuccess() {

                                }
                            });
                        } else {
                            CallManager.getInstance(context).initializedIncomingCall(stringeeCall2);
                            CallManager.getInstance(context).initAnswer();
                            NotificationUtils.getInstance(context).showIncomingCallNotification(stringeeCall2.getFrom(), false, stringeeCall2.isVideoCall());
                        }
                    });
                }

                @Override
                public void onConnectionError(StringeeClient stringeeClient, final StringeeError stringeeError) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onConnectionError: " + stringeeError.getMessage());
                        if (listener != null) {
                            listener.onConnect(stringeeError.getMessage());
                        }
                    });
                }

                @Override
                public void onRequestNewToken(StringeeClient stringeeClient) {
                    // Get new token here and connect to Stringe server
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onRequestNewToken");
                        if (listener != null) {
                            listener.onConnect("Request new token");
                        }
                    });
                }

                @Override
                public void onCustomMessage(String from, JSONObject msg) {
                    Utils.runOnUiThread(() -> Log.d(Constant.TAG, "onCustomMessage: from - " + from + " - msg - " + msg));
                }

                @Override
                public void onTopicMessage(String from, JSONObject msg) {

                }
            });
        }
        if (!stringeeClient.isConnected()) {
            stringeeClient.connect(TOKEN);
        }
    }
}
