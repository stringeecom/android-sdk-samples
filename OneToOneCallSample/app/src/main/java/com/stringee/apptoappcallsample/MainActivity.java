package com.stringee.apptoappcallsample;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.stringee.StringeeClient;
import com.stringee.apptoappcallsample.utils.Utils;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCallParam;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeConnectionListener;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String token;
    public static StringeeClient client;
    private String myUserId = "stringee1";
    private String to;
    public static int callNum = 0;

    private EditText etTo;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvUserId = (TextView) findViewById(R.id.tv_userid);
        tvUserId.setText("Connected as: " + myUserId);

        Button btnVoiceCall = (Button) findViewById(R.id.btn_voice_call);
        btnVoiceCall.setOnClickListener(this);
        Button btnVideoCall = (Button) findViewById(R.id.btn_video_call);
        btnVideoCall.setOnClickListener(this);
        etTo = (EditText) findViewById(R.id.et_to);

        progressDialog = ProgressDialog.show(this, "", "Connecting...");
        progressDialog.setCancelable(true);
        progressDialog.show();

        initStringee();
        getTokenAndConnect(myUserId);
    }

    private void initStringee() {
        client = new StringeeClient(this);
        client.setConnectionListener(new StringeeConnectionListener() {
            @Override
            public void onConnectionConnected(StringeeClient stringeeClient, boolean isReconnecting) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Utils.reportMessage(MainActivity.this, "Stringee session connected.");
                    }
                });
            }

            @Override
            public void onConnectionDisconnected(StringeeClient stringeeClient, boolean isReconnecting) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                        Utils.reportMessage(MainActivity.this, "Stringee session disconnected.");
                    }
                });
            }

            @Override
            public void onIncomingCall(StringeeCall stringeeCall) {
                Intent intent = new Intent(MainActivity.this, IncomingCallActivity.class);
                intent.putExtra("stringeecall", stringeeCall);
                startActivity(intent);
            }

            @Override
            public void onConnectionError(StringeeClient stringeeClient, StringeeError stringeeError) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.reportMessage(MainActivity.this, "Stringee session fails to connect.");
                    }
                });
            }

            @Override
            public void onRefreshToken(StringeeClient stringeeClient) {
                getTokenAndConnect(myUserId);
            }
        });
    }

    private void getTokenAndConnect(final String userId) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String url = "https://v1.stringee.com/samples/your_server/access_token/access_token-test.php?u=" + userId;
                RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                StringRequest request = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            token = jsonObject.getString("access_token");
                            client.connect(token);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                    }
                });
                queue.add(request);
            }
        });
        thread.start();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_voice_call:
                to = etTo.getText().toString();
                if (to.trim().length() > 0) {
                    if (client.isConnected()) {
                        Intent intent = new Intent(this, OutgoingCallActivity.class);
                        intent.putExtra("from", myUserId);
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
                        intent.putExtra("from", myUserId);
                        intent.putExtra("to", to);
                        intent.putExtra("is_video_call", true);
                        startActivity(intent);
                    } else {
                        Utils.reportMessage(this, "Stringee session not connected");
                    }
                }
                break;
        }
    }


}
