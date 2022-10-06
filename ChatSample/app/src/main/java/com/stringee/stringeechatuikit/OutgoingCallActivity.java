package com.stringee.stringeechatuikit;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog.Builder;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import com.stringee.call.StringeeCall;
import com.stringee.listener.StatusListener;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
    private ImageButton btnVideo;
    private ImageButton btnSwitch;

    private StringeeCall mStringeeCall;
    private String from;
    private String to;
    private boolean isVideoCall;
    private boolean isMute = false;
    private boolean isSpeaker = false;
    private boolean isVideo = false;

    private StringeeCall.MediaState mMediaState;
    private StringeeCall.SignalingState mSignalingState;

    public static final int REQUEST_PERMISSION_CALL = 1;
    public static final int REQUEST_PERMISSION_CAMERA = 2;
    public static final int REQUEST_PERMISSION_CAMERA_WHEN_ANSWER = 3;

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
        btnVideo = (ImageButton) findViewById(R.id.btn_video);
        btnVideo.setOnClickListener(this);
        btnSwitch = (ImageButton) findViewById(R.id.btn_switch);
        btnSwitch.setOnClickListener(this);

        isSpeaker = isVideoCall;
        if (isSpeaker) {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_on);
        } else {
            btnSpeaker.setImageResource(R.drawable.ic_speaker_off);
        }

        isVideo = isVideoCall;
        if (isVideo) {
            btnVideo.setImageResource(R.drawable.ic_video);
        } else {
            btnVideo.setImageResource(R.drawable.ic_video_off);
        }

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
            case REQUEST_PERMISSION_CAMERA:
                if (isGranted) {
                    enableOrDisableVideo();
                }
                break;
            case REQUEST_PERMISSION_CAMERA_WHEN_ANSWER:
                if (isGranted) {
                    acceptCameraRequest();
                }
                break;
        }
    }

    private void makeCall() {
        mStringeeCall = new StringeeCall(Common.client, from, to);
        mStringeeCall.setVideoCall(isVideoCall);

        mStringeeCall.setCallListener(new StringeeCall.StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall stringeeCall, final StringeeCall.SignalingState signalingState, String s, int i, String s1) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSignalingState = signalingState;
                        if (signalingState == StringeeCall.SignalingState.CALLING) {
                            tvState.setText("Outgoing call");
                        } else if (signalingState == StringeeCall.SignalingState.RINGING) {
                            tvState.setText("Ringing");
                        } else if (signalingState == StringeeCall.SignalingState.ANSWERED) {
                            tvState.setText("Starting");
                            if (mMediaState == StringeeCall.MediaState.CONNECTED) {
                                tvState.setText("Started");
                            }
                        } else if (signalingState == StringeeCall.SignalingState.BUSY) {
                            tvState.setText("Busy");
                            mStringeeCall.hangup(new StatusListener() {
                                @Override
                                public void onSuccess() {

                                }
                            });
                            finish();
                        } else if (signalingState == StringeeCall.SignalingState.ENDED) {
                            tvState.setText("Ended");
                            mStringeeCall.hangup(new StatusListener() {
                                @Override
                                public void onSuccess() {

                                }
                            });
                            finish();
                        }
                    }
                });
            }

            @Override
            public void onError(StringeeCall stringeeCall, int i, String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Utils.reportMessage(OutgoingCallActivity.this, "Fails to make call.");
                    }
                });
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState, String s) {

            }

            @Override
            public void onMediaStateChange(StringeeCall stringeeCall, final StringeeCall.MediaState mediaState) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mMediaState = mediaState;
                        if (mediaState == StringeeCall.MediaState.CONNECTED) {
                            if (mSignalingState == StringeeCall.SignalingState.ANSWERED) {
                                tvState.setText("Started");
                            }
                        }
                    }
                });
            }

            @Override
            public void onLocalStream(final StringeeCall stringeeCall) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (stringeeCall.isVideoCall()) {
                            mLocalViewContainer.addView(stringeeCall.getLocalView());
                            stringeeCall.renderLocalView(true);
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
                            stringeeCall.renderRemoteView(false);
                        }
                    }
                });
            }

            @Override
            public void onCallInfo(StringeeCall stringeeCall, final JSONObject jsonObject) {
                try {
                    String type = jsonObject.getString("type");
                    if (type.equals("cameraRequest")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Builder builder = new Builder(OutgoingCallActivity.this);
                                builder.setMessage("You have a camera request. Do you want to accept?");
                                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        dialogInterface.dismiss();
                                        JSONObject answerObject = new JSONObject();
                                        try {
                                            answerObject.put("type", "answerCameraRequest");
                                            answerObject.put("accept", false);
                                            mStringeeCall.sendCallInfo(answerObject,new StatusListener() {
                                                @Override
                                                public void onSuccess() {

                                                }
                                            });

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        JSONObject answerObject = new JSONObject();
                                        try {
                                            answerObject.put("type", "answerCameraRequest");
                                            answerObject.put("accept", true);
                                            mStringeeCall.sendCallInfo(answerObject,new StatusListener() {
                                                @Override
                                                public void onSuccess() {

                                                }
                                            });

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                            if (ContextCompat.checkSelfPermission(OutgoingCallActivity.this,
                                                    Manifest.permission.CAMERA)
                                                    != PackageManager.PERMISSION_GRANTED) {
                                                String[] permissions = {Manifest.permission.CAMERA};
                                                ActivityCompat.requestPermissions(OutgoingCallActivity.this, permissions, REQUEST_PERMISSION_CAMERA_WHEN_ANSWER);
                                                return;
                                            }
                                        }
                                        acceptCameraRequest();

                                    }
                                });
                                androidx.appcompat.app.AlertDialog dialog = builder.create();
                                dialog.show();
                            }
                        });
                    } else if (type.equals("answerCameraRequest")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                boolean accept = false;
                                try {
                                    accept = jsonObject.getBoolean("accept");
                                    if (accept) {
                                        Utils.reportMessage(OutgoingCallActivity.this, "Your camera request is accepted.");
                                    } else {
                                        Utils.reportMessage(OutgoingCallActivity.this, "Your camera request is rejected.");
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        mStringeeCall.makeCall(new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
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
//                if (mStringeeCall != null) {
//                    mStringeeCall.setSpeakerphoneOn(isSpeaker);
//                }
                break;
            case R.id.btn_end:
                mStringeeCall.hangup(new StatusListener() {
                    @Override
                    public void onSuccess() {

                    }
                });
                finish();
                break;
            case R.id.btn_video:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        String[] permissions = {Manifest.permission.CAMERA};
                        ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSION_CAMERA);
                        return;
                    }
                }
                enableOrDisableVideo();
                break;
            case R.id.btn_switch:
                if (mStringeeCall != null) {
                    mStringeeCall.switchCamera(null);
                }
                break;
        }
    }

    private void enableOrDisableVideo() {
        isVideo = !isVideo;
        if (isVideo) {
            btnVideo.setImageResource(R.drawable.ic_video);
        } else {
            btnVideo.setImageResource(R.drawable.ic_video_off);
        }
        if (mStringeeCall != null) {
            if (!mStringeeCall.isVideoCall()) { // Send camera request
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("type", "cameraRequest");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mStringeeCall.sendCallInfo(jsonObject,new StatusListener() {
                    @Override
                    public void onSuccess() {

                    }
                });
            }
            mStringeeCall.enableVideo(isVideo);
        }
    }

    private void acceptCameraRequest() {
        isVideo = true;
        btnVideo.setImageResource(R.drawable.ic_video);
        mStringeeCall.enableVideo(true);
    }
}
