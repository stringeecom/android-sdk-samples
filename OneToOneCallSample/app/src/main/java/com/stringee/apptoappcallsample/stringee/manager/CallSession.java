package com.stringee.apptoappcallsample.stringee.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;

import com.stringee.apptoappcallsample.stringee.common.CallAudioManager;
import com.stringee.apptoappcallsample.stringee.common.CallConstants;
import com.stringee.apptoappcallsample.stringee.common.CallEngine;
import com.stringee.apptoappcallsample.stringee.common.CallStatus;
import com.stringee.apptoappcallsample.stringee.common.CallUtils;
import com.stringee.apptoappcallsample.stringee.common.StringeeCallConfig;
import com.stringee.apptoappcallsample.stringee.listener.CallUiListener;
import com.stringee.apptoappcallsample.stringee.service.InCallService;
import com.stringee.apptoappcallsample.stringee.service.IncomingCallService;
import com.stringee.apptoappcallsample.stringee.service.MediaProjectionCallService;
import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.common.StringeeAudioManager.AudioDevice;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.video.StringeeScreenCapture;
import com.stringee.video.StringeeVideoTrack;

import org.json.JSONObject;
import org.webrtc.RendererCommon;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/** Owns one SDK call, its state machine, media, timer, audio, and idempotent cleanup. */
public final class CallSession {
    private static final long OUTGOING_SETUP_TIMEOUT_MS = 60_000L;

    public interface Owner {
        void onSessionStateChanged(CallSession session, CallStatus status);

        void onSessionError(CallSession session, String action, StringeeError error);

        void onSessionReleased(CallSession session);
    }

    private final Context context;
    private final Owner owner;
    private final long generation;
    private final CallAudioManager audioManager;
    private final CallStateMachine stateMachine;
    private final CallSetupStateMachine setupStateMachine = new CallSetupStateMachine();

    private StringeeCall stringeeCall;
    private StringeeCall2 stringeeCall2;
    private String peer = "";
    private String callId = "";
    private CallEngine engine;
    private boolean videoCall;
    private boolean incoming;
    private boolean micEnabled = true;
    private boolean videoEnabled;
    private boolean sharing;
    private boolean switchingCamera;
    private boolean released;
    private boolean outgoingStarted;
    private long startedAt;
    private Timer timer;
    private Timer setupTimeoutTimer;
    private StatusListener outgoingCallback;
    private CallUiListener uiListener;
    private StringeeScreenCapture screenCapture;
    private MediaProjectionCallService mediaProjectionService;
    private final List<StringeeVideoTrack> remoteScreenTracks = new ArrayList<>();

    public CallSession(Context context, Owner owner, long generation, CallStatus initialStatus) {
        this.context = context.getApplicationContext();
        this.owner = owner;
        this.generation = generation;
        stateMachine = new CallStateMachine(initialStatus);
        audioManager = CallAudioManager.getInstance(context);
        audioManager.setListener((selected, available) -> {
            if (isCurrent() && uiListener != null) {
                uiListener.onAudioDeviceChanged(selected, available);
            }
        });
    }

    void prepareOutgoing(StringeeClient client, StringeeCallConfig config) {
        incoming = false;
        peer = config.getTo();
        engine = config.getCallEngine();
        videoCall = config.isVideoCall();
        videoEnabled = videoCall;
        if (engine == CallEngine.STRINGEE_CALL) {
            stringeeCall = new StringeeCall(client, client.getUserId(), config.getTo());
            stringeeCall.setVideoCall(videoCall);
            if (!CallUtils.isEmpty(config.getCustomData())) {
                stringeeCall.setCustom(config.getCustomData());
            }
        } else {
            stringeeCall2 = new StringeeCall2(client, client.getUserId(), config.getTo());
            stringeeCall2.setVideoCall(videoCall);
            if (!CallUtils.isEmpty(config.getCustomData())) {
                stringeeCall2.setCustom(config.getCustomData());
            }
        }
        registerSdkListeners();
    }

    void prepareIncoming(StringeeCall call) {
        incoming = true;
        peer = call.getFrom();
        callId = call.getCallId();
        setupStateMachine.establishIncoming(callId);
        engine = CallEngine.STRINGEE_CALL;
        stringeeCall = call;
        videoCall = call.isVideoCall();
        videoEnabled = videoCall;
        stateMachine.setStatus(CallStatus.INCOMING);
        registerSdkListeners();
    }

    void prepareIncoming(StringeeCall2 call) {
        incoming = true;
        peer = call.getFrom();
        callId = call.getCallId();
        setupStateMachine.establishIncoming(callId);
        engine = CallEngine.STRINGEE_CALL2;
        stringeeCall2 = call;
        videoCall = call.isVideoCall();
        videoEnabled = videoCall;
        stateMachine.setStatus(CallStatus.INCOMING);
        registerSdkListeners();
    }

    private void registerSdkListeners() {
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall.setCallListener(new StringeeCall.StringeeCallListener() {
                @Override
                public void onSignalingStateChange(StringeeCall call,
                                                   StringeeCall.SignalingState state,
                                                   String reason, int sipCode,
                                                   String sipReason) {
                    CallUtils.runOnUiThread(() -> handleSignaling(call, state));
                }

                @Override
                public void onError(StringeeCall call, int code, String description) {
                    CallUtils.runOnUiThread(() -> handleSdkError(call, code, description));
                }

                @Override
                public void onHandledOnAnotherDevice(StringeeCall call,
                                                     StringeeCall.SignalingState state,
                                                     String description) {
                    CallUtils.runOnUiThread(() -> {
                        if (call == stringeeCall && state != StringeeCall.SignalingState.RINGING) {
                            terminal(CallStatus.ENDED);
                        }
                    });
                }

                @Override
                public void onMediaStateChange(StringeeCall call,
                                               StringeeCall.MediaState state) {
                    CallUtils.runOnUiThread(() -> {
                        if (call != stringeeCall || released) {
                            return;
                        }
                        handleMediaConnected(state == StringeeCall.MediaState.CONNECTED);
                    });
                }

                @Override
                public void onLocalStream(StringeeCall call) {
                    CallUtils.runOnUiThread(() -> {
                        if (call == stringeeCall && videoCall && uiListener != null) {
                            uiListener.onLocalVideoAvailable();
                        }
                    });
                }

                @Override
                public void onRemoteStream(StringeeCall call) {
                    CallUtils.runOnUiThread(() -> {
                        if (call == stringeeCall && videoCall && uiListener != null) {
                            uiListener.onRemoteVideoAvailable();
                        }
                    });
                }

                @Override
                public void onCallInfo(StringeeCall call, JSONObject info) {
                    Log.d(CallConstants.TAG, "onCallInfo: " + info);
                }
            });
            return;
        }
        if (stringeeCall2 == null) {
            return;
        }
        stringeeCall2.setCaptureSessionListener(new StringeeVideoTrack.CaptureSessionListener() {
            @Override
            public void onCapturerStarted(StringeeVideoTrack.TrackType trackType) {
                Log.d(CallConstants.TAG, "onCapturerStarted: " + trackType);
            }

            @Override
            public void onCapturerStopped(StringeeVideoTrack.TrackType trackType) {
                CallUtils.runOnUiThread(() -> {
                    if (trackType == StringeeVideoTrack.TrackType.SCREEN && sharing) {
                        finishSharing();
                    }
                });
            }
        });
        stringeeCall2.setCallListener(new StringeeCall2.StringeeCallListener() {
            @Override
            public void onSignalingStateChange(StringeeCall2 call,
                                               StringeeCall2.SignalingState state,
                                               String reason, int sipCode, String sipReason) {
                CallUtils.runOnUiThread(() -> handleSignaling(call, state));
            }

            @Override
            public void onError(StringeeCall2 call, int code, String description) {
                CallUtils.runOnUiThread(() -> handleSdkError(call, code, description));
            }

            @Override
            public void onHandledOnAnotherDevice(StringeeCall2 call,
                                                 StringeeCall2.SignalingState state,
                                                 String description) {
                CallUtils.runOnUiThread(() -> {
                    if (call == stringeeCall2 && state != StringeeCall2.SignalingState.RINGING) {
                        terminal(CallStatus.ENDED);
                    }
                });
            }

            @Override
            public void onMediaStateChange(StringeeCall2 call,
                                           StringeeCall2.MediaState state) {
                CallUtils.runOnUiThread(() -> {
                    if (call != stringeeCall2 || released) {
                        return;
                    }
                    handleMediaConnected(state == StringeeCall2.MediaState.CONNECTED);
                });
            }

            @Override
            public void onLocalTrackAdded(StringeeCall2 call, StringeeVideoTrack track) {
                CallUtils.runOnUiThread(() -> {
                    if (call != stringeeCall2 || released
                            || track.getTrackType() == StringeeVideoTrack.TrackType.SCREEN) {
                        return;
                    }
                    if (videoCall && uiListener != null) {
                        uiListener.onLocalVideoAvailable();
                    }
                });
            }

            @Override
            public void onRemoteTrackAdded(StringeeCall2 call, StringeeVideoTrack track) {
                CallUtils.runOnUiThread(() -> {
                    if (call != stringeeCall2 || released) {
                        return;
                    }
                    if (track.getTrackType() == StringeeVideoTrack.TrackType.SCREEN) {
                        if (!remoteScreenTracks.contains(track)) {
                            remoteScreenTracks.add(track);
                        }
                        if (uiListener != null) {
                            uiListener.onScreenTrackAdded(track);
                        }
                    } else if (videoCall && uiListener != null) {
                        uiListener.onRemoteVideoAvailable();
                    }
                });
            }

            @Override
            public void onRemoteTrackRemoved(StringeeCall2 call, StringeeVideoTrack track) {
                CallUtils.runOnUiThread(() -> {
                    if (call == stringeeCall2 && !released
                            && track.getTrackType() == StringeeVideoTrack.TrackType.SCREEN) {
                        remoteScreenTracks.remove(track);
                        if (uiListener != null) {
                            uiListener.onScreenTrackRemoved(track);
                        }
                    }
                });
            }

            @Override
            public void onCallInfo(StringeeCall2 call, JSONObject info) {
                Log.d(CallConstants.TAG, "onCallInfo2: " + info);
            }

            @Override
            public void onTrackMediaStateChange(String trackId,
                                                StringeeVideoTrack.MediaType mediaType,
                                                boolean enabled) {
            }
        });
    }

    private void handleSignaling(StringeeCall call, StringeeCall.SignalingState state) {
        if (call != stringeeCall || released) {
            return;
        }
        switch (state) {
            case CALLING:
                updateStatus(CallStatus.CALLING);
                break;
            case RINGING:
                updateStatus(CallStatus.RINGING);
                break;
            case ANSWERED:
                updateStatus(stateMachine.onSignalingAnswered());
                break;
            case BUSY:
                terminal(CallStatus.BUSY);
                break;
            case ENDED:
                terminal(CallStatus.ENDED);
                break;
        }
    }

    private void handleSignaling(StringeeCall2 call, StringeeCall2.SignalingState state) {
        if (call != stringeeCall2 || released) {
            return;
        }
        switch (state) {
            case CALLING:
                updateStatus(CallStatus.CALLING);
                break;
            case RINGING:
                updateStatus(CallStatus.RINGING);
                break;
            case ANSWERED:
                updateStatus(stateMachine.onSignalingAnswered());
                break;
            case BUSY:
                terminal(CallStatus.BUSY);
                break;
            case ENDED:
                terminal(CallStatus.ENDED);
                break;
        }
    }

    private void handleMediaConnected(boolean connected) {
        CallStatus status = connected ? stateMachine.onMediaConnected()
                : stateMachine.onMediaDisconnected();
        updateStatus(status);
    }

    private void updateStatus(CallStatus status) {
        if (released) {
            return;
        }
        stateMachine.setStatus(status);
        if (status == CallStatus.STARTED) {
            startTimerIfNeeded();
            InCallService.startOrUpdate(context, generation);
        }
        if (uiListener != null) {
            uiListener.onCallStatus(status);
        }
        owner.onSessionStateChanged(this, status);
    }

    private void handleSdkError(Object call, int code, String description) {
        boolean current = call == stringeeCall || call == stringeeCall2;
        if (!current || released) {
            return;
        }
        StringeeError error = new StringeeError(code, description);
        if (!incoming && setupStateMachine.isPending()) {
            handleMakeCallFailure(error);
            return;
        }
        if (uiListener != null) {
            uiListener.onError(description);
        }
        owner.onSessionError(this, "call", error);
        terminal(CallStatus.ENDED);
    }

    public void registerUiListener(CallUiListener listener) {
        uiListener = listener;
        if (listener != null) {
            listener.onCallStatus(stateMachine.getStatus());
            listener.onMicChanged(micEnabled);
            listener.onVideoChanged(videoEnabled);
            listener.onAudioDeviceChanged(audioManager.getSelectedDevice(),
                    Collections.emptySet());
            if (videoCall && getLocalView() != null) {
                listener.onLocalVideoAvailable();
            }
            if (videoCall && getRemoteView() != null) {
                listener.onRemoteVideoAvailable();
            }
            for (StringeeVideoTrack track : new ArrayList<>(remoteScreenTracks)) {
                listener.onScreenTrackAdded(track);
            }
        }
    }

    public void unregisterUiListener(CallUiListener listener) {
        if (uiListener == listener) {
            uiListener = null;
        }
    }

    void setOutgoingCallback(StatusListener callback) {
        outgoingCallback = callback;
    }

    void startOutgoing() {
        if (outgoingStarted || incoming || released) {
            return;
        }
        outgoingStarted = true;
        InCallService.startOrUpdate(context, generation);
        audioManager.start(videoCall);
        scheduleSetupTimeout();
        StringeeCall outgoingCall = stringeeCall;
        StringeeCall2 outgoingCall2 = stringeeCall2;
        StatusListener result = new StatusListener() {
            @Override
            public void onSuccess() {
                CallUtils.runOnUiThread(() -> handleMakeCallSuccess(
                        outgoingCall, outgoingCall2));
            }

            @Override
            public void onError(StringeeError error) {
                CallUtils.runOnUiThread(() -> handleMakeCallFailure(error));
            }
        };
        try {
            if (engine == CallEngine.STRINGEE_CALL && outgoingCall != null) {
                outgoingCall.makeCall(result);
            } else if (outgoingCall2 != null) {
                outgoingCall2.makeCall(result);
            } else {
                handleMakeCallFailure(new StringeeError(-1, "Call is not prepared"));
            }
        } catch (RuntimeException exception) {
            CallUtils.reportException(CallSession.class, exception);
            handleMakeCallFailure(new StringeeError(-1,
                    exception.getMessage() == null ? "Unable to start call"
                            : exception.getMessage()));
        }
    }

    private void handleMakeCallSuccess(StringeeCall outgoingCall,
                                       StringeeCall2 outgoingCall2) {
        String serverCallId = outgoingCall != null ? outgoingCall.getCallId()
                : outgoingCall2 != null ? outgoingCall2.getCallId() : "";
        CallSetupStateMachine.SuccessResult result = setupStateMachine.onSuccess(serverCallId);
        switch (result) {
            case ESTABLISHED:
                cancelSetupTimeout();
                callId = serverCallId;
                StatusListener callback = takeOutgoingCallback();
                if (callback != null) {
                    callback.onSuccess();
                }
                break;
            case INVALID_SERVER_ID:
                finishOutgoingSetupFailure(new StringeeError(-1,
                        "Stringee returned an empty call ID"));
                break;
            case LATE_SUCCESS:
                hangUpSdkCall(outgoingCall, outgoingCall2,
                        "Hang up after late makeCall success");
                break;
            case IGNORED:
            default:
                break;
        }
    }

    private void handleMakeCallFailure(StringeeError error) {
        if (!setupStateMachine.fail()) {
            return;
        }
        finishOutgoingSetupFailure(error);
    }

    private void finishOutgoingSetupFailure(StringeeError error) {
        cancelSetupTimeout();
        StatusListener callback = takeOutgoingCallback();
        if (callback != null) {
            callback.onError(error);
        }
        if (released) {
            return;
        }
        owner.onSessionError(this, "makeCall", error);
        terminal(CallStatus.ENDED);
    }

    private void scheduleSetupTimeout() {
        cancelSetupTimeout();
        setupTimeoutTimer = new Timer("StringeeCallSetup", true);
        setupTimeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                CallUtils.runOnUiThread(() -> handleMakeCallFailure(
                        new StringeeError(-1, "Call setup timed out")));
            }
        }, OUTGOING_SETUP_TIMEOUT_MS);
    }

    private void cancelSetupTimeout() {
        if (setupTimeoutTimer != null) {
            setupTimeoutTimer.cancel();
            setupTimeoutTimer = null;
        }
    }

    private StatusListener takeOutgoingCallback() {
        StatusListener callback = outgoingCallback;
        outgoingCallback = null;
        return callback;
    }

    private void hangUpSdkCall(StringeeCall outgoingCall, StringeeCall2 outgoingCall2,
                               String reason) {
        try {
            if (outgoingCall != null) {
                outgoingCall.hangup(reason, new StatusListener() {
                    @Override
                    public void onSuccess() {
                    }
                });
            } else if (outgoingCall2 != null) {
                outgoingCall2.hangup(reason, new StatusListener() {
                    @Override
                    public void onSuccess() {
                    }
                });
            }
        } catch (RuntimeException exception) {
            CallUtils.reportException(CallSession.class, exception);
        }
    }

    void ringing() {
        StatusListener listener = terminalOnError("ringing");
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall.ringing(listener);
        } else if (stringeeCall2 != null) {
            stringeeCall2.ringing(listener);
        }
    }

    public void answer() {
        if (!incoming || stateMachine.getStatus() != CallStatus.INCOMING || released) {
            return;
        }
        updateStatus(CallStatus.STARTING);
        audioManager.stopRinging();
        audioManager.start(videoCall);
        IncomingCallService.clear(context, getCallId());
        InCallService.startOrUpdate(context, generation);
        StatusListener listener = terminalOnError("answer");
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall.answer(listener);
        } else if (stringeeCall2 != null) {
            stringeeCall2.answer(listener);
        }
    }

    public void reject() {
        if (released) {
            return;
        }
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall.reject(terminalOnError("reject"));
        } else if (stringeeCall2 != null) {
            stringeeCall2.reject(terminalOnError("reject"));
        }
        terminal(CallStatus.ENDED);
    }

    public void hangUp() {
        if (released) {
            return;
        }
        boolean cancelledPendingSetup = !incoming && outgoingStarted
                && setupStateMachine.cancel();
        if (!cancelledPendingSetup) {
            if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
                stringeeCall.hangup(terminalOnError("hangup"));
            } else if (stringeeCall2 != null) {
                stringeeCall2.hangup(terminalOnError("hangup"));
            }
        }
        terminal(CallStatus.ENDED);
    }

    void endForDisconnect() {
        if (incoming && stateMachine.getStatus() == CallStatus.INCOMING) {
            reject();
        } else {
            hangUp();
        }
    }

    private StatusListener terminalOnError(String action) {
        return new StatusListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(StringeeError error) {
                owner.onSessionError(CallSession.this, action, error);
                terminal(CallStatus.ENDED);
            }
        };
    }

    public void toggleMute() {
        if (released) {
            return;
        }
        boolean mute = micEnabled;
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall.mute(mute);
        } else if (stringeeCall2 != null) {
            stringeeCall2.mute(mute);
        }
        micEnabled = !micEnabled;
        if (uiListener != null) {
            uiListener.onMicChanged(micEnabled);
        }
    }

    public void toggleVideo() {
        if (!videoCall || released) {
            return;
        }
        videoEnabled = !videoEnabled;
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall.enableVideo(videoEnabled);
        } else if (stringeeCall2 != null) {
            stringeeCall2.enableVideo(videoEnabled);
        }
        if (uiListener != null) {
            uiListener.onVideoChanged(videoEnabled);
        }
    }

    public void toggleSpeaker() {
        audioManager.toggleBuiltInDevice();
    }

    public void switchCamera() {
        if (!videoCall || switchingCamera || released) {
            return;
        }
        switchingCamera = true;
        StatusListener listener = new StatusListener() {
            @Override
            public void onSuccess() {
                switchingCamera = false;
            }

            @Override
            public void onError(StringeeError error) {
                switchingCamera = false;
                owner.onSessionError(CallSession.this, "switchCamera", error);
            }
        };
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall.switchCamera(listener);
        } else if (stringeeCall2 != null) {
            stringeeCall2.switchCamera(listener);
        }
    }

    public View getLocalView() {
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            return stringeeCall.getLocalView2();
        }
        return stringeeCall2 == null ? null : stringeeCall2.getLocalView2();
    }

    public View getRemoteView() {
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            return stringeeCall.getRemoteView2();
        }
        return stringeeCall2 == null ? null : stringeeCall2.getRemoteView2();
    }

    public void renderLocalView() {
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall.renderLocalView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        } else if (stringeeCall2 != null) {
            stringeeCall2.renderLocalView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        }
    }

    public void renderRemoteView() {
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall.renderRemoteView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        } else if (stringeeCall2 != null) {
            stringeeCall2.renderRemoteView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        }
    }

    public void prepareScreenShare(Activity activity, ActivityResultLauncher<Intent> launcher,
                            MediaProjectionManager projectionManager) {
        if (engine != CallEngine.STRINGEE_CALL2 || !videoCall || sharing
                || stateMachine.getStatus() != CallStatus.STARTED || stringeeCall2 == null) {
            return;
        }
        screenCapture = new StringeeScreenCapture(activity);
        launcher.launch(projectionManager.createScreenCaptureIntent());
    }

    public void startScreenShare(MediaProjectionCallService service, Intent permissionData) {
        if (screenCapture == null || stringeeCall2 == null || sharing || released) {
            service.stopService();
            return;
        }
        mediaProjectionService = service;
        screenCapture.createCapture(permissionData, new CallbackListener<StringeeVideoTrack>() {
            @Override
            public void onSuccess(StringeeVideoTrack track) {
                stringeeCall2.startCaptureScreen(screenCapture, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        sharing = true;
                        if (videoEnabled) {
                            videoEnabled = false;
                            stringeeCall2.enableVideo(false);
                            if (uiListener != null) {
                                uiListener.onVideoChanged(false);
                            }
                        }
                        if (uiListener != null) {
                            uiListener.onSharingChanged(true);
                        }
                    }

                    @Override
                    public void onError(StringeeError error) {
                        owner.onSessionError(CallSession.this, "startScreenShare", error);
                        finishSharing();
                    }
                });
            }

            @Override
            public void onError(StringeeError error) {
                owner.onSessionError(CallSession.this, "createScreenCapture", error);
                finishSharing();
            }
        });
    }

    public void stopScreenShare() {
        if (!sharing || stringeeCall2 == null) {
            return;
        }
        stringeeCall2.stopCaptureScreen(new StatusListener() {
            @Override
            public void onSuccess() {
            }
        });
        finishSharing();
    }

    private void finishSharing() {
        sharing = false;
        screenCapture = null;
        if (mediaProjectionService != null) {
            mediaProjectionService.stopService();
            mediaProjectionService = null;
        }
        if (uiListener != null) {
            uiListener.onSharingChanged(false);
        }
    }

    public void onMediaProjectionServiceDestroyed(MediaProjectionCallService service) {
        if (mediaProjectionService != service) {
            return;
        }
        mediaProjectionService = null;
        if (sharing && stringeeCall2 != null) {
            stringeeCall2.stopCaptureScreen(new StatusListener() {
                @Override
                public void onSuccess() {
                }
            });
        }
        sharing = false;
        screenCapture = null;
        if (uiListener != null) {
            uiListener.onSharingChanged(false);
        }
    }

    private void startTimerIfNeeded() {
        if (startedAt == 0) {
            startedAt = System.currentTimeMillis();
        }
        if (timer != null) {
            return;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startedAt;
                SimpleDateFormat format = new SimpleDateFormat("mm:ss", Locale.getDefault());
                format.setTimeZone(TimeZone.getTimeZone("GMT"));
                String value = format.format(new Date(elapsed));
                CallUtils.runOnUiThread(() -> {
                    if (isCurrent() && uiListener != null) {
                        uiListener.onTimer(value);
                    }
                });
            }
        }, 0, 1000);
    }

    private void terminal(CallStatus terminalStatus) {
        if (released) {
            return;
        }
        stateMachine.setStatus(terminalStatus);
        if (uiListener != null) {
            uiListener.onCallStatus(terminalStatus);
        }
        owner.onSessionStateChanged(this, terminalStatus);
        release();
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        if (!incoming && outgoingStarted) {
            setupStateMachine.cancel();
        }
        outgoingCallback = null;
        cancelSetupTimeout();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (sharing && stringeeCall2 != null) {
            try {
                stringeeCall2.stopCaptureScreen(new StatusListener() {
                    @Override
                    public void onSuccess() {
                    }
                });
            } catch (RuntimeException exception) {
                CallUtils.reportException(CallSession.class, exception);
            }
        }
        finishSharing();
        try {
            if (stringeeCall != null) {
                stringeeCall.setCallListener(null);
            }
            if (stringeeCall2 != null) {
                stringeeCall2.setCallListener(null);
                stringeeCall2.setCaptureSessionListener(null);
            }
        } catch (RuntimeException exception) {
            CallUtils.reportException(CallSession.class, exception);
        }
        String releasedCallId = getCallId();
        stringeeCall = null;
        stringeeCall2 = null;
        remoteScreenTracks.clear();
        audioManager.stopRinging();
        audioManager.stop();
        IncomingCallService.clear(context, releasedCallId);
        InCallService.stop(context, generation);
        uiListener = null;
        owner.onSessionReleased(this);
    }

    private boolean isCurrent() {
        return !released && owner instanceof StringeeCallManager
                && ((StringeeCallManager) owner).isCurrentSession(this, generation);
    }

    public boolean isIncoming() {
        return incoming;
    }

    public boolean isVideoCall() {
        return videoCall;
    }

    public boolean isSharing() {
        return sharing;
    }

    public CallEngine getEngine() {
        return engine;
    }

    public CallStatus getStatus() {
        return stateMachine.getStatus();
    }

    public String getFrom() {
        return peer;
    }

    public String getCallId() {
        if (setupStateMachine.hasServerCallId()) {
            callId = setupStateMachine.getServerCallId();
            return callId;
        }
        if (!CallUtils.isEmpty(callId)) {
            return callId;
        }
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            callId = stringeeCall.getCallId();
            return callId;
        }
        if (stringeeCall2 != null) {
            callId = stringeeCall2.getCallId();
        }
        return callId;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public long getGeneration() {
        return generation;
    }

    boolean isOutgoingSetupPending() {
        return !incoming && setupStateMachine.isPending();
    }

    void onInCallServiceStartFailed() {
        StringeeError error = new StringeeError(-1,
                "Unable to start the in-call foreground service");
        if (isOutgoingSetupPending()) {
            handleMakeCallFailure(error);
            return;
        }
        owner.onSessionError(this, "foregroundService", error);
        hangUp();
    }

}
