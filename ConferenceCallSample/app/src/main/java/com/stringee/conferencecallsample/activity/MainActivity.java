package com.stringee.conferencecallsample.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.conferencecallsample.R;
import com.stringee.conferencecallsample.utils.Utils;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeConnectionListener;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String token;
    public static StringeeClient client;
    private String myUserId = "stringee" + System.currentTimeMillis(); //

    private EditText etRoomId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnMakeRoom = (Button) findViewById(R.id.btn_make_room);
        btnMakeRoom.setOnClickListener(this);
        Button btnJoinRoom = (Button) findViewById(R.id.btn_join_room);
        btnJoinRoom.setOnClickListener(this);

        etRoomId = (EditText) findViewById(R.id.et_room_id);

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
            public void onConnectionConnected(StringeeClient client, boolean isReconnecting) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
            public void onRefreshToken(StringeeClient client) {
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
