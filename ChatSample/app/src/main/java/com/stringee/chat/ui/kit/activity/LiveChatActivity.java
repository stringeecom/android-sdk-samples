package com.stringee.chat.ui.kit.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.textfield.TextInputEditText;
import com.stringee.chat.ui.kit.adapter.QueueAdapter;
import com.stringee.chat.ui.kit.commons.Notify;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.ChatProfile;
import com.stringee.messaging.Conversation;
import com.stringee.messaging.Message;
import com.stringee.messaging.Queue;
import com.stringee.messaging.User;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.stringeechatuikit.BaseActivity;
import com.stringee.stringeechatuikit.R.id;
import com.stringee.stringeechatuikit.R.layout;
import com.stringee.stringeechatuikit.R.string;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

import java.util.ArrayList;
import java.util.List;

public class LiveChatActivity extends BaseActivity {
    private View vConnect;
    private View vSubmit;
    private TextInputEditText etWidgetKey;
    private TextInputEditText etName;
    private TextInputEditText etPhone;
    private TextInputEditText etEmail;
    private TextInputEditText etText;
    private Spinner spQueue;
    private List<Queue> queues = new ArrayList<>();
    private Queue queue;
    private QueueAdapter queueAdapter;
    private BroadcastReceiver connectReceiver;
    public static String name = "";
    public static String email = "";
    public static String phone = "";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_live_chat);

        getSupportActionBar().setTitle(string.live_chat);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        vConnect = findViewById(id.v_connect);
        vSubmit = findViewById(id.v_submit);
        etWidgetKey = findViewById(id.et_widget_key);
        etName = findViewById(id.et_name);
        etPhone = findViewById(id.et_phone);
        etEmail = findViewById(id.et_email);
        etText = findViewById(id.et_text);
        spQueue = findViewById(id.sp_queue);

        queueAdapter = new QueueAdapter(LiveChatActivity.this, queues);
        spQueue.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                queue = queueAdapter.getItem(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        spQueue.setAdapter(queueAdapter);

//        connectReceiver = new BroadcastReceiver() {
//            @Override
//            public void onReceive(Context context, Intent intent) {
//                getSupportActionBar().setTitle(etName.getText().toString());
//                vConnect.setVisibility(View.GONE);
//                vSubmit.setVisibility(View.VISIBLE);
//
//                if (!Utils.isStringEmpty(etPhone.getText())) {
//                    User user = new User();
//                    user.setPhone(etPhone.getText().toString());
//                    Common.client.updateUser(user, new StatusListener() {
//                        @Override
//                        public void onSuccess() {
//                            phone = etPhone.getText().toString();
//                        }
//
//                        @Override
//                        public void onError(com.stringee.exception.StringeeError stringeeError) {
//                            super.onError(stringeeError);
//                        }
//                    });
//                }
//                getChatInfo();
//                dismissProgress();
//            }
//        };
//        LocalBroadcastManager.getInstance(this).registerReceiver(connectReceiver, new IntentFilter(Notify.CONNECTION_CONNECTED.getValue()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        etName.setText(name);
        etPhone.setText(phone);
        etEmail.setText(email);
    }

    @Override
    protected void onDestroy() {
        Common.client.disconnect();
        initAndConnectStringee(accessToken);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(connectReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (vSubmit.getVisibility() == View.VISIBLE) {
            getSupportActionBar().setTitle(string.live_chat);
            vConnect.setVisibility(View.VISIBLE);
            vSubmit.setVisibility(View.GONE);
            Common.client.disconnect();
        } else {
            name = "";
            email = "";
            phone = "";
            super.onBackPressed();
        }
    }

    public void submit(View view) {
        if (queue == null) {
            Utils.reportMessage(this, "No queue available");
            return;
        }
        if (Utils.isStringEmpty(etText.getText())) {
            Utils.reportMessage(this, "Message cannot empty");
            return;
        }
        showProgress(getString(string.loading));
        Common.client.updateUser(name, email, null, new StatusListener() {
            @Override
            public void onSuccess() {
                Common.client.createLiveChat(queue.getId(), new CallbackListener<Conversation>() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        runOnUiThread(() -> {
                            conversation.sendMessage(Common.client, new Message(etText.getText().toString()), new StatusListener() {
                                @Override
                                public void onSuccess() {

                                }
                            });
                            dismissProgress();
                            Intent intent = new Intent(LiveChatActivity.this, ConversationActivity.class);
                            intent.putExtra("conversation", conversation);
                            startActivity(intent);
                        });
                    }

                    @Override
                    public void onError(StringeeError stringeeError) {
                        super.onError(stringeeError);
                        runOnUiThread(() -> {
                            dismissProgress();
                            Utils.reportMessage(LiveChatActivity.this, stringeeError.getMessage());
                        });
                    }
                });
            }
        });
    }

    private void getChatInfo() {
        Common.client.getChatProfile(etWidgetKey.getText().toString().trim(), new CallbackListener<ChatProfile>() {
            @Override
            public void onSuccess(ChatProfile chatProfile) {
                runOnUiThread(() -> {
                    if (!Utils.isListEmpty(chatProfile.getQueues())) {
                        queues.clear();
                        queues.addAll(chatProfile.getQueues());
                        queueAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    public void connect(View view) {
        if (Utils.isStringEmpty(etWidgetKey.getText())) {
            Utils.reportMessage(this, "widget key cannot empty");
            return;
        }
        if (Utils.isStringEmpty(etName.getText())) {
            Utils.reportMessage(this, "Name cannot empty");
            return;
        }
        showProgress(getString(string.loading));
        name = etName.getText().toString();
        email = !Utils.isStringEmpty(etEmail.getText()) ? etEmail.getText().toString() : "";
        Common.client.getLiveChatToken(etWidgetKey.getText().toString().trim(), name, email, new CallbackListener<String>() {
            @Override
            public void onSuccess(String s) {
                runOnUiThread(() -> {
                    Common.client.disconnect();
                    initAndConnectStringee(s);
                    getSupportActionBar().setTitle(etName.getText().toString());
                    vConnect.setVisibility(View.GONE);
                    vSubmit.setVisibility(View.VISIBLE);

                    if (!Utils.isStringEmpty(etPhone.getText())) {
                        User user = new User();
                        user.setPhone(etPhone.getText().toString());
                        Common.client.updateUser(user, new StatusListener() {
                            @Override
                            public void onSuccess() {
                                phone = etPhone.getText().toString();
                            }

                            @Override
                            public void onError(com.stringee.exception.StringeeError stringeeError) {
                                super.onError(stringeeError);
                            }
                        });
                    }
                    getChatInfo();
                    dismissProgress();
                });
            }

            @Override
            public void onError(StringeeError stringeeError) {
                super.onError(stringeeError);
                runOnUiThread(() -> {
                    dismissProgress();
                    Utils.reportMessage(LiveChatActivity.this, stringeeError.getMessage());
                });
            }
        });
    }
}