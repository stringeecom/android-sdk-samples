package com.stringee.videocallsample;

import android.Manifest.permission;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.stringee.call.StringeeCall2;
import com.stringee.call.StringeeCall2.MediaState;
import com.stringee.call.StringeeCall2.SignalingState;
import com.stringee.call.StringeeCall2.StringeeCallListener;
import com.stringee.common.StringeeAudioManager;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.video.StringeeVideoTrack;
import com.stringee.video.StringeeVideoTrack.MediaType;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OutgoingCallActivity extends AppCompatActivity implements View.OnClickListener {
    private FrameLayout vLocal;
    private FrameLayout vRemote;
    private TextView tvState;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;
    private ImageButton btnVideo;
    private ImageButton btnEnd;
    private ImageButton btnSwitch;
    private View vControl;

    private StringeeCall2 stringeeCall;
    private SensorManagerUtils sensorManagerUtils;
    private StringeeAudioManager audioManager;
    private String from;
    private String to;
    private boolean isMute = false;
    private boolean isSpeaker = true;
    private boolean isVideo = true;
    private boolean isPermissionGranted = true;

    private MediaState mMediaState;
    private SignalingState mSignalingState;

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

        setContentView(R.layout.activity_outgoing_call);

        sensorManagerUtils = SensorManagerUtils.getInstance(this);
        sensorManagerUtils.acquireProximitySensor(getLocalClassName());
        sensorManagerUtils.disableKeyguard();

        Common.isInCall = true;

        from = getIntent().getStringExtra("from");
        to = getIntent().getStringExtra("to");

        initView();

        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            List<String> lstPermissions = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this, permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(permission.RECORD_AUDIO);
            }

            if (ContextCompat.checkSelfPermission(this, permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(permission.CAMERA);
            }

            if (VERSION.SDK_INT >= VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    lstPermissions.add(permission.BLUETOOTH_CONNECT);
                }
            }

            if (lstPermissions.size() > 0) {
                String[] permissions = new String[lstPermissions.size()];
                for (int i = 0; i < lstPermissions.size(); i++) {
                    permissions[i] = lstPermissions.get(i);
                }
                ActivityCompat.requestPermissions(this, permissions, Common.REQUEST_PERMISSION_CALL);
                return;
            }
        }

        makeCall();
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
        if (requestCode == Common.REQUEST_PERMISSION_CALL) {
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
        vLocal = findViewById(R.id.v_local);
        vRemote = findViewById(R.id.v_remote);

        vControl = findViewById(R.id.v_control);

        TextView tvTo = findViewById(R.id.tv_to);
        tvTo.setText(to);
        tvState = findViewById(R.id.tv_state);

        btnMute = findViewById(R.id.btn_mute);
        btnMute.setOnClickListener(this);
        btnSpeaker = findViewById(R.id.btn_speaker);
        btnSpeaker.setOnClickListener(this);
        btnVideo = findViewById(R.id.btn_video);
        btnVideo.setOnClickListener(this);
        btnSwitch = findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(this);
        btnEnd = findViewById(R.id.btn_end);
        btnEnd.setOnClickListener(this);

        btnSpeaker.setBackgroundResource(isSpeaker ? R.drawable.btn_speaker_on : R.drawable.btn_speaker_off);
        btnVideo.setImageResource(isVideo ? R.drawable.btn_video : R.drawable.btn_video_off);

        btnVideo.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        btnSwitch.setVisibility(isVideo ? View.VISIBLE : View.GONE);
    }

    private void makeCall() {
        //create audio manager to control audio device
        audioManager = StringeeAudioManager.create(OutgoingCallActivity.this);
        audioManager.start(new StringeeAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(StringeeAudioManager.AudioDevice audioDevice, Set<StringeeAudioManager.AudioDevice> set) {

            }
        });
        audioManager.setSpeakerphoneOn(true);

        //make a call
        stringeeCall = new StringeeCall2(Common.client, from, to);
        stringeeCall.setVideoCall(true);

        stringeeCall.setCallListener(new StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall2 stringeeCall2, final SignalingState signalingState, String reason, int sipCode, String sipReason) {
                runOnUiThread(() -> {
                    Log.d("Stringee", "onSignalingStateChange: " + signalingState);
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
            public void onError(StringeeCall2 stringeeCall2, int code, String desc) {
                runOnUiThread(() -> {
                    Log.d("Stringee", "onError: " + desc);
                    tvState.setText("Ended");
                    dismissLayout();
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall2 stringeeCall2, SignalingState signalingState, String desc) {
            }

            @Override
            public void onMediaStateChange(StringeeCall2 stringeeCall2, final MediaState mediaState) {
                runOnUiThread(() -> {
                    Log.d("Stringee", "onMediaStateChange: " + mediaState);
                    mMediaState = mediaState;
                    if (mediaState == MediaState.CONNECTED) {
                        if (mSignalingState == SignalingState.ANSWERED) {
                            tvState.setText("Started");
                        }
                    }
                });
            }

            @Override
            public void onLocalStream(final StringeeCall2 stringeeCall2) {
                runOnUiThread(() -> {
                    Log.d("Stringee", "onLocalStream");
                    if (stringeeCall2.isVideoCall()) {
                        vLocal.removeAllViews();
                        vLocal.addView(stringeeCall2.getLocalView());
                        stringeeCall2.renderLocalView(true);
                    }
                });
            }

            @Override
            public void onRemoteStream(final StringeeCall2 stringeeCall2) {
                runOnUiThread(() -> {
                    Log.d("Stringee", "onRemoteStream");
                    if (stringeeCall2.isVideoCall()) {
                        vRemote.removeAllViews();
                        vRemote.addView(stringeeCall2.getRemoteView());
                        stringeeCall2.renderRemoteView(false);
                    }
                });
            }

            @Override
            public void onVideoTrackAdded(StringeeVideoTrack stringeeVideoTrack) {

            }

            @Override
            public void onVideoTrackRemoved(StringeeVideoTrack stringeeVideoTrack) {

            }

            @Override
            public void onCallInfo(StringeeCall2 stringeeCall2, final JSONObject jsonObject) {
            }

            @Override
            public void onTrackMediaStateChange(String from, MediaType mediaType, boolean enable) {

            }
        });

        stringeeCall.makeCall();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_mute:
                isMute = !isMute;
                btnMute.setBackgroundResource(isMute ? R.drawable.btn_mute : R.drawable.btn_mic);
                if (stringeeCall != null) {
                    stringeeCall.mute(isMute);
                }
                break;
            case R.id.btn_speaker:
                isSpeaker = !isSpeaker;
                btnSpeaker.setBackgroundResource(isSpeaker ? R.drawable.btn_speaker_on : R.drawable.btn_speaker_off);
                if (audioManager != null) {
                    audioManager.setSpeakerphoneOn(isSpeaker);
                }
                break;
            case R.id.btn_end:
                tvState.setText("Ended");
                endCall();
                break;
            case R.id.btn_video:
                isVideo = !isVideo;
                btnVideo.setImageResource(isVideo ? R.drawable.btn_video : R.drawable.btn_video_off);
                if (stringeeCall != null) {
                    stringeeCall.enableVideo(isVideo);
                }
                break;
            case R.id.btn_switch:
                if (stringeeCall != null) {
                    stringeeCall.switchCamera(new StatusListener() {
                        @Override
                        public void onSuccess() {

                        }

                        @Override
                        public void onError(StringeeError stringeeError) {
                        }
                    });
                }
                break;
        }
    }

    private void endCall() {
        stringeeCall.hangup();
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
