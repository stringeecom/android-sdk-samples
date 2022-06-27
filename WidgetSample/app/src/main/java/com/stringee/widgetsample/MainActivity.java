package com.stringee.widgetsample;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.widget.CallConfig;
import com.stringee.widget.StringeeListener;
import com.stringee.widget.StringeeWidget;

public class MainActivity extends AppCompatActivity {

    //put your access token here
    private String accessToken = "YOUR_ACCESS_TOKEN";
    private StringeeWidget stringeeWidget;
    private String to;

    private EditText etTo;
    private TextView tvUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvUserId = findViewById(R.id.tv_userid);
        etTo = findViewById(R.id.et_to);

        Button btnVoiceCall = findViewById(R.id.btn_voice_call);
        btnVoiceCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                to = etTo.getText().toString().trim();
                if (to.length() > 0) {
                    CallConfig config = new CallConfig(stringeeWidget.getClient().getUserId(), to);
                    stringeeWidget.makeCall(config, new StatusListener() {
                        @Override
                        public void onSuccess() {

                        }
                    });
                }
            }
        });
        Button btnVideoCall = findViewById(R.id.btn_video_call);
        btnVideoCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                to = etTo.getText().toString().trim();
                if (to.length() > 0) {
                    CallConfig config = new CallConfig(stringeeWidget.getClient().getUserId(), to);
                    config.setVideoCall(true);
                    stringeeWidget.makeCall(config, new StatusListener() {
                        @Override
                        public void onSuccess() {

                        }
                    });
                }
            }
        });


        initAndConnectStringee(accessToken);
    }

    private void initAndConnectStringee(String token) {
        stringeeWidget = StringeeWidget.getInstance(this);
        stringeeWidget.setListener(new StringeeListener() {
            @Override
            public void onConnectionConnected() {
                Log.d("Stringee", "onConnectionConnected");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvUserId.setText("Connected as: " + stringeeWidget.getClient().getUserId());
                    }
                });
            }

            @Override
            public void onConnectionDisconnected() {
                Log.d("Stringee", "onConnectionDisconnected");
            }

            @Override
            public void onConnectionError(StringeeError error) {
                Log.d("Stringee", "onConnectionError: " + error.getMessage());
            }

            @Override
            public void onRequestNewToken() {
                Log.d("Stringee", "onRequestNewToken");
            }

            @Override
            public void onCallStateChange(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState) {

            }

            @Override
            public void onCallStateChange2(StringeeCall2 stringeeCall, StringeeCall2.SignalingState signalingState) {

            }
        });
        stringeeWidget.connect(token);
    }
}
