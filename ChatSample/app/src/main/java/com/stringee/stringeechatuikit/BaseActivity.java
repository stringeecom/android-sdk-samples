package com.stringee.stringeechatuikit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.chat.ui.kit.commons.Notify;
import com.stringee.chat.ui.kit.notification.NotificationService;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.listener.StringeeConnectionListener;
import com.stringee.messaging.ChatRequest;
import com.stringee.messaging.ChatRequest.State;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.messaging.StringeeChange;
import com.stringee.messaging.StringeeObject;
import com.stringee.messaging.User;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.messaging.listeners.ChangeEventListener;
import com.stringee.messaging.listeners.LiveChatEventListener;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Constant;
import com.stringee.stringeechatuikit.common.PrefUtils;
import com.stringee.stringeechatuikit.common.Utils;

import org.json.JSONObject;

public class BaseActivity extends AppCompatActivity implements View.OnClickListener {

    private ProgressDialog prLoading;
    public final String accessToken = "PUT_YOUR_TOKEN_HERE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAndConnectStringee(accessToken);
    }

    public void showProgress(String text) {
        prLoading = ProgressDialog.show(this, "", text);
        prLoading.setCancelable(true);
        prLoading.show();
    }

    public void dismissProgress() {
        if (prLoading != null && prLoading.isShowing()) {
            prLoading.dismiss();
        }
    }

    @Override
    public void onClick(View view) {

    }

    public void initAndConnectStringee(String token) {
        if (Common.client == null) {
            Common.client = new StringeeClient(this);
            Common.client.setConnectionListener(new StringeeConnectionListener() {
                @Override
                public void onConnectionConnected(final StringeeClient client, boolean isReconnecting) {
                    if (!isReconnecting && !PrefUtils.getBoolean(Constant.PREF_REGISTERED_PUSH_TOKEN, false)) {
                        FirebaseInstanceId.getInstance().getInstanceId()
                                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                                    @Override
                                    public void onComplete(Task<InstanceIdResult> task) {
                                        if (!task.isSuccessful()) {
                                            Log.d("Stringee", "getInstanceId failed", task.getException());
                                            return;
                                        }

                                        // Get new Instance ID token
                                        final String refreshedToken = task.getResult().getToken();
                                        Common.client.registerPushToken(refreshedToken, new StatusListener() {
                                            @Override
                                            public void onSuccess() {
                                                PrefUtils.putBoolean(Constant.PREF_REGISTERED_PUSH_TOKEN, true);
                                                PrefUtils.putString(Constant.PREF_PUSH_TOKEN, refreshedToken);
                                            }

                                            @Override
                                            public void onError(StringeeError error) {
                                                Log.e("Stringee", error.getMessage());
                                            }
                                        });
                                    }
                                });
                    }
                    LocalBroadcastManager.getInstance(BaseActivity.this).sendBroadcast(new Intent(Notify.CONNECTION_CONNECTED.getValue()));
                }

                @Override
                public void onConnectionDisconnected(StringeeClient client, boolean isReconnecting) {
                    Log.d("Stringee", "onConnectionDisconnected");
                }

                @Override
                public void onIncomingCall(StringeeCall stringeeCall) {
                    String callId = stringeeCall.getCallId();
                    Common.callsMap.put(callId, stringeeCall);
                    Intent intent = new Intent(BaseActivity.this, IncomingCallActivity.class);
                    intent.putExtra("call_id", callId);
                    startActivity(intent);
                }

                @Override
                public void onIncomingCall2(com.stringee.call.StringeeCall2 stringeeCall2) {

                }

                @Override
                public void onConnectionError(StringeeClient client, StringeeError error) {
                    Log.d("Stringee", "onConnectionError: " + error.getMessage());
                }

                @Override
                public void onRequestNewToken(StringeeClient client) {
                    Log.d("Stringee", "onRequestNewToken");
                }

                @Override
                public void onCustomMessage(String from, JSONObject msg) {

                }

                @Override
                public void onTopicMessage(String s, JSONObject jsonObject) {

                }
            });

            Common.client.setChangeEventListener(new ChangeEventListener() {
                @Override
                public void onChangeEvent(StringeeChange change) {
                    changeEvent(change);
                }
            });
            Common.isChangeListenerSet = true;

            Common.client.setLiveChatEventListener(new LiveChatEventListener() {
                @Override
                public void onReceiveChatRequest(ChatRequest chatRequest) {

                }

                @Override
                public void onReceiveTransferChatRequest(ChatRequest chatRequest) {

                }

                @Override
                public void onHandleOnAnotherDevice(ChatRequest chatRequest, State state) {

                }

                @Override
                public void onTimeoutAnswerChat(ChatRequest chatRequest) {

                }

                @Override
                public void onTimeoutInQueue(Conversation conversation) {
                    runOnUiThread(() -> {
                        Intent intent = new Intent(Notify.CONVERSATION_DELETED.getValue());
                        intent.putExtra("conversation", conversation);
                        LocalBroadcastManager.getInstance(BaseActivity.this).sendBroadcast(intent);
                    });
                }

                @Override
                public void onConversationEnded(Conversation conversation, User user) {
                    runOnUiThread(() -> {
                        Intent intent = new Intent(Notify.CONVERSATION_DELETED.getValue());
                        intent.putExtra("conversation", conversation);
                        LocalBroadcastManager.getInstance(BaseActivity.this).sendBroadcast(intent);
                    });
                }
            });
        }
        if (!Utils.isStringEmpty(token)) {
            Common.client.connect(token);
        }
    }

    public static void notifyMessage(final Message message, final BaseActivity context) {
        if (!(Common.isChatting && Common.currentConvId != null && Common.currentConvId.equals(message.getConversationId())) && message.getMsgType() == Message.MsgType.RECEIVE && !message.getSenderId().equals(Common.client.getUserId())) {
            String from = message.getSenderId();
            String senderName = from;
            User user = Common.client.getUser(from);
            if (user != null && user.getName() != null && user.getName().length() > 0) {
                senderName = user.getName();
            }
            final String finalSenderName = senderName;
            Common.client.getConversation(message.getConversationId(), new CallbackListener<Conversation>() {
                @Override
                public void onSuccess(final Conversation conversation) {
                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationService.showNotification(context, conversation.getId(), conversation.getName(), finalSenderName, conversation.isGroup(), message.getText());
                        }
                    });
                }
            });
        }
    }

    public void changeEvent(StringeeChange change) {
        StringeeObject.Type objectType = change.getObjectType();
        StringeeChange.Type changeType = change.getChangeType();
        if (objectType == StringeeObject.Type.CONVERSATION) {
            Conversation conversation = (Conversation) change.getObject();
            switch (changeType) {
                case INSERT:
                    Intent intent = new Intent(Notify.CONVERSATION_ADDED.getValue());
                    intent.putExtra("conversation", conversation);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    break;
                case UPDATE:
                    Intent intent1 = new Intent(Notify.CONVERSATION_UPDATED.getValue());
                    intent1.putExtra("conversation", conversation);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
                    break;
                case DELETE:
                    break;
            }
        } else if (objectType == StringeeObject.Type.MESSAGE) {
            Message message = (Message) change.getObject();
            switch (changeType) {
                case INSERT:
                    notifyMessage(message, this);
                    Intent intent = new Intent(Notify.MESSAGE_ADDED.getValue());
                    intent.putExtra("message", message);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    break;
                case UPDATE:
                    Intent intent1 = new Intent(Notify.MESSAGE_UPDATED.getValue());
                    intent1.putExtra("message", message);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);
                    break;
                case DELETE:
                    break;
            }
        }
    }
}