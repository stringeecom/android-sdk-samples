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
import com.stringee.callpushnotificationsample.common.NotificationUtils;
import com.stringee.callpushnotificationsample.common.PermissionsUtils;
import com.stringee.callpushnotificationsample.common.RingtoneUtils;
import com.stringee.callpushnotificationsample.common.SensorManagerUtils;
import com.stringee.callpushnotificationsample.common.Utils;
import com.stringee.common.StringeeAudioManager;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;

import org.json.JSONObject;

public class IncomingCallActivity extends AppCompatActivity implements OnClickListener {
    private FrameLayout vLocal;
    private FrameLayout vRemote;
    private TextView tvState;
    private ImageButton btnEnd;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;
    private ImageButton btnVideo;
    private ImageButton btnSwitch;
    private View vControl;
    private View vIncoming;

    private StringeeCall stringeeCall;
    private SensorManagerUtils sensorManagerUtils;
    private StringeeAudioManager audioManager;
    private boolean isMute = false;
    private boolean isSpeaker = false;
    private boolean isVideo = false;
    private boolean isPermissionGranted = true;
    private boolean isVideoCall = false;

    private MediaState mMediaState;
    private SignalingState mSignalingState;

    private static final String TAG = "Stringee";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //add Flag for show on lockScreen, disable keyguard, keep screen on
        getWindow().addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_DISMISS_KEYGUARD
                | LayoutParams.FLAG_KEEP_SCREEN_ON
                | LayoutParams.FLAG_TURN_SCREEN_ON);

        if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        setContentView(layout.activity_incoming_call);

        sensorManagerUtils = SensorManagerUtils.getInstance(this).initialize(getLocalClassName());

        Common.isInCall = true;

        String callId = getIntent().getStringExtra("call_id");
        stringeeCall = Common.callsMap.get(callId);
        if (stringeeCall == null) {
            sensorManagerUtils.releaseSensor();
            Utils.postDelay(() -> {
                Common.isInCall = false;
                finish();
            }, 1000);
            return;
        }

        initView();

        NotificationUtils.getInstance(this).cancelNotification(NotificationUtils.INCOMING_CALL_ID);

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

        startRinging();
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

                sensorManagerUtils = SensorManagerUtils.getInstance(IncomingCallActivity.this).initialize(getLocalClassName());
                sensorManagerUtils.turnOff();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        runOnUiThread(() -> {
            if ( mSignalingState == SignalingState.CALLING || mSignalingState == SignalingState.RINGING || mSignalingState == SignalingState.ANSWERED) {
                getWindow().addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        LayoutParams.FLAG_DISMISS_KEYGUARD |
                        LayoutParams.FLAG_KEEP_SCREEN_ON |
                        LayoutParams.FLAG_TURN_SCREEN_ON);

                if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true);
                    setTurnScreenOn(true);
                }

                sensorManagerUtils = SensorManagerUtils.getInstance(IncomingCallActivity.this).initialize(getLocalClassName());
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
                endCall(false);
            } else {
                isPermissionGranted = true;
                startRinging();
            }
        }
    }

    private void initView() {
        vLocal = findViewById(id.v_local);
        vRemote = findViewById(id.v_remote);

        vControl = findViewById(id.v_control);
        vIncoming = findViewById(id.v_incoming);

        TextView tvFrom = findViewById(id.tv_from);
        tvFrom.setText(stringeeCall.getFrom());
        tvState = findViewById(id.tv_state);

        ImageButton btnAnswer = findViewById(id.btn_answer);
        btnAnswer.setOnClickListener(this);
        btnEnd = findViewById(id.btn_end);
        btnEnd.setOnClickListener(this);
        ImageButton btnReject = findViewById(id.btn_reject);
        btnReject.setOnClickListener(this);
        btnMute = findViewById(id.btn_mute);
        btnMute.setOnClickListener(this);
        btnSpeaker = findViewById(id.btn_speaker);
        btnSpeaker.setOnClickListener(this);
        btnVideo = findViewById(id.btn_video);
        btnVideo.setOnClickListener(this);
        btnSwitch = findViewById(id.btn_switch);
        btnSwitch.setOnClickListener(this);

        isVideoCall = stringeeCall.isVideoCall();

        isSpeaker = isVideoCall;
        btnSpeaker.setBackgroundResource(isSpeaker ? drawable.btn_speaker_on : drawable.btn_speaker_off);

        isVideo = isVideoCall;
        btnVideo.setImageResource(isVideo ? drawable.btn_video : drawable.btn_video_off);

        btnVideo.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        btnSwitch.setVisibility(isVideo ? View.VISIBLE : View.GONE);
    }

    private void startRinging() {
        if (isVideoCall) {
            sensorManagerUtils.disableKeyguard();
        } else {
            sensorManagerUtils.turnOn();
        }

        //create audio manager to control audio device
        audioManager = StringeeAudioManager.create(IncomingCallActivity.this);
        audioManager.start((selectedAudioDevice, availableAudioDevices) ->
                Log.d(TAG, "selectedAudioDevice: " + selectedAudioDevice + " - availableAudioDevices: " + availableAudioDevices));
        audioManager.setSpeakerphoneOn(isVideo);

        stringeeCall.setCallListener(new StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall stringeeCall, final SignalingState signalingState, String reason, int sipCode, String sipReason) {
                runOnUiThread(() -> {
                    Log.d(TAG, "onSignalingStateChange: " + signalingState);
                    mSignalingState = signalingState;
                    if (signalingState == SignalingState.ANSWERED) {
                        tvState.setText("Starting");
                        if (mMediaState == MediaState.CONNECTED) {
                            tvState.setText("Started");
                            RingtoneUtils.getInstance(IncomingCallActivity.this).stopRinging();
                        }
                    } else if (signalingState == SignalingState.ENDED) {
                        endCall(true);
                    }
                });
            }

            @Override
            public void onError(StringeeCall stringeeCall, int code, String desc) {
                runOnUiThread(() -> {
                    Log.d(TAG, "onError: " + desc);
                    Utils.reportMessage(IncomingCallActivity.this, desc);
                    tvState.setText("Ended");
                    dismissLayout();
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall stringeeCall, final SignalingState signalingState, String desc) {
                runOnUiThread(() -> {
                    Log.d(TAG, "onHandledOnAnotherDevice: " + desc);
                    if (signalingState != SignalingState.RINGING) {
                        Utils.reportMessage(IncomingCallActivity.this, desc);
                        tvState.setText("Ended");
                        dismissLayout();
                    }
                });
            }

            @Override
            public void onMediaStateChange(StringeeCall stringeeCall, final MediaState mediaState) {
                runOnUiThread(() -> {
                    Log.d(TAG, "onMediaStateChange: " + mediaState);
                    mMediaState = mediaState;
                    if (mediaState == MediaState.CONNECTED) {
                        if (mSignalingState == SignalingState.ANSWERED) {
                            tvState.setText("Started");
                            RingtoneUtils.getInstance(IncomingCallActivity.this).stopRinging();
                        }
                    } else {
                        tvState.setText("Reconnecting...");
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

        stringeeCall.ringing(new StatusListener() {
            @Override
            public void onSuccess() {
                Log.d("Stringee", "ringing success");
            }

            @Override
            public void onError(StringeeError stringeeError) {
                super.onError(stringeeError);
                runOnUiThread(() -> {
                    Log.d(TAG, "ringing error: " + stringeeError.getMessage());
                    Utils.reportMessage(IncomingCallActivity.this, stringeeError.getMessage());
                    endCall(false);
                });
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
        } else if (vId == id.btn_answer) {
            if (stringeeCall != null) {
                vControl.setVisibility(View.VISIBLE);
                vIncoming.setVisibility(View.GONE);
                btnEnd.setVisibility(View.VISIBLE);
                btnSwitch.setVisibility(isVideoCall ? View.VISIBLE : View.GONE);
                stringeeCall.answer(new StatusListener() {
                    @Override
                    public void onSuccess() {

                    }
                });
            }
        } else if (vId == id.btn_end) {
            endCall(true);
        } else if (vId == id.btn_reject) {
            endCall(false);
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
                            Log.d(TAG, "switchCamera error: " + stringeeError.getMessage());
                            Utils.reportMessage(IncomingCallActivity.this, stringeeError.getMessage());
                        });
                    }
                });
            }
        }
    }

    private void endCall(boolean isHangup) {
        if (stringeeCall != null) {
            tvState.setText("Ended");
            if (isHangup) {
                stringeeCall.hangup(new StatusListener() {
                    @Override
                    public void onSuccess() {

                    }
                });
            } else {
                stringeeCall.reject(new StatusListener() {
                    @Override
                    public void onSuccess() {

                    }
                });
            }
        }
        dismissLayout();
    }

    private void dismissLayout() {
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }
        RingtoneUtils.getInstance(IncomingCallActivity.this).stopRinging();
        vControl.setVisibility(View.GONE);
        vIncoming.setVisibility(View.GONE);
        btnEnd.setVisibility(View.GONE);
        btnSwitch.setVisibility(View.GONE);
        sensorManagerUtils.releaseSensor();
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