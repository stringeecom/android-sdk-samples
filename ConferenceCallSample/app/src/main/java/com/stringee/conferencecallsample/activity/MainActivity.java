package com.stringee.conferencecallsample.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.conferencecallsample.R;
import com.stringee.conferencecallsample.utils.Utils;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeConnectionListener;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static StringeeClient client;
    private String accessToken = "your_access_token"; // replace your access token here.

    private EditText etRoomId;
    private ProgressDialog progressDialog;
    private TextView tvUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUserId = (TextView) findViewById(R.id.tv_userid);

        Button btnMakeRoom = (Button) findViewById(R.id.btn_make_room);
        btnMakeRoom.setOnClickListener(this);
        Button btnJoinRoom = (Button) findViewById(R.id.btn_join_room);
        btnJoinRoom.setOnClickListener(this);

        etRoomId = (EditText) findViewById(R.id.et_room_id);

        progressDialog = ProgressDialog.show(this, "", "Connecting...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        initAndConnectStringee();
    }

    private void initAndConnectStringee() {
        client = new StringeeClient(this);
        client.setConnectionListener(new StringeeConnectionListener() {
            @Override
            public void onConnectionConnected(final StringeeClient client, boolean isReconnecting) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvUserId.setText("Connected as: " + client.getUserId());
                        progressDialog.dismiss();
                        Utils.reportMessage(MainActivity.this, "Stringee session connected.");
                    }
                });
            }

            @Override
            public void onConnectionDisconnected(StringeeClient client, boolean isReconnecting) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.reportMessage(MainActivity.this, "Stringee session disconnected.");
                        LocalBroadcastManager.getInstance(MainActivity.this).sendBroadcast(new Intent("disconnect"));
                    }
                });
            }

            @Override
            public void onIncomingCall(StringeeCall stringeeCall) {

            }

            @Override
            public void onConnectionError(StringeeClient client, StringeeError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Utils.reportMessage(MainActivity.this, "Stringee session fails to connect.");
                    }
                });
            }

            @Override
            public void onRequestNewToken(StringeeClient stringeeClient) {

            }
        });
        client.connect(accessToken);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_make_room:
                if (client.isConnected()) {
                    Intent intent = new Intent(this, ConferenceCallActivity.class);
                    intent.putExtra("action", "make");
                    startActivity(intent);
                } else {
                    Utils.reportMessage(this, "Stringee session not connected");
                }
                break;
            case R.id.btn_join_room:
                if (client.isConnected()) {
                    if (etRoomId.getText().toString().length() > 0) {
                        Intent intent = new Intent(this, ConferenceCallActivity.class);
                        intent.putExtra("action", "join");
                        intent.putExtra("room_id", Integer.parseInt(etRoomId.getText().toString()));
                        startActivity(intent);
                    }
                } else {
                    Utils.reportMessage(this, "Stringee session not connected");
                }
                break;
        }
    }
}
