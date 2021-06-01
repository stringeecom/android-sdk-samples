package com.stringee.apptoappcallsample;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.stringee.apptoappcallsample.R.id;
import com.stringee.call.StringeeCall2;
import com.stringee.call.StringeeCall2.MediaState;
import com.stringee.call.StringeeCall2.SignalingState;
import com.stringee.common.StringeeAudioManager;
import com.stringee.listener.StatusListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class IncomingCall2Activity extends AppCompatActivity implements View.OnClickListener {
    private FrameLayout mLocalViewContainer;
    private FrameLayout mRemoteViewContainer;
    private TextView tvFrom;
    private TextView tvState;
    private ImageButton btnAnswer;
    private ImageButton btnEnd;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;
    private ImageButton btnVideo;
    private ImageButton btnSwitch;
    private View vControl;

    private StringeeCall2 mStringeeCall2;
    private StringeeAudioManager audioManager;
    private boolean isMute = false;
    private boolean isSpeaker = false;
    private boolean isVideo = false;

    private StringeeCall2.MediaState mMediaState;
    private StringeeCall2.SignalingState mSignalingState;

    private KeyguardLock lock;

    public static final int REQUEST_PERMISSION_CALL = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //add Flag for show on lockScreen, disable keyguard, keep screen on
        getWindow().addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_DISMISS_KEYGUARD
                | LayoutParams.FLAG_KEEP_SCREEN_ON
                | LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_incoming_call);

        lock = ((KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE)).newKeyguardLock(Context.KEYGUARD_SERVICE);
        lock.disableKeyguard();

        if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        Common.isInCall = true;

        String callId = getIntent().getStringExtra("call_id");
        mStringeeCall2 = Common.calls2Map.get(callId);

        mLocalViewContainer = findViewById(id.v_local);
        mRemoteViewContainer = findViewById(id.v_remote);

        tvFrom = findViewById(id.tv_from);
        tvFrom.setText(mStringeeCall2.getFrom());
        tvState = findViewById(id.tv_state);

        btnAnswer = findViewById(id.btn_answer);
        btnAnswer.setOnClickListener(this);
        btnEnd = findViewById(id.btn_end);
        btnEnd.setOnClickListener(this);
        btnMute = findViewById(id.btn_mute);
        btnMute.setOnClickListener(this);
        btnSpeaker = findViewById(id.btn_speaker);
        btnSpeaker.setOnClickListener(this);
        btnVideo = findViewById(id.btn_video);
        btnVideo.setOnClickListener(this);
        btnSwitch = findViewById(id.btn_switch);
        btnSwitch.setOnClickListener(this);

        isSpeaker = mStringeeCall2.isVideoCall();
        btnSpeaker.setBackgroundResource(isSpeaker ? R.drawable.btn_speaker_on : R.drawable.btn_speaker_off);

        vControl = findViewById(R.id.v_control);
        isVideo = mStringeeCall2.isVideoCall();
        btnVideo.setImageResource(isVideo ? R.drawable.btn_video : R.drawable.btn_video_off);

        btnVideo.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        btnSwitch.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> lstPermissions = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (mStringeeCall2.isVideoCall()) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    lstPermissions.add(Manifest.permission.CAMERA);
                }
            }

            if (lstPermissions.size() > 0) {
                String[] permissions = new String[lstPermissions.size()];
                for (int i = 0; i < lstPermissions.size(); i++) {
                    permissions[i] = lstPermissions.get(i);
                }
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CALL);
                return;
            }
        }

        startRinging();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean isGranted = false;
        if (grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    isGranted = false;
                    break;
                } else {
                    isGranted = true;
                }
            }
        }
        if (requestCode == REQUEST_PERMISSION_CALL) {
            if (!isGranted) {
                tvState.setText("Ended");
                endCall(false, true);
            } else {
                startRinging();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        tvState.setText("Ended");
        endCall(true, false);
    }

    private void startRinging() {
        //create audio manager to control audio device
        if (audioManager == null) {
            audioManager = StringeeAudioManager.create(IncomingCall2Activity.this);
            audioManager.start(new StringeeAudioManager.AudioManagerEvents() {
                @Override
                public void onAudioDeviceChanged(StringeeAudioManager.AudioDevice selectedAudioDevice, Set<StringeeAudioManager.AudioDevice> availableAudioDevices) {
                }
            });
        }
        audioManager.setSpeakerphoneOn(isVideo);

        mStringeeCall2.setCallListener(new StringeeCall2.StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall2 stringeeCall2, final StringeeCall2.SignalingState signalingState, String s, int i, String s1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSignalingState = signalingState;
                        switch (signalingState) {
                            case ANSWERED:
                                tvState.setText("Starting");
                                if (mMediaState == StringeeCall2.MediaState.CONNECTED) {
                                    startCall();
                                }
                                break;
                            case ENDED:
                                tvState.setText("Ended");
                                endCall(true, false);
                                break;
                        }
                    }
                });
            }

            @Override
            public void onError(StringeeCall2 stringeeCall2, int i, String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.reportMessage(IncomingCall2Activity.this, s);
                    }
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall2 stringeeCall2, final StringeeCall2.SignalingState signalingState, String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (signalingState == StringeeCall2.SignalingState.ANSWERED || signalingState == StringeeCall2.SignalingState.BUSY) {
                            Utils.reportMessage(IncomingCall2Activity.this, "This call is handled on another device.");
                            tvState.setText("Ended");
                            endCall(false, false);
                        }
                    }
                });
            }

            @Override
            public void onMediaStateChange(StringeeCall2 stringeeCall2, final StringeeCall2.MediaState mediaState) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMediaState = mediaState;
                        if (mediaState == StringeeCall2.MediaState.CONNECTED) {
                            if (mSignalingState == StringeeCall2.SignalingState.ANSWERED) {
                                startCall();
                            }
                        } else {
                            tvState.setText("Reconnecting...");
                        }
                    }
                });
            }

            @Override
            public void onLocalStream(final StringeeCall2 stringeeCall2) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (stringeeCall2.isVideoCall()) {
                            mLocalViewContainer.removeAllViews();
                            SurfaceView localView = stringeeCall2.getLocalView();
                            mLocalViewContainer.addView(localView);
                            stringeeCall2.renderLocalView(true);
                        }
                    }
                });
            }

            @Override
            public void onRemoteStream(final StringeeCall2 stringeeCall2) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (stringeeCall2.isVideoCall()) {
                            mRemoteViewContainer.removeAllViews();
                            SurfaceView remoteView = stringeeCall2.getRemoteView();
                            mRemoteViewContainer.addView(remoteView);
                            stringeeCall2.renderRemoteView(false);
                        }
                    }
                });
            }

            @Override
            public void onCallInfo(StringeeCall2 stringeeCall2, final JSONObject jsonObject) {

            }
        });
        mStringeeCall2.ringing(new StatusListener() {
            @Override
            public void onSuccess() {
                Log.d("Stringee", "send ringing success");
            }
        });

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_mute:
                isMute = !isMute;
                btnMute.setBackgroundResource(isMute ? R.drawable.btn_mute : R.drawable.btn_mic);
                if (mStringeeCall2 != null) {
                    mStringeeCall2.mute(isMute);
                }
                break;
            case R.id.btn_speaker:
                isSpeaker = !isSpeaker;
                btnSpeaker.setBackgroundResource(isSpeaker ? R.drawable.btn_speaker_on : R.drawable.btn_speaker_off);
                if (mSignalingState == SignalingState.ANSWERED || mMediaState == MediaState.CONNECTED) {
                    if (audioManager != null) {
                        audioManager.setSpeakerphoneOn(isSpeaker);
                    }
                }
                break;
            case R.id.btn_answer:
                if (mStringeeCall2 != null) {
                    vControl.setVisibility(View.VISIBLE);
                    btnAnswer.setVisibility(View.GONE);
                    mStringeeCall2.answer();
                }
                break;
            case R.id.btn_end:
                tvState.setText("Ended");
                endCall(true, false);
                break;
            case R.id.btn_video:
                isVideo = !isVideo;
                btnVideo.setImageResource(isVideo ? R.drawable.btn_video : R.drawable.btn_video_off);
                if (mStringeeCall2 != null) {
                    mStringeeCall2.enableVideo(isVideo);
                }
                break;
            case R.id.btn_switch:
                if (mStringeeCall2 != null) {
                    mStringeeCall2.switchCamera(new StatusListener() {
                        @Override
                        public void onSuccess() {

                        }
                    });
                }
                break;
        }
    }

    private void startCall() {
        tvState.setText("Started");
    }

    private void endCall(boolean isHangup, boolean isReject) {
        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }

        if (isHangup) {
            if (mStringeeCall2 != null) {
                mStringeeCall2.hangup();
            }
        }

        if (isReject) {
            if (mStringeeCall2 != null) {
                mStringeeCall2.reject();
            }
        }

        Utils.postDelay(new Runnable() {
            @Override
            public void run() {
                Common.isInCall = false;
                finish();
            }
        }, 1000);
    }
}
