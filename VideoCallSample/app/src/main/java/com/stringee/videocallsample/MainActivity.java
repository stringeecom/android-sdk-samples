package com.stringee.videocallsample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleObserver;

import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeConnectionListener;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements LifecycleObserver {
    private String to;
    //put your token here
    private String token = "YOUR_ACCESS_TOKEN";

    private EditText etTo;
    private TextView tvUserId;
    private ProgressDialog progressDialog;
    private ActivityResultLauncher<Intent> launcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUserId = findViewById(R.id.tv_userid);
        etTo = findViewById(R.id.et_to);

        Button btnMakeCall = findViewById(R.id.btn_make_call);
        btnMakeCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                to = etTo.getText().toString().trim();
                if (to.length() > 0) {
                    Intent intent = new Intent(MainActivity.this, OutgoingCallActivity.class);
                    intent.putExtra("from", Common.client.getUserId());
                    intent.putExtra("to", to);
                    launcher.launch(intent);
                }
            }
        });

        progressDialog = ProgressDialog.show(this, "", "Connecting...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        // register data call back
        launcher = registerForActivityResult(new StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AppCompatActivity.RESULT_CANCELED)
                        if (result.getData() != null) {
                            if (result.getData().getAction() != null && result.getData().getAction().equals("open_app_setting")) {
                                Builder builder = new Builder(this);
                                builder.setTitle(R.string.app_name);
                                builder.setMessage("Permissions must be granted for the call");
                                builder.setPositiveButton("Ok", (dialogInterface, id) -> {
                                    dialogInterface.cancel();
                                });
                                builder.setNegativeButton("Settings", (dialogInterface, id) -> {
                                    dialogInterface.cancel();
                                    // open app setting
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                                    intent.setData(uri);
                                    startActivity(intent);
                                });
                                builder.create().show();
                            }
                        }
                });

        initAndConnectStringee();
    }

    public void initAndConnectStringee() {
        if (Common.client == null) {
            Common.client = new StringeeClient(this);
            Common.client.setConnectionListener(new StringeeConnectionListener() {
                @Override
                public void onConnectionConnected(final StringeeClient stringeeClient, boolean isReconnecting) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        tvUserId.setText("Connected as: " + stringeeClient.getUserId());
                    });
                }

                @Override
                public void onConnectionDisconnected(StringeeClient stringeeClient, boolean isReconnecting) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        tvUserId.setText("Disconnected");
                    });
                }

                @Override
                public void onIncomingCall(final StringeeCall stringeeCall) {
                }

                @Override
                public void onIncomingCall2(StringeeCall2 stringeeCall2) {
                    runOnUiThread(() -> {
                        if (Common.isInCall) {
                            stringeeCall2.reject();
                        } else {
                            Common.callMap.put(stringeeCall2.getCallId(), stringeeCall2);
                            Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
                            intent.putExtra("call_id", stringeeCall2.getCallId());
                            startActivity(intent);
                        }
                    });
                }

                @Override
                public void onConnectionError(StringeeClient stringeeClient, final StringeeError stringeeError) {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        tvUserId.setText("Connect error: " + stringeeError.getMessage());
                    });
                }

                @Override
                public void onRequestNewToken(StringeeClient stringeeClient) {
                    // Get new token here and connect to Stringe server
                    Log.d("Stringee", "Token expired. Request new token here");
                }

                @Override
                public void onCustomMessage(String from, JSONObject msg) {
                }

                @Override
                public void onTopicMessage(String from, JSONObject msg) {

                }
            });
        }
        Common.client.connect(token);
    }
}
