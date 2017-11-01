package com.stringee.apptoappcallsample;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.stringee.apptoappcallsample.utils.StringeeAudioManager;
import com.stringee.apptoappcallsample.utils.Utils;
import com.stringee.call.StringeeCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by luannguyen on 10/26/2017.
 */

public class OutgoingCallActivity extends AppCompatActivity implements View.OnClickListener {

    private FrameLayout mLocalViewContainer;
    private FrameLayout mRemoteViewContainer;
    private TextView tvTo;
    private TextView tvState;
    private ImageButton btnMute;
    private ImageButton btnSpeaker;

    private StringeeCall mStringeeCall;
    private String from;
    private String to;
    private boolean isVideoCall;
    private boolean isMute = false;
    private boolean isSpeaker = false;

    public static final int REQUEST_PERMISSION_CALL = 1;

    public StringeeAudioManager audioManager;
    public AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    audioManager.setAudioDeviceInternal(audioManager.getSelectedAudioDevice());
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    audioManager.setAudioDeviceInternal(audioManager.getSelectedAudioDevice());
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
                    audioManager.setAudioDeviceInternal(audioManager.getSelectedAudioDevice());
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                    audioManager.setAudioDeviceInternal(audioManager.getSelectedAudioDevice());
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    break;
                default:
                    break;
            }
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_call);

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

        ImageButton btnEnd = (ImageButton) findViewById(R.id.btn_end);
        btnEnd.setOnClickListener(this);

        if (audioManager == null) {
            audioManager = StringeeAudioManager.create(getApplicationContext(), isVideoCall);
            audioManager.start(new StringeeAudioManager.AudioManagerEvents() {
                @Override
                public void onAudioDeviceChanged(
                        StringeeAudioManager.AudioDevice audioDevice, Set<StringeeAudioManager.AudioDevice> availableAudioDevices) {
                }
            }, mOnAudioFocusChangeListener);
        }

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

        makeCall();
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
        switch (requestCode) {
            case REQUEST_PERMISSION_CALL:
                if (!isGranted) {
                    finish();
                } else {
                    makeCall();
                }
                break;
        }
    }

    private void makeCall() {
        mStringeeCall = new StringeeCall(this, MainActivity.client, from, to);
        mStringeeCall.setVideoCall(isVideoCall);

        mStringeeCall.setStateListener(new StringeeCall.StringeeCallStateListener() {
            @Override
            public void onStateChange(StringeeCall stringeeCall, final StringeeCall.CallState state, String description) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (state == StringeeCall.CallState.CONNECTING) {
                            tvState.setText("Outgoing call");
                        } else if (state == StringeeCall.CallState.RINGING) {
                            tvState.setText("Ringing");
                        } else if (state == StringeeCall.CallState.STARTED) {
                            tvState.setText("Started");
                        } else if (state == StringeeCall.CallState.BUSY) {
                            tvState.setText("Busy");
                            mStringeeCall.endCallAndReleaseResource();
                            finish();
                        } else if (state == StringeeCall.CallState.END) {
                            tvState.setText("Ended");
                            mStringeeCall.endCallAndReleaseResource();
                            finish();
                        }
                    }
                });
            }

            @Override
            public void onError(StringeeCall stringeeCall, int code, String description) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.reportMessage(OutgoingCallActivity.this, "Fails to make call.");
                    }
                });
            }

            @Override
            public void onDTMFComplete(String s, int requestId, int result) {

            }
        });

        mStringeeCall.setMediaListener(new StringeeCall.StringeeCallMediaListener() {
            @Override
            public void onLocalStream(final StringeeCall stringeeCall) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (stringeeCall.isVideoCall()) {
                            mLocalViewContainer.addView(stringeeCall.getLocalView());
                            stringeeCall.renderLocalView();
                        }
                    }
                });
            }

            @Override
            public void onRemoteStream(final StringeeCall stringeeCall) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (stringeeCall.isVideoCall()) {
                            mRemoteViewContainer.addView(stringeeCall.getRemoteView());
                            stringeeCall.renderRemoteView();
                        }
                    }
                });
            }
        });

        mStringeeCall.makeCall();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_mute:
                isMute = !isMute;
                if (isMute) {
                    btnMute.setImageResource(R.drawable.ic_mute);
                } else {
                    btnMute.setImageResource(R.drawable.ic_mic);
                }
                if (mStringeeCall != null) {
                    mStringeeCall.mute(isMute);
                }
                break;
            case R.id.btn_speaker:
                isSpeaker = !isSpeaker;
                if (isSpeaker) {
                    btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
                } else {
                    btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
                }
                if (audioManager != null) {
                    audioManager.setSpeakerphoneOn(isSpeaker);
                }
                break;
            case R.id.btn_end:
                if (mStringeeCall != null) {
                    mStringeeCall.endCallAndReleaseResource();
                }
                finish();
                break;
        }
    }
}
