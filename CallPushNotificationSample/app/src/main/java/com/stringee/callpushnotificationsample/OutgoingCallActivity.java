package com.stringee.callpushnotificationsample;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall.MediaState;
import com.stringee.call.StringeeCall.SignalingState;
import com.stringee.call.StringeeCall.StringeeCallListener;
import com.stringee.callpushnotificationsample.R.drawable;
import com.stringee.callpushnotificationsample.R.id;
import com.stringee.callpushnotificationsample.R.layout;
import com.stringee.callpushnotificationsample.common.Common;
import com.stringee.callpushnotificationsample.common.PermissionsUtils;
import com.stringee.callpushnotificationsample.common.SensorManagerUtils;
import com.stringee.callpushnotificationsample.common.Utils;
import com.stringee.common.StringeeAudioManager;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;

import org.json.JSONObject;

public class OutgoingCallActivity extends AppCompatActivity implements OnClickListener {
    private FrameLayout vLocal;
    private FrameLayout vRemote;
    private TextView tvState;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;
    private ImageButton btnVideo;
    private ImageButton btnEnd;
    private ImageButton btnSwitch;
    private View vControl;

    private StringeeCall stringeeCall;
    private SensorManagerUtils sensorManagerUtils;
    private StringeeAudioManager audioManager;
    private String from;
    private String to;
    private boolean isVideoCall;
    private boolean isMute = false;
    private boolean isSpeaker = false;
    private boolean isVideo = false;
    private boolean isPermissionGranted = true;

    private MediaState mMediaState;
    private SignalingState mSignalingState;

    private static final String TAG = "Stringee";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //add Flag for show on lockScreen and disable keyguard
        getWindow().addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_DISMISS_KEYGUARD
                | LayoutParams.FLAG_KEEP_SCREEN_ON
                | LayoutParams.FLAG_TURN_SCREEN_ON);

        if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        setContentView(layout.activity_outgoing_call);

        sensorManagerUtils = SensorManagerUtils.getInstance(this).initialize(getLocalClassName());

        Common.isInCall = true;

        from = getIntent().getStringExtra("from");
        to = getIntent().getStringExtra("to");
        isVideoCall = getIntent().getBooleanExtra("is_video_call", false);

        initView();

        // Check permission
        if (isVideoCall) {
            if (!PermissionsUtils.isVideoCallPermissionGranted(this)) {
                PermissionsUtils.requestVideoCallPermission(this);
                return;
            }
        } else {
            if (!PermissionsUtils.isVoiceCallPermissionGranted(this)) {
                PermissionsUtils.requestVoiceCallPermission(this);
                return;
            }
        }

        makeCall();
    }

    @Override
    protected void onPause() {
        super.onPause();
        runOnUiThread(() -> {
            if (mSignalingState == SignalingState.CALLING || mSignalingState == SignalingState.RINGING || mSignalingState == SignalingState.ANSWERED) {
                getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON |
                        LayoutParams.FLAG_DISMISS_KEYGUARD |
                        LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        LayoutParams.FLAG_TURN_SCREEN_ON);

                if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
                    setShowWhenLocked(false);
                    setTurnScreenOn(false);
                }

                sensorManagerUtils = SensorManagerUtils.getInstance(OutgoingCallActivity.this).initialize(getLocalClassName());
                sensorManagerUtils.turnOff();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        runOnUiThread(() -> {
            if (mSignalingState == SignalingState.CALLING || mSignalingState == SignalingState.RINGING || mSignalingState == SignalingState.ANSWERED) {
                getWindow().addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        LayoutParams.FLAG_DISMISS_KEYGUARD |
                        LayoutParams.FLAG_KEEP_SCREEN_ON |
                        LayoutParams.FLAG_TURN_SCREEN_ON);

                if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true);
                    setTurnScreenOn(true);
                }

                sensorManagerUtils = SensorManagerUtils.getInstance(OutgoingCallActivity.this).initialize(getLocalClassName());
                if (isVideoCall) {
                    sensorManagerUtils.disableKeyguard();
                } else {
                    sensorManagerUtils.turnOn();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = false;
        if (grantResults.length > 0) {
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                    break;
                } else {
                    isGranted = true;
                }
            }
        }
        if (requestCode == PermissionsUtils.REQUEST_CALL_PERMISSION) {
            if (!isGranted) {
                isPermissionGranted = false;
                tvState.setText("Ended");
                dismissLayout();
            } else {
                isPermissionGranted = true;
                makeCall();
            }
        }
    }

    private void initView() {
        vLocal = findViewById(id.v_local);
        vRemote = findViewById(id.v_remote);

        vControl = findViewById(id.v_control);

        TextView tvTo = findViewById(id.tv_to);
        tvTo.setText(to);
        tvState = findViewById(id.tv_state);

        btnMute = findViewById(id.btn_mute);
        btnMute.setOnClickListener(this);
        btnSpeaker = findViewById(id.btn_speaker);
        btnSpeaker.setOnClickListener(this);
        btnVideo = findViewById(id.btn_video);
        btnVideo.setOnClickListener(this);
        btnSwitch = findViewById(id.btn_switch);
        btnSwitch.setOnClickListener(this);
        btnEnd = findViewById(id.btn_end);
        btnEnd.setOnClickListener(this);

        isSpeaker = isVideoCall;
        btnSpeaker.setBackgroundResource(isSpeaker ? drawable.btn_speaker_on : drawable.btn_speaker_off);

        isVideo = isVideoCall;
        btnVideo.setImageResource(isVideo ? drawable.btn_video : drawable.btn_video_off);

        btnVideo.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        btnSwitch.setVisibility(isVideo ? View.VISIBLE : View.GONE);
    }

    private void makeCall() {
        if (isVideoCall) {
            sensorManagerUtils.disableKeyguard();
        } else {
            sensorManagerUtils.turnOn();
        }

        //create audio manager to control audio device
        audioManager = StringeeAudioManager.create(OutgoingCallActivity.this);
        audioManager.start((selectedAudioDevice, availableAudioDevices) ->
                Log.d(TAG, "selectedAudioDevice: " + selectedAudioDevice + " - availableAudioDevices: " + availableAudioDevices));
        audioManager.setSpeakerphoneOn(isVideoCall);

        //make new call
        stringeeCall = new StringeeCall(Common.client, from, to);
        stringeeCall.setVideoCall(isVideoCall);

        stringeeCall.setCallListener(new StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall stringeeCall, final SignalingState signalingState, String reason, int sipCode, String sipReason) {
                runOnUiThread(() -> {
                    Log.d(TAG, "onSignalingStateChange: " + signalingState);
                    mSignalingState = signalingState;
                    switch (signalingState) {
                        case CALLING:
                            tvState.setText("Outgoing call");
                            break;
                        case RINGING:
                            tvState.setText("Ringing");
                            break;
                        case ANSWERED:
                            tvState.setText("Starting");
                            if (mMediaState == MediaState.CONNECTED) {
                                tvState.setText("Started");
                            }
                            break;
                        case BUSY:
                            tvState.setText("Busy");
                            endCall();
                            break;
                        case ENDED:
                            tvState.setText("Ended");
                            endCall();
                            break;
                    }
                });
            }

            @Override
            public void onError(StringeeCall stringeeCall, int code, String desc) {
                runOnUiThread(() -> {
                    Log.d(TAG, "onError: " + desc);
                    Utils.reportMessage(OutgoingCallActivity.this, desc);
                    tvState.setText("Ended");
                    dismissLayout();
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall stringeeCall, SignalingState signalingState, String desc) {

            }

            @Override
            public void onMediaStateChange(StringeeCall stringeeCall, final MediaState mediaState) {
                runOnUiThread(() -> {
                    Log.d(TAG, "onMediaStateChange: " + mediaState);
                    mMediaState = mediaState;
                    if (mediaState == MediaState.CONNECTED) {
                        if (mSignalingState == SignalingState.ANSWERED) {
                            tvState.setText("Started");
                        }
                    }
                });
            }

            @Override
            public void onLocalStream(final StringeeCall stringeeCall) {
                runOnUiThread(() -> {
                    Log.d(TAG, "onLocalStream");
                    if (stringeeCall.isVideoCall()) {
                        vLocal.removeAllViews();
                        vLocal.addView(stringeeCall.getLocalView());
                        stringeeCall.renderLocalView(true);
                    }
                });
            }

            @Override
            public void onRemoteStream(final StringeeCall stringeeCall) {
                runOnUiThread(() -> {
                    Log.d(TAG, "onRemoteStream");
                    if (stringeeCall.isVideoCall()) {
                        vRemote.removeAllViews();
                        vRemote.addView(stringeeCall.getRemoteView());
                        stringeeCall.renderRemoteView(false);
                    }
                });
            }

            @Override
            public void onCallInfo(StringeeCall stringeeCall, final JSONObject jsonObject) {
                runOnUiThread(() -> Log.d(TAG, "onCallInfo: " + jsonObject.toString()));
            }
        });

        stringeeCall.makeCall(new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    @Override
    public void onClick(View view) {
        int vId = view.getId();
        if (vId == id.btn_mute) {
            if (stringeeCall != null) {
                isMute = !isMute;
                btnMute.setBackgroundResource(isMute ? drawable.btn_mute : drawable.btn_mic);
                stringeeCall.mute(isMute);
            }
        } else if (vId == id.btn_speaker) {
            if (audioManager != null) {
                isSpeaker = !isSpeaker;
                btnSpeaker.setBackgroundResource(isSpeaker ? drawable.btn_speaker_on : drawable.btn_speaker_off);
                audioManager.setSpeakerphoneOn(isSpeaker);
            }
        } else if (vId == id.btn_end) {
            tvState.setText("Ended");
            endCall();
        } else if (vId == id.btn_video) {
            if (stringeeCall != null) {
                isVideo = !isVideo;
                btnVideo.setImageResource(isVideo ? drawable.btn_video : drawable.btn_video_off);
                stringeeCall.enableVideo(isVideo);
            }
        } else if (vId == id.btn_switch) {
            if (stringeeCall != null) {
                stringeeCall.switchCamera(new StatusListener() {
                    @Override
                    public void onSuccess() {
                    }

                    @Override
                    public void onError(StringeeError stringeeError) {
                        super.onError(stringeeError);
                        runOnUiThread(() -> {
                            Utils.reportMessage(OutgoingCallActivity.this, stringeeError.getMessage());
                        });
                    }
                });
            }
        }
    }

    private void endCall() {
        stringeeCall.hangup(new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
        dismissLayout();
    }

    private void dismissLayout() {
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        sensorManagerUtils.releaseSensor();
        vControl.setVisibility(View.GONE);
        btnEnd.setVisibility(View.GONE);
        btnSwitch.setVisibility(View.GONE);
        Utils.postDelay(() -> {
            Common.isInCall = false;
            if (!isPermissionGranted) {
                Intent intent = new Intent();
                intent.setAction("open_app_setting");
                setResult(RESULT_CANCELED, intent);
            }
            finish();
        }, 1000);
    }
}