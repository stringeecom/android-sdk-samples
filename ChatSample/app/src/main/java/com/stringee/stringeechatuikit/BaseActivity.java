package com.stringee.stringeechatuikit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.messaging.FirebaseMessaging;
import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.chat.ui.kit.commons.Notify;
import com.stringee.chat.ui.kit.commons.utils.PermissionsUtils;
import com.stringee.chat.ui.kit.notification.NotificationService;
import com.stringee.common.SocketAddress;
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

import java.util.ArrayList;
import java.util.List;

public class BaseActivity extends AppCompatActivity implements View.OnClickListener {

    private ProgressDialog prLoading;
    public final String accessToken = "PUT_YOUR_TOKEN_HERE";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAndConnectStringee(accessToken);

        PermissionsUtils.getInstance().requestPermissions(this, PermissionsUtils.PERMISSIONS_LOCATION, PermissionsUtils.REQUEST_LOCATION);
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
            // Set host
            // List<SocketAddress> socketAddressList = new ArrayList<>();
            // socketAddressList.add(new SocketAddress("YOUR_IP", YOUR_PORT));
            // Common.client.setHost(socketAddressList);
            // Common.client.setBaseAPIUrl("YOUR_BASE_API_URL");
            // Common.client.setStringeeXBaseUrl("YOUR_STRINGEE_X_BASE_URL");
            Common.client.setConnectionListener(new StringeeConnectionListener() {
                @Override
                public void onConnectionConnected(final StringeeClient client, boolean isReconnecting) {
                    if (!isReconnecting && !PrefUtils.getBoolean(Constant.PREF_REGISTERED_PUSH_TOKEN, false)) {
                        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(deviceToken -> Common.client.registerPushToken(deviceToken, new StatusListener() {
                            @Override
                            public void onSuccess() {
                                PrefUtils.putBoolean(Constant.PREF_REGISTERED_PUSH_TOKEN, true);
                                PrefUtils.putString(Constant.PREF_PUSH_TOKEN, deviceToken);
                            }

                            @Override
                            public void onError(StringeeError error) {
                                Log.e("Stringee", error.getMessage());
                            }
                        }));
                    }
                    LocalBroadcastManager.getInstance(BaseActivity.this).sendBroadcast(new Intent(Notify.CONNECTION_CONNECTED.getValue()));
                }

                @Override
                public void onConnectionDisconnected(StringeeClient client, boolean isReconnecting) {
                    Log.d("Stringee", "onConnectionDisconnected");
                }

                @Override
                public void onIncomingCall(StringeeCall stringeeCall) {
                }

                @Override
                public void onIncomingCall2(StringeeCall2 stringeeCall2) {

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
                    context.runOnUiThread(() -> {
                        String notificationText = message.getText();
                        Message.Type type = message.getType();
                        switch (type) {
                            case TEXT:
                            case LINK:
                                notificationText = message.getText();
                                break;
                            case CREATE_CONVERSATION:
                                notificationText = context.getString(R.string.create_conversation, message.getSenderName());
                                break;
                            case LOCATION:
                                notificationText = context.getString(R.string.location);
                                break;
                            case AUDIO:
                                notificationText = context.getString(R.string.audio);
                                break;
                            case FILE:
                                notificationText = context.getString(R.string.file);
                                break;
                            case PHOTO:
                                notificationText = context.getString(R.string.photo);
                                break;
                            case VIDEO:
                                notificationText = context.getString(R.string.video);
                                break;
                            case CONTACT:
                                notificationText = context.getString(R.string.contact);
                                break;
                            case STICKER:
                                notificationText = context.getString(R.string.sticker);
                                break;
                            case NOTIFICATION:
                                notificationText = Utils.getNotificationText(context, conversation, message.getText());
                                break;
                            case RATING:
                                notificationText = Utils.getRatingText(context, conversation, message);
                                break;
                        }
                        NotificationService.showNotification(context, conversation.getId(), conversation.getName(), finalSenderName, conversation.isGroup(), notificationText);
                    });
                }
            });
        }
    }

    public void changeEvent(StringeeChange change) {
        StringeeObject.Type objectType = change.getObjectType();
        StringeeChange.Type changeType = change.getChangeType();
        Log.d("Stringee", "changeEvent: " + objectType + " - " + changeType);
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