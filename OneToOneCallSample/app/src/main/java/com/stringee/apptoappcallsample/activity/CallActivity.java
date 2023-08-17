package com.stringee.apptoappcallsample.activity;

import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.stringee.apptoappcallsample.R;
import com.stringee.apptoappcallsample.common.CallStatus;
import com.stringee.apptoappcallsample.common.Constant;
import com.stringee.apptoappcallsample.common.SensorManagerUtils;
import com.stringee.apptoappcallsample.databinding.ActivityVideoCallBinding;
import com.stringee.apptoappcallsample.databinding.ActivityVoiceCallBinding;
import com.stringee.apptoappcallsample.databinding.LayoutIncomingCallBinding;
import com.stringee.apptoappcallsample.listener.OnCallListener;
import com.stringee.apptoappcallsample.manager.CallManager;

public class CallActivity extends AppCompatActivity implements View.OnClickListener {
    private ActivityVideoCallBinding videoCallBinding;
    private ActivityVoiceCallBinding voiceCallBinding;
    private LayoutIncomingCallBinding incomingCallBinding;
    private CallManager callManager;
    private SensorManagerUtils sensorManagerUtils;
    private boolean isVideoCall;
    private boolean isIncomingCall;
    private boolean isStringeeCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        videoCallBinding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        voiceCallBinding = ActivityVoiceCallBinding.inflate(getLayoutInflater());

        isVideoCall = getIntent().getBooleanExtra(Constant.PARAM_IS_VIDEO_CALL, false);
        setContentView(isVideoCall ? videoCallBinding.getRoot() : voiceCallBinding.getRoot());
        incomingCallBinding = isVideoCall ? videoCallBinding.vIncomingCall : voiceCallBinding.vIncomingCall;

        isIncomingCall = getIntent().getBooleanExtra(Constant.PARAM_IS_INCOMING_CALL, false);
        isStringeeCall = getIntent().getBooleanExtra(Constant.PARAM_IS_STRINGEE_CALL, false);

        callManager = CallManager.getInstance(this);
        sensorManagerUtils = SensorManagerUtils.getInstance(this).initialize(getLocalClassName());

        if (!isVideoCall) {
            sensorManagerUtils.turnOn();
        } else {
            sensorManagerUtils.disableKeyguard();
        }

        incomingCallBinding.btnAnswer.setOnClickListener(this);
        incomingCallBinding.btnReject.setOnClickListener(this);
        if (!isVideoCall) {
            voiceCallBinding.btnEnd.setOnClickListener(this);
            voiceCallBinding.btnMute.setOnClickListener(this);
            voiceCallBinding.btnSpeaker.setOnClickListener(this);
        } else {
            videoCallBinding.btnEnd.setOnClickListener(this);
            videoCallBinding.btnMute.setOnClickListener(this);
            videoCallBinding.btnCamera.setOnClickListener(this);
            videoCallBinding.btnSwitch.setOnClickListener(this);
        }

        incomingCallBinding.getRoot().setVisibility(callManager.getCallStatus() != CallStatus.INCOMING ? View.GONE : View.VISIBLE);
        if (isVideoCall) {
            videoCallBinding.vInCall.setVisibility(callManager.getCallStatus() != CallStatus.INCOMING ? View.VISIBLE : View.GONE);
        } else {
            voiceCallBinding.vInCall.setVisibility(callManager.getCallStatus() != CallStatus.INCOMING ? View.VISIBLE : View.GONE);
        }

        initCall();
    }

    @Override
    protected void onPause() {
        super.onPause();
        runOnUiThread(() -> {
            if (callManager.getCallStatus() == CallStatus.STARTED || callManager.getCallStatus() == CallStatus.CALLING || callManager.getCallStatus() == CallStatus.RINGING) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

                sensorManagerUtils = SensorManagerUtils.getInstance(this).initialize(getLocalClassName());
                if (!isVideoCall) {
                    sensorManagerUtils.turnOff();
                } else {
                    sensorManagerUtils.reEnableKeyguard();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(false);
                    setTurnScreenOn(false);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        runOnUiThread(() -> {
            if (callManager.getCallStatus() == CallStatus.STARTED || callManager.getCallStatus() == CallStatus.CALLING || callManager.getCallStatus() == CallStatus.RINGING) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

                sensorManagerUtils = SensorManagerUtils.getInstance(this).initialize(getLocalClassName());
                if (!isVideoCall) {
                    sensorManagerUtils.turnOn();
                } else {
                    sensorManagerUtils.disableKeyguard();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true);
                    setTurnScreenOn(true);
                }
            }
        });
    }

    private void initCall() {
        callManager.registerEvent(new OnCallListener() {
            @Override
            public void onCallStatus(CallStatus status) {
                runOnUiThread(() -> {
                    if (!isVideoCall) {
                        voiceCallBinding.tvStatus.setText(status.getValue());
                    }
                    incomingCallBinding.getRoot().setVisibility(status != CallStatus.INCOMING ? View.GONE : View.VISIBLE);
                    if (isVideoCall) {
                        videoCallBinding.vInCall.setVisibility(status != CallStatus.INCOMING ? View.VISIBLE : View.GONE);
                    } else {
                        voiceCallBinding.vInCall.setVisibility(status != CallStatus.INCOMING ? View.VISIBLE : View.GONE);
                    }
                    if (status == CallStatus.ENDED || status == CallStatus.BUSY) {
                        dismiss();
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> dismiss());
            }

            @Override
            public void onReceiveLocalStream() {
                runOnUiThread(() -> {
                    if (isVideoCall) {
                        FrameLayout.LayoutParams childParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                        childParams.gravity = Gravity.CENTER;

                        videoCallBinding.vLocal.removeAllViews();
                        videoCallBinding.vLocal.addView(callManager.getLocalView(), childParams);
                        callManager.renderLocalView();
                    }
                });
            }

            @Override
            public void onReceiveRemoteStream() {
                runOnUiThread(() -> {
                    if (isVideoCall) {
                        FrameLayout.LayoutParams childParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                        childParams.gravity = Gravity.CENTER;

                        videoCallBinding.vRemote.removeAllViews();
                        videoCallBinding.vRemote.addView(callManager.getRemoteView(), childParams);
                        callManager.renderRemoteView();
                    }
                });
            }

            @Override
            public void onSpeakerChange(boolean isOn) {
                runOnUiThread(() -> {
                    voiceCallBinding.btnSpeaker.setBackgroundResource(isOn ? R.drawable.btn_ic_selector : R.drawable.btn_ic_selected_selector);
                    voiceCallBinding.btnSpeaker.setImageResource(isOn ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
                });
            }

            @Override
            public void onMicChange(boolean isOn) {
                runOnUiThread(() -> {
                    if (isVideoCall) {
                        videoCallBinding.btnMute.setBackgroundResource(!isOn ? R.drawable.btn_ic_selector : R.drawable.btn_ic_selected_selector);
                        videoCallBinding.btnMute.setImageResource(!isOn ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);
                    } else {
                        voiceCallBinding.btnMute.setBackgroundResource(!isOn ? R.drawable.btn_ic_selector : R.drawable.btn_ic_selected_selector);
                        voiceCallBinding.btnMute.setImageResource(!isOn ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);
                    }
                });
            }

            @Override
            public void onVideoChange(boolean isOn) {
                runOnUiThread(() -> {
                    videoCallBinding.btnCamera.setBackgroundResource(isOn ? R.drawable.btn_ic_selected_selector : R.drawable.btn_ic_selector);
                    videoCallBinding.btnCamera.setImageResource(isOn ? R.drawable.ic_cam_on : R.drawable.ic_cam_off);
                });
            }

            @Override
            public void onTimer(String duration) {
                runOnUiThread(() -> {
                    if (!isVideoCall) {
                        voiceCallBinding.tvTime.setText(duration);
                    }
                });
            }
        });
        if (!isIncomingCall) {
            String to = getIntent().getStringExtra(Constant.PARAM_TO);
            callManager.initializedOutgoingCall(to, isVideoCall, isStringeeCall);
            callManager.makeCall();
        } else {
            incomingCallBinding.tvUser.setText(callManager.getFrom());
            boolean isAnswerFromPush = getIntent().getBooleanExtra(Constant.PARAM_ACTION_ANSWER_FROM_PUSH, false);
            if (isAnswerFromPush){
                callManager.answer();
            }
        }
    }

    @Override
    public void onClick(View view) {
        int vId = view.getId();
        if (vId == R.id.btn_answer) {
            callManager.answer();
        } else if (vId == R.id.btn_reject) {
            callManager.endCall(false);
        } else if (vId == R.id.btn_end) {
            callManager.endCall(true);
        } else if (vId == R.id.btn_mute) {
            callManager.mute();
        } else if (vId == R.id.btn_speaker) {
            callManager.changeSpeaker();
        } else if (vId == R.id.btn_camera) {
            callManager.enableVideo();
        } else if (vId == R.id.btn_switch) {
            callManager.switchCamera();
        }
    }

    private void dismiss() {
        sensorManagerUtils.releaseSensor();
        callManager.release();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        finish();
    }
}