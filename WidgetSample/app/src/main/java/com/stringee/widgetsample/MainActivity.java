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
    public static String accessToken = "eyJhbGciOiJIUzI1NiIsImN0eSI6InN0cmluZ2VlLWFwaTt2PTEiLCJ0eXAiOiJKV1QifQ.eyJleHAiOjE2NjkxODA2NTQsInVzZXJJZCI6IlZUX1RFU1RfSEFVTlRfQlAiLCJpc3MiOiJTS1NWOTNIRTRrT2xEOGpMeDVzY2toUUVaSzRYTGE3T0JKIiwianRpIjoiU0tTVjkzSEU0a09sRDhqTHg1c2NraFFFWks0WExhN09CSi0xNjY5MTc3MDU0NTgwIn0.qGsJvdBxl_ZsW4D2eIHafmkuPvf88ESYrBd7Nw_C26w";
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
        java.util.List<com.stringee.common.SocketAddress> socketAddressList= new java.util.ArrayList<>();
        socketAddressList.add(new com.stringee.common.SocketAddress("ccv1.viettel.vn",9879));
        socketAddressList.add(new com.stringee.common.SocketAddress("ccv2.viettel.vn",9879));
        stringeeWidget.setHost(socketAddressList);
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
