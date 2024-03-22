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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeConnectionListener;
import com.stringee.videoconference.videoconference.sample.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private TextView tvTitle;

    private final String accessToken = "PUT_YOUR_TOKEN_HERE";
    private final String roomToken = "PUT_YOUR_ROOM_TOKEN_HERE";

    public static StringeeClient client;

    public static final int REQUEST_VIDEO_CONFERENCE_PERMISSION = 1;

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
        if (view.getId() == R.id.joinButton) {
            if (client != null && client.isConnected()) {
                if (checkPermission()) {
                    Intent intent = new Intent(this, RoomActivity.class);
                    intent.putExtra("room_token", roomToken);
                    startActivity(intent);
                }
            } else {
                Toast.makeText(this, "Please wait for connecting", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private void initView() {
        tvTitle = findViewById(R.id.titleTextView);
        Button btnJoinRoom = findViewById(R.id.joinButton);
        btnJoinRoom.setOnClickListener(this);
    }

    private boolean checkPermission() {
        List<String> lstPermissions = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            lstPermissions.add(Manifest.permission.RECORD_AUDIO);
        }

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            lstPermissions.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (lstPermissions.size() > 0) {
            String[] permissions = new String[lstPermissions.size()];
            for (int i = 0; i < lstPermissions.size(); i++) {
                permissions[i] = lstPermissions.get(i);
            }
            ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_VIDEO_CONFERENCE_PERMISSION);
            return false;
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
                    runOnUiThread(() -> tvTitle.setText(getString(R.string.connected_as, client.getUserId())));
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = false;
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                isGranted = false;
                break;
            } else {
                isGranted = true;
            }
        }
        if (requestCode == REQUEST_VIDEO_CONFERENCE_PERMISSION) {
            if (isGranted) {
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
        }
    }
}