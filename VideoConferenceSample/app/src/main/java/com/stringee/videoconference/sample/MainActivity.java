package com.stringee.videoconference.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeConnectionListener;
import com.stringee.videoconference.videoconference.sample.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btnJoinRoom;
    private TextView tvTitle;

    private String accessToken = "PUT_ACCESS_TOKEN_HERE";
    private String roomToken = "PUT_ROOM_TOKEN_HERER";

    public static StringeeClient client;

    public static final int REQUEST_VIDEO_PERMISSION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        connect(accessToken);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.joinButton:
                if (client != null && client.isConnected()) {
                    if (checkPermission()) {
                        Intent intent = new Intent(this, RoomActivity.class);
                        intent.putExtra("room_token", roomToken);
                        startActivity(intent);
                    }
                } else {
                    Toast.makeText(this, "Please wait for connecting", Toast.LENGTH_SHORT).show();
                }

                break;
        }

    }

    private void initView() {
        tvTitle = findViewById(R.id.titleTextView);
        btnJoinRoom = findViewById(R.id.joinButton);
        btnJoinRoom.setOnClickListener(this);
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> lstPermissions = new ArrayList<>();
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.CAMERA);
            }

            if (lstPermissions.size() > 0) {
                String[] permissions = new String[lstPermissions.size()];
                for (int i = 0; i < lstPermissions.size(); i++) {
                    permissions[i] = lstPermissions.get(i);
                }
                ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_VIDEO_PERMISSION);
                return false;
            }
            return true;
        }
        return true;
    }

    private void connect(String accessToken) {
        tvTitle.setText(R.string.connecting);
        if (client == null) {
            client = new StringeeClient(this);
            client.setConnectionListener(new StringeeConnectionListener() {
                @Override
                public void onConnectionConnected(StringeeClient stringeeClient, boolean b) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvTitle.setText(getString(R.string.connected_as, client.getUserId()));
                        }
                    });
                }

                @Override
                public void onConnectionDisconnected(StringeeClient stringeeClient, boolean b) {

                }

                @Override
                public void onIncomingCall(StringeeCall stringeeCall) {

                }

                @Override
                public void onIncomingCall2(StringeeCall2 stringeeCall2) {

                }

                @Override
                public void onConnectionError(StringeeClient stringeeClient, StringeeError stringeeError) {

                }

                @Override
                public void onRequestNewToken(StringeeClient stringeeClient) {

                }

                @Override
                public void onCustomMessage(String s, JSONObject jsonObject) {

                }

                @Override
                public void onTopicMessage(String s, JSONObject jsonObject) {

                }
            });
        }
        client.connect(accessToken);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                                           int[] grantResults) {
        boolean isGranted = false;
        if (grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                    break;
                } else {
                    isGranted = true;
                }
            }
        }
        switch (requestCode) {
            case REQUEST_VIDEO_PERMISSION:
                if (!isGranted) {
                    return;
                } else {
                    if (client != null) {
                        if (client.isConnected()) {
                            Intent intent = new Intent(this, RoomActivity.class);
                            intent.putExtra("room_token", roomToken);
                            startActivity(intent);
                        }
                    } else {
                        Toast.makeText(this, "Please login first!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }
}