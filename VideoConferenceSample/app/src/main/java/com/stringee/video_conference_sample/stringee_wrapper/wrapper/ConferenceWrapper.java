package com.stringee.video_conference_sample.stringee_wrapper.wrapper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import com.stringee.StringeeClient;
import com.stringee.common.StringeeAudioManager;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.listener.StringeeRoomListener;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.video.RemoteParticipant;
import com.stringee.video.StringeeRoom;
import com.stringee.video.StringeeScreenCapture;
import com.stringee.video.StringeeVideo;
import com.stringee.video.StringeeVideoTrack;
import com.stringee.video.TextureViewRenderer;
import com.stringee.video_conference_sample.stringee_wrapper.common.Utils;
import com.stringee.video_conference_sample.stringee_wrapper.service.MyMediaProjectionService;
import com.stringee.video_conference_sample.stringee_wrapper.wrapper.listener.ConferenceListener;

import org.json.JSONObject;
import org.webrtc.RendererCommon;

public class ConferenceWrapper implements StringeeRoomListener {
    private StringeeRoom stringeeRoom;
    private final StringeeClient stringeeClient;
    private final String roomToken;
    private final Context context;
    private boolean isLeaving;
    private StringeeVideoTrack localVideoTrack;
    private StringeeVideoTrack localShareTrack;
    private StringeeAudioManager audioManager;
    private ConferenceListener conferenceListener;
    private StringeeScreenCapture screenCapture;
    private MyMediaProjectionService mediaProjectionService;

    ConferenceWrapper(Context context, StringeeClient stringeeClient, String roomToken) {
        this.context = context.getApplicationContext();
        this.stringeeClient = stringeeClient;
        this.roomToken = roomToken;
    }

    public static ConferenceWrapper create(Context context, StringeeClient stringeeClient, String roomToken) {
        return new ConferenceWrapper(context, stringeeClient, roomToken);
    }

    public void setConferenceListener(ConferenceListener conferenceListener) {
        this.conferenceListener = conferenceListener;
    }

    public void connectRoom() {
        this.stringeeRoom = StringeeVideo.connect(stringeeClient, roomToken, this);
        Utils.runOnUiThread(() -> {
            audioManager = StringeeAudioManager.create(context);
            audioManager.start((selectedAudioDevice, availableAudioDevices) -> {
                audioManager.setSpeakerphoneOn(selectedAudioDevice == StringeeAudioManager.AudioDevice.EARPIECE || selectedAudioDevice == StringeeAudioManager.AudioDevice.SPEAKER_PHONE);
                audioManager.setBluetoothScoOn(selectedAudioDevice == StringeeAudioManager.AudioDevice.BLUETOOTH);
            });
            audioManager.setSpeakerphoneOn(true);
        });
    }

    public void displayLocalTrack(FrameLayout flPreview) {
        StringeeVideoTrack stringeeVideoTrack = createLocalVideoTrack();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER;
        TextureViewRenderer view = stringeeVideoTrack.getView2(context);
        if (view.getParent() != null) {
            ((FrameLayout) view.getParent()).removeView(view);
        }
        flPreview.addView(view, layoutParams);
        stringeeVideoTrack.renderView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        view.setMirror(true);
    }

    public StringeeVideoTrack createLocalVideoTrack() {
        if (localVideoTrack != null) {
            return localVideoTrack;
        }
        StringeeVideoTrack.Options options = new StringeeVideoTrack.Options();
        options.video(true);
        options.audio(true);
        options.screen(false);
        localVideoTrack = StringeeVideo.createLocalVideoTrack(context, options, new StringeeVideoTrack.CaptureSessionListener() {
            @Override
            public void onCapturerStarted(StringeeVideoTrack.TrackType trackType) {

            }

            @Override
            public void onCapturerStopped(StringeeVideoTrack.TrackType trackType) {

            }
        }, new StatusListener() {
            @Override
            public void onSuccess() {

            }
        });
        return localVideoTrack;
    }

    public void release() {
        if (localVideoTrack != null) {
            localVideoTrack.release();
        }
        if (stringeeRoom != null) {
            StringeeVideo.release(stringeeRoom);
        }
    }

    public void enableVideo(boolean enable) {
        if (localVideoTrack != null) {
            localVideoTrack.enableVideo(enable);
        }
    }

    public void enableMic(boolean enable) {
        if (localVideoTrack != null) {
            localVideoTrack.mute(!enable);
        }
    }

    public void switchCamera() {
        if (localVideoTrack != null) {
            localVideoTrack.switchCamera(new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        }
    }

    public void leaveRoom(boolean isLeaveAll) {
        if (isLeaving) {
            return;
        }
        isLeaving = true;
        if (stringeeRoom != null) {
            if (localVideoTrack != null) {
                localVideoTrack.release();
                stringeeRoom.unpublish(localVideoTrack, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("Stringee", "Unpublish success");
                    }

                    @Override
                    public void onError(StringeeError stringeeError) {
                        super.onError(stringeeError);
                        Log.d("Stringee", "Unpublish error: " + stringeeError.getMessage());
                    }
                });
            }
            if (localShareTrack != null) {
                localShareTrack.release();
                stringeeRoom.unpublish(localShareTrack, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("Stringee", "Unpublish success");
                    }

                    @Override
                    public void onError(StringeeError stringeeError) {
                        super.onError(stringeeError);
                        Log.d("Stringee", "Unpublish error: " + stringeeError.getMessage());
                    }
                });
            }

            stringeeRoom.leave(isLeaveAll, new StatusListener() {
                @Override
                public void onSuccess() {
                    Log.d("Stringee", "Leave room success");
                }

                @Override
                public void onError(StringeeError stringeeError) {
                    super.onError(stringeeError);
                    Log.d("Stringee", "Leave room error: " + stringeeError.getMessage());
                }
            });
        }
        if (mediaProjectionService != null) {
            mediaProjectionService.stopService();
        }

        if (audioManager != null) {
            audioManager.stop();
            audioManager = null;
        }

        if (conferenceListener != null) {
            conferenceListener.onLeaveRoom();
        }
    }

    public void stopSharing() {
        if (localShareTrack != null) {
            localShareTrack.release();
            stringeeRoom.unpublish(localShareTrack, new StatusListener() {
                @Override
                public void onSuccess() {

                }

            });
            localShareTrack = null;
        }
        if (conferenceListener != null) {
            conferenceListener.onSharingScreen(false);
        }
        if (mediaProjectionService != null) {
            mediaProjectionService.stopService();
        }
    }

    public void prepareShareScreen(Activity activity) {
        screenCapture = new StringeeScreenCapture(activity);
    }

    public void startCapture(MyMediaProjectionService mediaProjectionService, Intent mediaProjectionPermissionResultData) {
        this.mediaProjectionService = mediaProjectionService;
        screenCapture.createCapture(mediaProjectionPermissionResultData, new CallbackListener<StringeeVideoTrack>() {
            @Override
            public void onSuccess(StringeeVideoTrack stringeeVideoTrack) {
                if (stringeeVideoTrack == null) {
                    if (conferenceListener != null) {
                        conferenceListener.onSharingScreen(false);
                    }
                    if (mediaProjectionService != null) {
                        mediaProjectionService.stopService();
                    }
                    return;
                }
                localShareTrack = stringeeVideoTrack;
                stringeeRoom.publish(localShareTrack, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        if (conferenceListener != null) {
                            conferenceListener.onSharingScreen(true);
                        }
                    }

                    @Override
                    public void onError(StringeeError errorInfo) {
                        if (conferenceListener != null) {
                            conferenceListener.onSharingScreen(false);
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
                if (conferenceListener != null) {
                    conferenceListener.onSharingScreen(false);
                }
                if (mediaProjectionService != null) {
                    mediaProjectionService.stopService();
                }
            }
        });
    }

    public void subscribeTrack(StringeeVideoTrack videoTrack) {
        videoTrack.setListener(videoTrackListener(videoTrack));
        StringeeVideoTrack.Options opt = new StringeeVideoTrack.Options();
        opt.audio(true);
        opt.video(true);
        stringeeRoom.subscribe(videoTrack, opt, new StatusListener() {
            @Override
            public void onSuccess() {
                Log.d("Stringee", "Subscribe success: " + videoTrack.getId());
            }

            @Override
            public void onError(StringeeError errorInfo) {
                super.onError(errorInfo);
                Log.d("Stringee", "Subscribe error: " + videoTrack.getId());
            }
        });
    }

    private StringeeVideoTrack.Listener videoTrackListener(StringeeVideoTrack stringeeVideoTrack) {
        return new StringeeVideoTrack.Listener() {
            @Override
            public void onMediaAvailable() {
                Utils.runOnUiThread(() -> {
                    Log.d("Stringee", "Track ready: " + stringeeVideoTrack.getId());
                    if (conferenceListener != null) {
                        conferenceListener.onTrackAdded(stringeeVideoTrack);
                    }
                });
            }

            @Override
            public void onMediaStateChange(StringeeVideoTrack.MediaState mediaState) {

            }
        };
    }

    @Override
    public void onConnected(StringeeRoom stringeeRoom) {
        Utils.runOnUiThread(() -> {
            Log.d("Stringee", "onConnected: " + stringeeRoom.getId());
            if (localVideoTrack == null) {
                localVideoTrack = createLocalVideoTrack();
            }
            stringeeRoom.publish(localVideoTrack, new StatusListener() {
                @Override
                public void onSuccess() {
                    Log.d("Stringee", "Publish success");
                }

                @Override
                public void onError(StringeeError errorInfo) {
                    super.onError(errorInfo);
                    Log.d("Stringee", "Publish error: " + errorInfo.getMessage());
                }
            });

            for (RemoteParticipant remoteParticipant : stringeeRoom.getRemoteParticipants()) {
                for (StringeeVideoTrack videoTrack : remoteParticipant.getVideoTracks()) {
                    subscribeTrack(videoTrack);
                }
            }
        });
    }

    @Override
    public void onDisconnected(StringeeRoom stringeeRoom) {
        Utils.runOnUiThread(() -> Log.d("Stringee", "onDisconnected: " + stringeeRoom.getId()));
    }

    @Override
    public void onError(StringeeRoom stringeeRoom, StringeeError stringeeError) {
        Utils.runOnUiThread(() -> {
            Log.d("Stringee", "onError: " + stringeeRoom.getId());
            if (isLeaving) {
                return;
            }
            isLeaving = true;
            if (conferenceListener != null) {
                conferenceListener.onLeaveRoom();
            }
        });

    }

    @Override
    public void onParticipantConnected(StringeeRoom stringeeRoom, RemoteParticipant remoteParticipant) {
        Utils.runOnUiThread(() -> Log.d("Stringee", "onParticipantConnected: " + stringeeRoom.getId() + " - " + remoteParticipant.getId()));
    }

    @Override
    public void onParticipantDisconnected(StringeeRoom stringeeRoom, RemoteParticipant remoteParticipant) {
        Utils.runOnUiThread(() -> {
            Log.d("Stringee", "onParticipantDisconnected: " + stringeeRoom.getId() + " - " + remoteParticipant.getId());
            if (remoteParticipant.getId().equals(stringeeClient.getUserId())) {
                if (isLeaving) {
                    return;
                }
                isLeaving = true;
                if (conferenceListener != null) {
                    conferenceListener.onLeaveRoom();
                }
            }
        });
    }

    @Override
    public void onVideoTrackAdded(StringeeRoom stringeeRoom, StringeeVideoTrack stringeeVideoTrack) {
        Utils.runOnUiThread(() -> {
            Log.d("Stringee", "onVideoTrackAdded: " + stringeeRoom.getId() + " - " + stringeeVideoTrack.getId());
            if (!stringeeVideoTrack.getUserId().equals(stringeeClient.getUserId())) {
                subscribeTrack(stringeeVideoTrack);
            }
        });
    }

    @Override
    public void onVideoTrackRemoved(StringeeRoom stringeeRoom, StringeeVideoTrack stringeeVideoTrack) {
        Utils.runOnUiThread(() -> {
            Log.d("Stringee", "onVideoTrackRemoved: " + stringeeRoom.getId() + " - " + stringeeVideoTrack.getId());
            boolean isParticipantTrack = true;
            if (localVideoTrack != null && localVideoTrack.getLocalId().equals(stringeeVideoTrack.getLocalId())) {
                isParticipantTrack = false;
            } else if (localShareTrack != null && localShareTrack.getLocalId().equals(stringeeVideoTrack.getLocalId())) {
                isParticipantTrack = false;
            }

            if (isParticipantTrack) {
                stringeeRoom.unsubscribe(stringeeVideoTrack, new StatusListener() {
                    @Override
                    public void onSuccess() {
                        Log.d("Stringee", "Unsubscribe success: " + stringeeVideoTrack.getId());
                    }

                    @Override
                    public void onError(StringeeError errorInfo) {
                        super.onError(errorInfo);
                        Log.d("Stringee", "Unsubscribe error: " + stringeeVideoTrack.getId());
                    }
                });
                if (conferenceListener != null) {
                    conferenceListener.onTrackRemoved(stringeeVideoTrack);
                }
            }
        });
    }

    @Override
    public void onMessage(StringeeRoom stringeeRoom, JSONObject jsonObject, RemoteParticipant
            remoteParticipant) {
        Utils.runOnUiThread(() -> Log.d("Stringee", "onMessage: " + stringeeRoom.getId() + " - " + remoteParticipant.getId() + " - " + jsonObject.toString()));
    }

    @Override
    public void onVideoTrackNotification(RemoteParticipant
                                                 remoteParticipant, StringeeVideoTrack stringeeVideoTrack, StringeeVideoTrack.MediaType
                                                 mediaType) {
        Utils.runOnUiThread(() -> Log.d("Stringee", "onVideoTrackNotification: " + remoteParticipant.getId() + " - " + stringeeVideoTrack.getId() + " - " + mediaType));
    }
}
