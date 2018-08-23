package com.stringee.apptoappcallsample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.stringee.StringeeClient;
import com.stringee.apptoappcallsample.utils.Utils;
import com.stringee.call.StringeeCall;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeConnectionListener;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static StringeeClient client;
    private String to;
    public static Map<String, StringeeCall> callsMap = new HashMap<>();
    private String accessToken = "your_access_token"; // replace your access token here.

    private EditText etTo;
    private TextView tvUserId;
    private ProgressDialog progressDialog;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private final String PREF_NAME = "com.stringee.onetoonecallsample";
    private final String IS_TOKEN_REGISTERED = "is_token_registered";
    private final String TOKEN = "token";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUserId = (TextView) findViewById(R.id.tv_userid);

        Button btnVoiceCall = (Button) findViewById(R.id.btn_voice_call);
        btnVoiceCall.setOnClickListener(this);
        Button btnVideoCall = (Button) findViewById(R.id.btn_video_call);
        btnVideoCall.setOnClickListener(this);
        etTo = (EditText) findViewById(R.id.et_to);

        Button btnUnregister = (Button) findViewById(R.id.btn_unregister);
        btnUnregister.setOnClickListener(this);

        progressDialog = ProgressDialog.show(this, "", "Connecting...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        editor = sharedPreferences.edit();

        initAndConnectStringee();
    }

    public void initAndConnectStringee() {
        client = new StringeeClient(this);
        client.setConnectionListener(new StringeeConnectionListener() {
            @Override
            public void onConnectionConnected(final StringeeClient stringeeClient, boolean isReconnecting) {
                boolean isTokenRegistered = sharedPreferences.getBoolean(IS_TOKEN_REGISTERED, false);
                if (!isTokenRegistered) {
                    final String token = FirebaseInstanceId.getInstance().getToken();
                    client.registerPushToken(token, new StringeeClient.RegisterPushTokenListener() {
                        @Override
                        public void onPushTokenRegistered(boolean success, String desc) {
                            Log.d("Stringee", "Register push token: " + desc);
                            if (success) {
                                editor.putBoolean(IS_TOKEN_REGISTERED, true);
                                editor.putString(TOKEN, token);
                                editor.commit();
                            }
                        }

                        @Override
                        public void onPushTokenUnRegistered(boolean success, String desc) {
                            Log.d("Stringee", "Unregister push token: " + desc);
                        }
                    });
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        tvUserId.setText("Connected as: " + stringeeClient.getUserId());
                        Utils.reportMessage(MainActivity.this, "StringeeClient is connected.");
                    }
                });
            }

            @Override
            public void onConnectionDisconnected(StringeeClient stringeeClient, boolean isReconnecting) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Utils.reportMessage(MainActivity.this, "StringeeClient disconnected.");
                    }
                });
            }

            @Override
            public void onIncomingCall(final StringeeCall stringeeCall) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        callsMap.put(stringeeCall.getCallId(), stringeeCall);
                        Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
                        intent.putExtra("call_id", stringeeCall.getCallId());
                        startActivity(intent);
                    }
                });
            }

            @Override
            public void onConnectionError(StringeeClient stringeeClient, final StringeeError stringeeError) {
                Log.d("Stringee", "StringeeClient fails to connect: " + stringeeError.getMessage());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Utils.reportMessage(MainActivity.this, "StringeeClient fails to connect: " + stringeeError.getMessage());
                    }
                });
            }

            @Override
            public void onRequestNewToken(StringeeClient stringeeClient) {
                // Get new token here and connect to Stringe server
            }

            @Override
            public void onCustomMessage(String s, JSONObject jsonObject) {

            }
        });
        client.connect(accessToken);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_voice_call:
                to = etTo.getText().toString();
                if (to.trim().length() > 0) {
                    if (client.isConnected()) {
                        Intent intent = new Intent(this, OutgoingCallActivity.class);
                        intent.putExtra("from", client.getUserId());
                        intent.putExtra("to", to);
                        intent.putExtra("is_video_call", false);
                        startActivity(intent);
                    } else {
                        Utils.reportMessage(this, "Stringee session not connected");
                    }
                }
                break;
            case R.id.btn_video_call:
                to = etTo.getText().toString();
                if (to.trim().length() > 0) {
                    if (client.isConnected()) {
                        Intent intent = new Intent(this, OutgoingCallActivity.class);
                        intent.putExtra("from", client.getUserId());
                        intent.putExtra("to", to);
                        intent.putExtra("is_video_call", true);
                        startActivity(intent);
                    } else {
                        Utils.reportMessage(this, "Stringee session not connected");
                    }
                }
                break;
            case R.id.btn_unregister:
                client.unregisterPushToken(sharedPreferences.getString(TOKEN, ""), new StringeeClient.RegisterPushTokenListener() {
                    @Override
                    public void onPushTokenRegistered(boolean success, String desc) {

                    }

                    @Override
                    public void onPushTokenUnRegistered(boolean success, String desc) {
                        Log.d("Stringee", "Unregister push token: " + desc);
                        editor.remove(IS_TOKEN_REGISTERED);
                        editor.remove(TOKEN);
                        editor.commit();
                    }
                });
                break;
        }
    }
}
