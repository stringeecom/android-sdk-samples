package com.stringee.apptoappcallsample.stringee.activity;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.stringee.apptoappcallsample.R;
import com.stringee.apptoappcallsample.databinding.ActivityVideoCallBinding;
import com.stringee.apptoappcallsample.databinding.ActivityVoiceCallBinding;
import com.stringee.apptoappcallsample.databinding.LayoutIncomingCallBinding;
import com.stringee.apptoappcallsample.stringee.common.CallConstants;
import com.stringee.apptoappcallsample.stringee.common.CallEngine;
import com.stringee.apptoappcallsample.stringee.common.CallPermissions;
import com.stringee.apptoappcallsample.stringee.common.CallStatus;
import com.stringee.apptoappcallsample.stringee.listener.CallUiListener;
import com.stringee.apptoappcallsample.stringee.manager.CallSession;
import com.stringee.apptoappcallsample.stringee.manager.StringeeCallManager;
import com.stringee.apptoappcallsample.stringee.service.MediaProjectionCallService;
import com.stringee.common.StringeeAudioManager.AudioDevice;
import com.stringee.video.StringeeVideoTrack;

import org.webrtc.RendererCommon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Permission-aware in-call UI for voice, video, and Call2 screen sharing. */
public class CallActivity extends AppCompatActivity implements View.OnClickListener,
        CallUiListener {
    private ActivityVideoCallBinding videoBinding;
    private ActivityVoiceCallBinding voiceBinding;
    private LayoutIncomingCallBinding incomingBinding;
    private CallSession session;
    private boolean videoCall;
    private boolean permissionFlowCompleted;
    private boolean answerRequested;
    private ActivityResultLauncher<Intent> mediaProjectionLauncher;
    private final List<StringeeVideoTrack> screenTracks = new ArrayList<>();
    private StringeeVideoTrack displayedScreenTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StringeeCallManager manager = StringeeCallManager.getInstance(this);
        session = manager.getSession();
        long requestedGeneration = getIntent().getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1);
        if (session == null || (requestedGeneration >= 0
                && !manager.ownsSessionGeneration(requestedGeneration))) {
            finish();
            return;
        }
        answerRequested = getIntent().getBooleanExtra(CallConstants.EXTRA_ANSWER, false);
        videoCall = session.isVideoCall();
        videoBinding = ActivityVideoCallBinding.inflate(getLayoutInflater());
        voiceBinding = ActivityVoiceCallBinding.inflate(getLayoutInflater());
        setContentView(videoCall ? videoBinding.getRoot() : voiceBinding.getRoot());
        incomingBinding = videoCall ? videoBinding.vIncomingCall : voiceBinding.vIncomingCall;

        configureWindow();
        bindActions();
        configureInitialUi();
        mediaProjectionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Intent serviceIntent = new Intent(this,
                                MediaProjectionCallService.class)
                                .setAction(CallConstants.ACTION_START_IN_CALL)
                                .putExtra(MediaProjectionCallService.EXTRA_PERMISSION_DATA,
                                        result.getData());
                        ContextCompat.startForegroundService(this, serviceIntent);
                    }
                });
        session.registerUiListener(this);
        ensureCallPermissions();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        long requestedGeneration = intent.getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1);
        if (requestedGeneration >= 0 && !StringeeCallManager.getInstance(this)
                .ownsSessionGeneration(requestedGeneration)) {
            return;
        }
        if (intent.getBooleanExtra(CallConstants.EXTRA_ANSWER, false)) {
            answerRequested = true;
            startCallAfterPermissions();
        }
    }

    private void configureWindow() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
            }
        });
    }

    private void bindActions() {
        incomingBinding.btnAnswer.setOnClickListener(this);
        incomingBinding.btnReject.setOnClickListener(this);
        if (videoCall) {
            videoBinding.btnEnd.setOnClickListener(this);
            videoBinding.btnMute.setOnClickListener(this);
            videoBinding.btnCamera.setOnClickListener(this);
            videoBinding.btnSwitch.setOnClickListener(this);
            videoBinding.btnShare.setOnClickListener(this);
        } else {
            voiceBinding.btnEnd.setOnClickListener(this);
            voiceBinding.btnMute.setOnClickListener(this);
            voiceBinding.btnSpeaker.setOnClickListener(this);
        }
    }

    private void configureInitialUi() {
        boolean incoming = session.getStatus() == CallStatus.INCOMING;
        incomingBinding.getRoot().setVisibility(incoming ? View.VISIBLE : View.GONE);
        if (videoCall) {
            videoBinding.vInCall.setVisibility(incoming ? View.GONE : View.VISIBLE);
            videoBinding.vShareBtn.setVisibility(
                    session.getEngine() == CallEngine.STRINGEE_CALL2 ? View.VISIBLE : View.GONE);
        } else {
            voiceBinding.vInCall.setVisibility(incoming ? View.GONE : View.VISIBLE);
            voiceBinding.tvUser1.setText(session.getFrom());
        }
        incomingBinding.tvUser.setText(session.getFrom());
    }

    private void ensureCallPermissions() {
        if (CallPermissions.hasCallPermissions(this, videoCall)) {
            permissionFlowCompleted = true;
            startCallAfterPermissions();
        } else {
            CallPermissions.requestCallPermissions(this, videoCall);
        }
    }

    private void startCallAfterPermissions() {
        if (!permissionFlowCompleted || session == null) {
            return;
        }
        if (session.isIncoming()) {
            if (answerRequested) {
                answerRequested = false;
                session.answer();
            }
        } else {
            StringeeCallManager.getInstance(this).startPreparedOutgoing();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CallPermissions.REQUEST_CALL_PERMISSIONS) {
            return;
        }
        permissionFlowCompleted = CallPermissions.verify(grantResults);
        if (permissionFlowCompleted) {
            startCallAfterPermissions();
        } else if (session != null) {
            if (session.isIncoming()) {
                session.reject();
            } else {
                session.release();
            }
            finishCallActivity();
        }
    }

    @Override
    public void onClick(View view) {
        if (session == null) {
            return;
        }
        int id = view.getId();
        if (id == R.id.btn_answer) {
            answerRequested = true;
            startCallAfterPermissions();
        } else if (id == R.id.btn_reject) {
            session.reject();
        } else if (id == R.id.btn_end) {
            session.hangUp();
        } else if (id == R.id.btn_mute) {
            session.toggleMute();
        } else if (id == R.id.btn_speaker) {
            session.toggleSpeaker();
        } else if (id == R.id.btn_camera) {
            session.toggleVideo();
        } else if (id == R.id.btn_switch) {
            session.switchCamera();
        } else if (id == R.id.btn_share) {
            if (session.isSharing()) {
                session.stopScreenShare();
            } else {
                session.prepareScreenShare(this, mediaProjectionLauncher,
                        getSystemService(MediaProjectionManager.class));
            }
        }
    }

    @Override
    public void onCallStatus(CallStatus status) {
        runOnUiThread(() -> {
            if (!videoCall) {
                voiceBinding.tvStatus.setText(status.getValue());
            }
            boolean incoming = status == CallStatus.INCOMING;
            incomingBinding.getRoot().setVisibility(incoming ? View.VISIBLE : View.GONE);
            if (videoCall) {
                videoBinding.vInCall.setVisibility(incoming ? View.GONE : View.VISIBLE);
            } else {
                voiceBinding.vInCall.setVisibility(incoming ? View.GONE : View.VISIBLE);
            }
            if (status == CallStatus.ENDED || status == CallStatus.BUSY) {
                finishCallActivity();
            }
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(this::finishCallActivity);
    }

    @Override
    public void onLocalVideoAvailable() {
        runOnUiThread(() -> renderView(videoBinding.vLocal, session.getLocalView(), true));
    }

    @Override
    public void onRemoteVideoAvailable() {
        runOnUiThread(() -> {
            if (displayedScreenTrack == null) {
                renderView(videoBinding.vRemote, session.getRemoteView(), false);
            }
        });
    }

    private void renderView(FrameLayout parent, View child, boolean local) {
        if (!videoCall || child == null || session == null) {
            return;
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        parent.removeAllViews();
        parent.addView(child, params);
        if (local) {
            session.renderLocalView();
        } else {
            session.renderRemoteView();
        }
    }

    @Override
    public void onAudioDeviceChanged(AudioDevice selected, Set<AudioDevice> available) {
        if (videoCall) {
            return;
        }
        runOnUiThread(() -> {
            boolean speaker = selected == AudioDevice.SPEAKER_PHONE;
            voiceBinding.btnSpeaker.setBackgroundResource(speaker
                    ? R.drawable.btn_ic_selector : R.drawable.btn_ic_selected_selector);
            voiceBinding.btnSpeaker.setImageResource(speaker
                    ? R.drawable.ic_speaker_on : R.drawable.ic_speaker_off);
        });
    }

    @Override
    public void onMicChanged(boolean enabled) {
        runOnUiThread(() -> {
            if (videoCall) {
                videoBinding.btnMute.setBackgroundResource(enabled
                        ? R.drawable.btn_ic_selected_selector : R.drawable.btn_ic_selector);
                videoBinding.btnMute.setImageResource(enabled
                        ? R.drawable.ic_mic_on : R.drawable.ic_mic_off);
            } else {
                voiceBinding.btnMute.setBackgroundResource(enabled
                        ? R.drawable.btn_ic_selected_selector : R.drawable.btn_ic_selector);
                voiceBinding.btnMute.setImageResource(enabled
                        ? R.drawable.ic_mic_on : R.drawable.ic_mic_off);
            }
        });
    }

    @Override
    public void onVideoChanged(boolean enabled) {
        if (videoCall) {
            runOnUiThread(() -> {
                videoBinding.btnCamera.setBackgroundResource(enabled
                        ? R.drawable.btn_ic_selected_selector : R.drawable.btn_ic_selector);
                videoBinding.btnCamera.setImageResource(enabled
                        ? R.drawable.ic_cam_on : R.drawable.ic_cam_off);
            });
        }
    }

    @Override
    public void onSharingChanged(boolean sharing) {
        if (videoCall) {
            runOnUiThread(() -> {
                videoBinding.btnShare.setBackgroundResource(sharing
                        ? R.drawable.btn_ic_selector : R.drawable.btn_ic_selected_selector);
                videoBinding.btnShare.setImageResource(sharing
                        ? R.drawable.ic_share_off : R.drawable.ic_share);
            });
        }
    }

    @Override
    public void onTimer(String duration) {
        runOnUiThread(() -> {
            if (videoCall) {
                videoBinding.tvTime.setText(duration);
            } else {
                voiceBinding.tvTime.setText(duration);
            }
        });
    }

    @Override
    public void onScreenTrackAdded(StringeeVideoTrack track) {
        runOnUiThread(() -> {
            if (!screenTracks.contains(track)) {
                screenTracks.add(track);
            }
            displayScreenTrack(track);
        });
    }

    @Override
    public void onScreenTrackRemoved(StringeeVideoTrack track) {
        runOnUiThread(() -> {
            screenTracks.removeIf(candidate -> candidate.getId().equals(track.getId()));
            displayedScreenTrack = screenTracks.isEmpty() ? null
                    : screenTracks.get(screenTracks.size() - 1);
            if (displayedScreenTrack == null) {
                renderView(videoBinding.vRemote, session.getRemoteView(), false);
            } else {
                displayScreenTrack(displayedScreenTrack);
            }
        });
    }

    private void displayScreenTrack(StringeeVideoTrack track) {
        if (!videoCall || track == null) {
            return;
        }
        displayedScreenTrack = track;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER;
        videoBinding.vRemote.removeAllViews();
        videoBinding.vRemote.addView(track.getView2(this), params);
        track.renderView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
    }

    private void finishCallActivity() {
        clearVideoViews();
        if (session != null) {
            session.unregisterUiListener(this);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        finish();
    }

    @Override
    protected void onDestroy() {
        clearVideoViews();
        if (session != null) {
            session.unregisterUiListener(this);
        }
        super.onDestroy();
    }

    private void clearVideoViews() {
        if (!videoCall || videoBinding == null) {
            return;
        }
        videoBinding.vLocal.removeAllViews();
        videoBinding.vRemote.removeAllViews();
        screenTracks.clear();
        displayedScreenTrack = null;
    }
}
