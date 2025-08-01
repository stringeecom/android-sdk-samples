package com.stringee.apptoappcallsample.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.stringee.apptoappcallsample.common.AudioManagerUtils;
import com.stringee.apptoappcallsample.common.CallStatus;
import com.stringee.apptoappcallsample.common.Constant;
import com.stringee.apptoappcallsample.common.NotificationUtils;
import com.stringee.apptoappcallsample.common.Utils;
import com.stringee.apptoappcallsample.listener.OnCallListener;
import com.stringee.apptoappcallsample.service.MyMediaProjectionService;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.video.StringeeScreenCapture;
import com.stringee.video.StringeeVideoTrack;

import org.json.JSONObject;
import org.webrtc.RendererCommon;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class CallManager {
    private static volatile CallManager instance;
    private final Context context;
    private StringeeCall stringeeCall;
    private StringeeCall2 stringeeCall2;
    private boolean isStringeeCall;
    private boolean isVideoCall = false;
    private boolean isSpeakerOn = false;
    private boolean isVideoEnable = false;
    private boolean isMicOn = true;
    private boolean isSharing = false;
    private final AudioManagerUtils audioManagerUtils;
    private OnCallListener listener;
    private final ClientManager clientManager;
    private StringeeCall.SignalingState callSignalingState = StringeeCall.SignalingState.CALLING;
    private StringeeCall.MediaState callMediaState = StringeeCall.MediaState.DISCONNECTED;
    private StringeeCall2.SignalingState call2SignalingState = StringeeCall2.SignalingState.CALLING;
    private StringeeCall2.MediaState call2MediaState = StringeeCall2.MediaState.DISCONNECTED;
    private CallStatus callStatus = CallStatus.CALLING;
    private Timer timer;
    private StringeeScreenCapture screenCapture;
    private MyMediaProjectionService mediaProjectionService;

    public CallManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManagerUtils = AudioManagerUtils.getInstance(context);
        this.audioManagerUtils.setAudioEvents(selectedAudioDevice -> Utils.runOnUiThread(() -> Log.d(Constant.TAG, "onAudioEvents: selectedAudioDevice - " + selectedAudioDevice.name())));
        this.clientManager = ClientManager.getInstance(context);
    }

    public static CallManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CallManager.class) {
                if (instance == null) {
                    instance = new CallManager(context);
                }
            }
        }
        return instance;
    }

    public CallStatus getCallStatus() {
        return callStatus;
    }

    public boolean isSharing() {
        return isSharing;
    }

    public void initializedOutgoingCall(String to, boolean isVideoCall, boolean isStringeeCall) {
        clientManager.isInCall = true;
        if (isStringeeCall) {
            this.stringeeCall = new StringeeCall(clientManager.getStringeeClient(), clientManager.getStringeeClient().getUserId(), to);
            this.stringeeCall.setVideoCall(isVideoCall);
        } else {
            this.stringeeCall2 = new StringeeCall2(clientManager.getStringeeClient(), clientManager.getStringeeClient().getUserId(), to);
            this.stringeeCall2.setVideoCall(isVideoCall);
        }
        this.isStringeeCall = isStringeeCall;
        this.isVideoCall = isVideoCall;
        this.isSpeakerOn = isVideoCall;
        this.isVideoEnable = isVideoCall;
        this.callStatus = CallStatus.CALLING;
        registerCallEvent();
    }

    public void initializedIncomingCall(StringeeCall stringeeCall) {
        clientManager.isInCall = true;
        this.isStringeeCall = true;
        this.stringeeCall = stringeeCall;
        this.isVideoCall = stringeeCall.isVideoCall();
        this.isSpeakerOn = stringeeCall.isVideoCall();
        this.isVideoEnable = stringeeCall.isVideoCall();
        this.callStatus = CallStatus.INCOMING;
        registerCallEvent();
    }

    public void initializedIncomingCall(StringeeCall2 stringeeCall2) {
        clientManager.isInCall = true;
        this.isStringeeCall = false;
        this.stringeeCall2 = stringeeCall2;
        this.isVideoCall = stringeeCall2.isVideoCall();
        this.isSpeakerOn = stringeeCall2.isVideoCall();
        this.isVideoEnable = stringeeCall2.isVideoCall();
        this.callStatus = CallStatus.INCOMING;
        registerCallEvent();
    }

    public void registerEvent(OnCallListener listener) {
        this.listener = listener;
    }

    private void registerCallEvent() {
        if (isStringeeCall) {
            stringeeCall.setCallListener(new StringeeCall.StringeeCallListener() {
                @Override
                public void onSignalingStateChange(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState, String reason, int sipCode, String sipReason) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onSignalingStateChange: " + signalingState);
                        callSignalingState = signalingState;
                        switch (callSignalingState) {
                            case CALLING:
                                callStatus = CallStatus.CALLING;
                                break;
                            case RINGING:
                                callStatus = CallStatus.RINGING;
                                break;
                            case ANSWERED:
                                callStatus = CallStatus.STARTING;
                                if (callMediaState == StringeeCall.MediaState.CONNECTED) {
                                    startTimer();
                                    callStatus = CallStatus.STARTED;
                                }
                                break;
                            case BUSY:
                                callStatus = CallStatus.BUSY;
                                release();
                                break;
                            case ENDED:
                                callStatus = CallStatus.ENDED;
                                release();
                                break;
                        }
                        if (listener != null) {
                            listener.onCallStatus(callStatus);
                        }
                    });
                }

                @Override
                public void onError(StringeeCall stringeeCall, int code, String desc) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onError: " + desc);
                        callStatus = CallStatus.ENDED;
                        if (listener != null) {
                            listener.onError(desc);
                            listener.onCallStatus(callStatus);
                        }
                    });
                }

                @Override
                public void onHandledOnAnotherDevice(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState, String desc) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onHandledOnAnotherDevice: " + signalingState);
                        if (signalingState != StringeeCall.SignalingState.RINGING) {
                            callStatus = CallStatus.ENDED;
                            if (listener != null) {
                                listener.onCallStatus(callStatus);
                            }
                        }
                    });
                }

                @Override
                public void onMediaStateChange(StringeeCall stringeeCall, StringeeCall.MediaState mediaState) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onMediaStateChange: " + mediaState);
                        callMediaState = mediaState;
                        if (callSignalingState == StringeeCall.SignalingState.ANSWERED) {
                            callStatus = CallStatus.STARTED;
                            startTimer();
                            if (listener != null) {
                                listener.onCallStatus(callStatus);
                            }
                        }
                    });
                }

                @Override
                public void onLocalStream(StringeeCall stringeeCall) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onLocalStream");
                        if (isVideoCall) {
                            if (listener != null) {
                                listener.onReceiveLocalStream();
                            }
                        }
                    });
                }

                @Override
                public void onRemoteStream(StringeeCall stringeeCall) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onRemoteStream");
                        if (isVideoCall) {
                            if (listener != null) {
                                listener.onReceiveRemoteStream();
                            }
                        }
                    });
                }

                @Override
                public void onCallInfo(StringeeCall stringeeCall, JSONObject jsonObject) {
                    Utils.runOnUiThread(() -> Log.d(Constant.TAG, "onCallInfo: " + jsonObject.toString()));
                }
            });
        } else {
            stringeeCall2.setCallListener(new StringeeCall2.StringeeCallListener() {
                @Override
                public void onSignalingStateChange(StringeeCall2 stringeeCall2, StringeeCall2.SignalingState signalingState, String reason, int sipCode, String sipReason) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onSignalingStateChange: " + signalingState);
                        call2SignalingState = signalingState;
                        switch (call2SignalingState) {
                            case CALLING:
                                callStatus = CallStatus.CALLING;
                                break;
                            case RINGING:
                                callStatus = CallStatus.RINGING;
                                break;
                            case ANSWERED:
                                callStatus = CallStatus.STARTING;
                                if (call2MediaState == StringeeCall2.MediaState.CONNECTED) {
                                    startTimer();
                                    callStatus = CallStatus.STARTED;
                                }
                                break;
                            case BUSY:
                                callStatus = CallStatus.BUSY;
                                release();
                                break;
                            case ENDED:
                                callStatus = CallStatus.ENDED;
                                release();
                                break;
                        }
                        if (listener != null) {
                            listener.onCallStatus(callStatus);
                        }
                    });
                }

                @Override
                public void onError(StringeeCall2 stringeeCall2, int code, String desc) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onError: " + desc);
                        callStatus = CallStatus.ENDED;
                        if (listener != null) {
                            listener.onError(desc);
                            listener.onCallStatus(callStatus);
                        }
                    });
                }

                @Override
                public void onHandledOnAnotherDevice(StringeeCall2 stringeeCall2, StringeeCall2.SignalingState signalingState, String desc) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onHandledOnAnotherDevice: " + signalingState);
                        if (signalingState != StringeeCall2.SignalingState.RINGING) {
                            callStatus = CallStatus.ENDED;
                            if (listener != null) {
                                listener.onCallStatus(callStatus);
                            }
                        }
                    });
                }

                @Override
                public void onMediaStateChange(StringeeCall2 stringeeCall2, StringeeCall2.MediaState mediaState) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onMediaStateChange: " + mediaState);
                        call2MediaState = mediaState;
                        if (call2SignalingState == StringeeCall2.SignalingState.ANSWERED) {
                            callStatus = CallStatus.STARTED;
                            startTimer();
                            if (listener != null) {
                                listener.onCallStatus(callStatus);
                            }
                        }
                    });
                }

                @Override
                public void onLocalStream(StringeeCall2 stringeeCall2) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onLocalStream");
                        if (isVideoCall) {
                            if (listener != null) {
                                listener.onReceiveLocalStream();
                            }
                        }
                    });
                }

                @Override
                public void onRemoteStream(StringeeCall2 stringeeCall2) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onRemoteStream");
                        if (isVideoCall) {
                            if (listener != null) {
                                listener.onReceiveRemoteStream();
                            }
                        }
                    });
                }

                @Override
                public void onVideoTrackAdded(StringeeVideoTrack stringeeVideoTrack) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onVideoTrackAdded: " + stringeeVideoTrack.getId());
                        if (stringeeVideoTrack.getTrackType() == StringeeVideoTrack.TrackType.SCREEN) {
                            if (listener != null) {
                                listener.onVideoTrackAdded(stringeeVideoTrack);
                            }
                        }
                    });
                }

                @Override
                public void onVideoTrackRemoved(StringeeVideoTrack stringeeVideoTrack) {
                    Utils.runOnUiThread(() -> {
                        Log.d(Constant.TAG, "onVideoTrackRemoved: " + stringeeVideoTrack.getId());
                        if (stringeeVideoTrack.getTrackType() == StringeeVideoTrack.TrackType.SCREEN) {
                            if (listener != null) {
                                listener.onVideoTrackRemoved(stringeeVideoTrack);
                            }
                        }
                    });
                }

                @Override
                public void onCallInfo(StringeeCall2 stringeeCall2, JSONObject jsonObject) {
                    Utils.runOnUiThread(() -> Log.d(Constant.TAG, "onCallInfo: " + jsonObject.toString()));
                }

                @Override
                public void onTrackMediaStateChange(String s, StringeeVideoTrack.MediaType mediaType, boolean b) {

                }
            });
        }
    }

    private void startAudioManager() {
        audioManagerUtils.startAudioManager();
        audioManagerUtils.setSpeakerphoneOn(isSpeakerOn);
    }

    public void makeCall() {
        if (isCallNotInitialized()) {
            if (listener != null) {
                listener.onCallStatus(CallStatus.ENDED);
            }
            release();
            return;
        }
        if (isStringeeCall) {
            stringeeCall.makeCall(new StatusListener() {
                @Override
                public void onSuccess() {
                    startAudioManager();
                    handleResponse("makeCall", true, null);
                }

                @Override
                public void onError(StringeeError stringeeError) {
                    super.onError(stringeeError);
                    handleResponse("makeCall", false, stringeeError.getMessage());
                }
            });
        } else {
            stringeeCall2.makeCall(new StatusListener() {
                @Override
                public void onSuccess() {
                    startAudioManager();
                    handleResponse("makeCall", true, null);
                }

                @Override
                public void onError(StringeeError stringeeError) {
                    super.onError(stringeeError);
                    handleResponse("makeCall", false, stringeeError.getMessage());
                }
            });
        }

    }

    public void initAnswer() {
        if (isCallNotInitialized()) {
            if (listener != null) {
                listener.onCallStatus(CallStatus.ENDED);
            }
            release();
            return;
        }
        if (isStringeeCall) {
            stringeeCall.ringing(new StatusListener() {
                @Override
                public void onSuccess() {
                    handleResponse("initAnswer", true, null);
                }

                @Override
                public void onError(StringeeError stringeeError) {
                    super.onError(stringeeError);
                    handleResponse("initAnswer", false, stringeeError.getMessage());
                }
            });
        } else {
            stringeeCall2.ringing(new StatusListener() {
                @Override
                public void onSuccess() {
                    handleResponse("initAnswer", true, null);
                }

                @Override
                public void onError(StringeeError stringeeError) {
                    super.onError(stringeeError);
                    handleResponse("initAnswer", false, stringeeError.getMessage());
                }
            });
        }
    }

    public void answer() {
        if (isCallNotInitialized()) {
            if (listener != null) {
                listener.onCallStatus(CallStatus.ENDED);
            }
            release();
            return;
        }
        NotificationUtils.getInstance(context).cancelNotification(Constant.INCOMING_CALL_ID);
        if (isStringeeCall) {
            stringeeCall.answer(new StatusListener() {
                @Override
                public void onSuccess() {
                    startAudioManager();
                    audioManagerUtils.stopRinging();
                    handleResponse("answer", true, null);
                }

                @Override
                public void onError(StringeeError stringeeError) {
                    super.onError(stringeeError);
                    handleResponse("answer", false, stringeeError.getMessage());
                }
            });
        } else {
            stringeeCall2.answer(new StatusListener() {
                @Override
                public void onSuccess() {
                    startAudioManager();
                    audioManagerUtils.stopRinging();
                    handleResponse("answer", true, null);
                }

                @Override
                public void onError(StringeeError stringeeError) {
                    super.onError(stringeeError);
                    handleResponse("answer", false, stringeeError.getMessage());
                }
            });
        }
    }

    public void endCall(boolean isHangUp) {
        if (isCallNotInitialized()) {
            if (listener != null) {
                listener.onCallStatus(CallStatus.ENDED);
            }
            release();
            return;
        }
        if (isStringeeCall) {
            if (isHangUp) {
                stringeeCall.hangup(new StatusListener() {
                    @Override
                    public void onSuccess() {
                        handleResponse("hangup", true, null);
                    }

                    @Override
                    public void onError(StringeeError stringeeError) {
                        super.onError(stringeeError);
                        handleResponse("hangup", false, stringeeError.getMessage());
                    }
                });
            } else {
                stringeeCall.reject(new StatusListener() {
                    @Override
                    public void onSuccess() {
                        handleResponse("reject", true, null);
                    }

                    @Override
                    public void onError(StringeeError stringeeError) {
                        super.onError(stringeeError);
                        handleResponse("reject", false, stringeeError.getMessage());
                    }
                });
            }
        } else {
            if (isHangUp) {
                stringeeCall2.hangup(new StatusListener() {
                    @Override
                    public void onSuccess() {
                        handleResponse("hangup", true, null);
                    }

                    @Override
                    public void onError(StringeeError stringeeError) {
                        super.onError(stringeeError);
                        handleResponse("hangup", false, stringeeError.getMessage());
                    }
                });
            } else {
                stringeeCall2.reject(new StatusListener() {
                    @Override
                    public void onSuccess() {
                        handleResponse("reject", true, null);
                    }

                    @Override
                    public void onError(StringeeError stringeeError) {
                        super.onError(stringeeError);
                        handleResponse("reject", false, stringeeError.getMessage());
                    }
                });
            }
        }
        if (listener != null) {
            listener.onCallStatus(CallStatus.ENDED);
        }
        release();
    }

    public void enableVideo() {
        if (isCallNotInitialized()) {
            if (listener != null) {
                listener.onCallStatus(CallStatus.ENDED);
            }
            release();
            return;
        }
        if (isStringeeCall) {
            stringeeCall.enableVideo(!isVideoEnable);
        } else {
            stringeeCall2.enableVideo(!isVideoEnable);
        }
        handleResponse("enableVideo", true, null);
        isVideoEnable = !isVideoEnable;
        if (listener != null) {
            listener.onVideoChange(isVideoEnable);
        }
    }

    public void mute() {
        if (isCallNotInitialized()) {
            if (listener != null) {
                listener.onCallStatus(CallStatus.ENDED);
            }
            release();
            return;
        }
        if (isStringeeCall) {
            stringeeCall.mute(isMicOn);
        } else {
            stringeeCall2.mute(isMicOn);
        }
        handleResponse("mute", true, null);
        isMicOn = !isMicOn;
        if (listener != null) {
            listener.onMicChange(isMicOn);
        }
    }

    public void changeSpeaker() {
        if (isCallNotInitialized()) {
            if (listener != null) {
                listener.onCallStatus(CallStatus.ENDED);
            }
            release();
            return;
        }
        audioManagerUtils.setSpeakerphoneOn(!isSpeakerOn);
        handleResponse("changeSpeaker", true, null);
        isSpeakerOn = !isSpeakerOn;
        if (listener != null) {
            listener.onSpeakerChange(isSpeakerOn);
        }
    }

    public void switchCamera() {
        if (isCallNotInitialized()) {
            if (listener != null) {
                listener.onCallStatus(CallStatus.ENDED);
            }
            release();
            return;
        }
        if (isStringeeCall) {
            stringeeCall.switchCamera(new StatusListener() {
                @Override
                public void onSuccess() {
                    handleResponse("switchCamera", true, null);
                }

                @Override
                public void onError(StringeeError stringeeError) {
                    super.onError(stringeeError);
                    handleResponse("switchCamera", false, stringeeError.getMessage());
                }
            });
        } else {
            stringeeCall2.switchCamera(new StatusListener() {
                @Override
                public void onSuccess() {
                    handleResponse("switchCamera", true, null);
                }

                @Override
                public void onError(StringeeError stringeeError) {
                    super.onError(stringeeError);
                    handleResponse("switchCamera", false, stringeeError.getMessage());
                }
            });
        }
    }

    private void handleResponse(String action, boolean isSuccess, String message) {
        Log.d(Constant.TAG, action + ": " + (isSuccess ? "success" : message));
        if (!isSuccess) {
            if (listener != null) {
                listener.onError(message);
            }
            release();
        }
    }

    private boolean isCallNotInitialized() {
        boolean isCallNotInitialized;
        if (isStringeeCall) {
            isCallNotInitialized = stringeeCall == null;
        } else {
            isCallNotInitialized = stringeeCall2 == null;
        }
        if (isCallNotInitialized) {
            if (listener != null) {
                listener.onError("call is not initialized");
            }
        }
        return isCallNotInitialized;
    }

    public void stopSharing() {
        if (stringeeCall2 != null) {
            if (!(callStatus == CallStatus.STARTED && call2MediaState != null && call2MediaState == StringeeCall2.MediaState.CONNECTED)) {
                return;
            }
            if (isSharing) {
                stringeeCall2.stopCaptureScreen(new StatusListener() {
                    @Override
                    public void onSuccess() {

                    }
                });
                if (mediaProjectionService != null) {
                    mediaProjectionService.stopService();
                }
                isSharing = false;
            }
        }
        if (listener != null) {
            listener.onSharing(isSharing);
        }
    }

    public void prepareShareScreen(Activity activity, ActivityResultLauncher<Intent> activityResultLauncher, MediaProjectionManager manager) {
        if (stringeeCall2 != null) {
            if (!(callStatus == CallStatus.STARTED && call2MediaState != null && call2MediaState == StringeeCall2.MediaState.CONNECTED)) {
                return;
            }
            if (!isSharing) {
                screenCapture = new StringeeScreenCapture(activity);
                activityResultLauncher.launch(manager.createScreenCaptureIntent());
            }
        }
    }

    public void startCapture(MyMediaProjectionService mediaProjectionService, Intent mediaProjectionPermissionResultData) {
        this.mediaProjectionService = mediaProjectionService;
        if (stringeeCall2 != null) {
            if (!(callStatus == CallStatus.STARTED && call2MediaState != null && call2MediaState == StringeeCall2.MediaState.CONNECTED)) {
                return;
            }
            if (!isSharing) {
                screenCapture.createCapture(mediaProjectionPermissionResultData, new CallbackListener<StringeeVideoTrack>() {
                    @Override
                    public void onSuccess(StringeeVideoTrack stringeeVideoTrack) {
                        stringeeCall2.startCaptureScreen(screenCapture, new StatusListener() {
                            @Override
                            public void onSuccess() {
                                isSharing = true;
                                if (listener != null) {
                                    listener.onSharing(true);
                                }
                            }

                            @Override
                            public void onError(StringeeError stringeeError) {
                                super.onError(stringeeError);
                                isSharing = false;
                                if (listener != null) {
                                    listener.onSharing(false);
                                }
                                if (mediaProjectionService != null) {
                                    mediaProjectionService.stopService();
                                }
                            }
                        });

                    }

                    @Override
                    public void onError(StringeeError errorInfo) {
                        super.onError(errorInfo);
                        isSharing = false;
                        if (listener != null) {
                            listener.onSharing(false);
                        }
                        if (mediaProjectionService != null) {
                            mediaProjectionService.stopService();
                        }
                    }
                });
            }
        }
    }

    public void release() {
        Log.d(Constant.TAG, "release callManager");
        if (isSharing && !isStringeeCall && isVideoCall) {
            if (stringeeCall2 != null) {
                stringeeCall2.stopCaptureScreen(new StatusListener() {
                    @Override
                    public void onSuccess() {

                    }
                });
            }

            if (screenCapture != null) {
                screenCapture = null;
            }

            if (mediaProjectionService != null) {
                mediaProjectionService.stopService();
            }
        }
        clientManager.isInCall = false;
        audioManagerUtils.stopAudioManager();
        audioManagerUtils.stopRinging();
        NotificationUtils.getInstance(context).cancelNotification(Constant.INCOMING_CALL_ID);
        if (timer != null) {
            timer.cancel();
        }
        if (isStringeeCall) {
            stringeeCall = null;
        } else {
            stringeeCall2 = null;
        }
        instance = null;
    }

    public String getFrom() {
        if (isStringeeCall) {
            return stringeeCall.getFrom();
        } else {
            return stringeeCall2.getFrom();
        }
    }

    public View getLocalView() {
        if (isStringeeCall) {
            return stringeeCall.getLocalView2();
        } else {
            return stringeeCall2.getLocalView2();
        }
    }

    public View getRemoteView() {
        if (isStringeeCall) {
            return stringeeCall.getRemoteView2();
        } else {
            return stringeeCall2.getRemoteView2();
        }
    }

    public void renderLocalView() {
        if (isStringeeCall) {
            stringeeCall.renderLocalView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
            stringeeCall.getLocalView2().setMirror(false);
        } else {
            stringeeCall2.renderLocalView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        }
    }

    public void renderRemoteView() {
        if (isStringeeCall) {
            stringeeCall.renderRemoteView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        } else {
            stringeeCall2.renderRemoteView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        }
    }

    private void startTimer() {
        if (timer == null) {
            long startTime = System.currentTimeMillis();

            timer = new Timer();
            TimerTask timerTask = new TimerTask() {
                @Override
                public void run() {
                    Utils.runOnUiThread(() -> {
                        if (listener != null) {
                            long time = System.currentTimeMillis() - startTime;
                            SimpleDateFormat format = new SimpleDateFormat("mm:ss", Locale.getDefault());
                            format.setTimeZone(TimeZone.getTimeZone("GMT"));
                            listener.onTimer(format.format(new Date(time)));
                        }
                    });
                }
            };
            timer.schedule(timerTask, 0, 1000);
        }
    }
}
