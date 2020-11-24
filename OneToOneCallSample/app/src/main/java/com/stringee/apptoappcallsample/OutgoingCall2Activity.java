package com.stringee.apptoappcallsample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.stringee.apptoappcallsample.common.Common;
import com.stringee.apptoappcallsample.common.StringeeAudioManager;
import com.stringee.apptoappcallsample.common.Utils;
import com.stringee.call.StringeeCall2;
import com.stringee.listener.StatusListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OutgoingCall2Activity extends AppCompatActivity implements View.OnClickListener {

    private FrameLayout mLocalViewContainer;
    private FrameLayout mRemoteViewContainer;
    private TextView tvTo;
    private TextView tvState;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;
    private ImageButton btnVideo;
    private ImageButton btnSwitch;

    private StringeeCall2 mStringeeCall2;
    private String from;
    private String to;
    private boolean isVideoCall;
    private boolean isMute = false;
    private boolean isSpeaker = false;
    private boolean isVideo = false;

    private StringeeCall2.MediaState mMediaState;
    private StringeeCall2.SignalingState mSignalingState;

    public static final int REQUEST_PERMISSION_CALL = 1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

        Common.isInCall = true;

        from = getIntent().getStringExtra("from");
        to = getIntent().getStringExtra("to");
        isVideoCall = getIntent().getBooleanExtra("is_video_call", false);


        mLocalViewContainer = (FrameLayout) findViewById(R.id.v_local);
        mRemoteViewContainer = (FrameLayout) findViewById(R.id.v_remote);

        tvTo = (TextView) findViewById(R.id.tv_to);
        tvTo.setText(to);

        tvState = (TextView) findViewById(R.id.tv_state);

        btnMute = (ImageButton) findViewById(R.id.btn_mute);
        btnMute.setOnClickListener(this);
        btnSpeaker = (ImageButton) findViewById(R.id.btn_speaker);
        btnSpeaker.setOnClickListener(this);
        btnVideo = (ImageButton) findViewById(R.id.btn_video);
        btnVideo.setOnClickListener(this);
        btnSwitch = (ImageButton) findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(this);

        isSpeaker = isVideoCall;
        btnSpeaker.setBackgroundResource(isSpeaker ? R.drawable.btn_speaker_on : R.drawable.btn_speaker_off);

        isVideo = isVideoCall;
        btnVideo.setImageResource(isVideo ? R.drawable.btn_video : R.drawable.btn_video_off);

        btnVideo.setVisibility(isVideo ? View.VISIBLE : View.GONE);
        btnSwitch.setVisibility(isVideo ? View.VISIBLE : View.GONE);

        ImageButton btnEnd = (ImageButton) findViewById(R.id.btn_end);
        btnEnd.setOnClickListener(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> lstPermissions = new ArrayList<>();

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                lstPermissions.add(Manifest.permission.RECORD_AUDIO);
            }

            if (isVideoCall) {
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
        Common.audioManager = StringeeAudioManager.create(OutgoingCall2Activity.this);
        Common.audioManager.start(new StringeeAudioManager.AudioManagerEvents() {
            @Override
            public void onAudioDeviceChanged(StringeeAudioManager.AudioDevice selectedAudioDevice, Set<StringeeAudioManager.AudioDevice> availableAudioDevices) {
                Log.d("StringeeAudioManager", "onAudioManagerDevicesChanged: " + availableAudioDevices + ", "
                        + "selected: " + selectedAudioDevice);
            }
        });
        Common.audioManager.setSpeakerphoneOn(isVideoCall);

        makeCall();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        endCall();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
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
        if (requestCode == REQUEST_PERMISSION_CALL) {
            if (!isGranted) {
                finish();
            } else {
                makeCall();
            }
        }
    }

    private void makeCall() {
        mStringeeCall2 = new StringeeCall2(MainActivity.client, from, to);
        mStringeeCall2.setVideoCall(isVideoCall);

        mStringeeCall2.setCallListener(new StringeeCall2.StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall2 stringeeCall2, final StringeeCall2.SignalingState signalingState, String s, int i, String s1) {
                Log.e("Stringee", "======== Custom data: " + stringeeCall2.getCustomDataFromYourServer());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
                                if (mMediaState == StringeeCall2.MediaState.CONNECTED) {
                                    tvState.setText("Started");
                                    Common.audioManager.setSpeakerphoneOn(isVideoCall);
                                }
                                break;
                            case BUSY:
                                tvState.setText("Busy");
                                endCall();
                            case ENDED:
                                tvState.setText("Ended");
                                endCall();
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
                        Utils.reportMessage(OutgoingCall2Activity.this, s);
                    }
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall2 stringeeCall2, StringeeCall2.SignalingState signalingState, String s) {

            }

            @Override
            public void onMediaStateChange(StringeeCall2 stringeeCall2, final StringeeCall2.MediaState mediaState) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMediaState = mediaState;
                        if (mediaState == StringeeCall2.MediaState.CONNECTED) {
                            if (mSignalingState == StringeeCall2.SignalingState.ANSWERED) {
                                tvState.setText("Started");
                            }
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
                            mLocalViewContainer.addView(stringeeCall2.getLocalView());
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
                            mRemoteViewContainer.addView(stringeeCall2.getRemoteView());
                            stringeeCall2.renderRemoteView(false);
                        }
                    }
                });
            }

            @Override
            public void onCallInfo(StringeeCall2 stringeeCall2, final JSONObject jsonObject) {
            }
        });

        mStringeeCall2.makeCall();
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
                if (Common.audioManager != null) {
                    Common.audioManager.setSpeakerphoneOn(isSpeaker);
                }
                break;
            case R.id.btn_end:
                endCall();
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

    private void endCall() {
        if (Common.audioManager != null) {
            Common.audioManager.stop();
            Common.audioManager = null;
        }

        mStringeeCall2.hangup();
        Utils.postDelay(new Runnable() {
            @Override
            public void run() {
                Common.isInCall = false;
                finish();
            }
        }, 1000);
    }
}
