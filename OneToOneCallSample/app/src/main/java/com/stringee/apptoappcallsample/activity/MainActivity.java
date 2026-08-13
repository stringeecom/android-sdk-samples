package com.stringee.apptoappcallsample.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.stringee.apptoappcallsample.R;
import com.stringee.apptoappcallsample.databinding.ActivityMainBinding;
import com.stringee.apptoappcallsample.stringee.common.CallEngine;
import com.stringee.apptoappcallsample.stringee.common.CallStatus;
import com.stringee.apptoappcallsample.stringee.common.ConnectionState;
import com.stringee.apptoappcallsample.stringee.common.StringeeCallConfig;
import com.stringee.apptoappcallsample.stringee.listener.StringeeCallListener;
import com.stringee.apptoappcallsample.stringee.manager.StringeeCallManager;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;

/** Host screen that connects a token and delegates outgoing calls to the Stringee facade. */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityMainBinding binding;
    private StringeeCallManager callManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        callManager = StringeeCallManager.getInstance(this);
        binding.etToken.setText(callManager.getSavedToken());
        bindActions();
        renderConnection(callManager.isConnected() ? ConnectionState.CONNECTED
                : ConnectionState.DISCONNECTED, callManager.getConnectedUserId());
        callManager.initialize(new StringeeCallListener() {
            @Override
            public void onConnectionStateChanged(ConnectionState state, String userId) {
                runOnUiThread(() -> renderConnection(state, userId));
            }

            @Override
            public void onCallStateChanged(CallStatus state) {
                runOnUiThread(() -> binding.tvCallStatus.setText(
                        getString(R.string.call_state, state.getValue())));
            }

            @Override
            public void onError(String action, StringeeError error) {
                runOnUiThread(() -> {
                    String message = action + ": " + error.getMessage();
                    binding.tvLastError.setText(getString(R.string.last_error, message));
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onRequestNewToken() {
                runOnUiThread(() -> binding.tvLastError.setText(
                        R.string.token_refresh_required));
            }
        });
        callManager.handleLaunchIntent(getIntent());
        if (callManager.isConnected()) {
            renderConnection(ConnectionState.CONNECTED, callManager.getConnectedUserId());
        }
    }

    private void bindActions() {
        binding.btnConnect.setOnClickListener(this);
        binding.btnDisconnect.setOnClickListener(this);
        binding.btnNotificationPermission.setOnClickListener(this);
        binding.btnFullScreenPermission.setOnClickListener(this);
        binding.btnAppSettings.setOnClickListener(this);
        binding.btnVoiceCall.setOnClickListener(this);
        binding.btnVideoCall.setOnClickListener(this);
        binding.btnVoiceCall2.setOnClickListener(this);
        binding.btnVideoCall2.setOnClickListener(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (callManager != null) {
            callManager.handleLaunchIntent(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderNotificationAccess();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        renderNotificationAccess();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_connect) {
            callManager.connect(binding.etToken.getText().toString());
        } else if (id == R.id.btn_disconnect) {
            callManager.disconnect();
        } else if (id == R.id.btn_notification_permission) {
            callManager.requestNotificationPermission(this);
        } else if (id == R.id.btn_full_screen_permission) {
            callManager.openFullScreenIntentSettings(this);
        } else if (id == R.id.btn_app_settings) {
            callManager.openAppSettings(this);
        } else if (id == R.id.btn_voice_call) {
            makeCall(CallEngine.STRINGEE_CALL, false);
        } else if (id == R.id.btn_video_call) {
            makeCall(CallEngine.STRINGEE_CALL, true);
        } else if (id == R.id.btn_voice_call2) {
            makeCall(CallEngine.STRINGEE_CALL2, false);
        } else if (id == R.id.btn_video_call2) {
            makeCall(CallEngine.STRINGEE_CALL2, true);
        }
    }

    private void makeCall(CallEngine engine, boolean videoCall) {
        StringeeCallConfig config = new StringeeCallConfig(
                binding.etTo.getText().toString(), engine, videoCall);
        callManager.makeCall(config, new StatusListener() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> binding.tvLastError.setText(""));
            }

            @Override
            public void onError(StringeeError error) {
                // The facade also forwards this error through StringeeCallListener.
            }
        });
    }

    private void renderConnection(ConnectionState state, String userId) {
        boolean connected = state == ConnectionState.CONNECTED;
        String suffix = connected && userId != null && !userId.isEmpty()
                ? " (" + userId + ")" : "";
        binding.tvConnectionStatus.setText(
                getString(R.string.connection_state, state.name() + suffix));
        binding.btnConnect.setEnabled(!connected && state != ConnectionState.CONNECTING);
        binding.btnDisconnect.setEnabled(connected || state == ConnectionState.CONNECTING);
        binding.btnDisconnect.setVisibility(
                connected || state == ConnectionState.CONNECTING ? View.VISIBLE : View.GONE);
        binding.layoutConnectionCard.setVisibility(connected ? View.GONE : View.VISIBLE);
        binding.layoutMakeCall.setVisibility(connected ? View.VISIBLE : View.GONE);
        binding.btnVoiceCall.setEnabled(connected);
        binding.btnVideoCall.setEnabled(connected);
        binding.btnVoiceCall2.setEnabled(connected);
        binding.btnVideoCall2.setEnabled(connected);
        int statusBackground;
        int statusColor;
        if (state == ConnectionState.CONNECTED) {
            statusBackground = R.drawable.bg_stringee_status_success;
            statusColor = R.color.stringee_green_dark;
        } else if (state == ConnectionState.ERROR) {
            statusBackground = R.drawable.bg_stringee_status_error;
            statusColor = R.color.stringee_error;
        } else {
            statusBackground = R.drawable.bg_stringee_status_info;
            statusColor = R.color.stringee_blue;
        }
        binding.layoutConnectionStatus.setBackgroundResource(statusBackground);
        binding.tvConnectionStatus.setTextColor(ContextCompat.getColor(this, statusColor));
    }

    private void renderNotificationAccess() {
        boolean notification = callManager != null && callManager.canPostNotifications();
        boolean fullScreen = callManager != null && callManager.canUseFullScreenIntent();
        boolean needsAccess = !notification || !fullScreen;
        binding.layoutIncomingCallAccess.setVisibility(needsAccess ? View.VISIBLE : View.GONE);
        binding.tvNotificationStatus.setText(getString(R.string.notification_access_state,
                notification ? getString(R.string.granted) : getString(R.string.not_granted),
                fullScreen ? getString(R.string.granted) : getString(R.string.not_granted)));
        binding.btnNotificationPermission.setVisibility(notification ? View.GONE : View.VISIBLE);
        binding.btnFullScreenPermission.setVisibility(fullScreen ? View.GONE : View.VISIBLE);
    }
}
