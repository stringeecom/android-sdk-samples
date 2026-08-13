package com.stringee.apptoappcallsample.stringee.listener;

import com.stringee.apptoappcallsample.stringee.common.CallStatus;
import com.stringee.common.StringeeAudioManager.AudioDevice;
import com.stringee.video.StringeeVideoTrack;

import java.util.Set;

/** Internal callbacks used to render an owned {@code CallSession} in the call activity. */
public interface CallUiListener {
    void onCallStatus(CallStatus status);

    void onError(String message);

    void onLocalVideoAvailable();

    void onRemoteVideoAvailable();

    void onAudioDeviceChanged(AudioDevice selected, Set<AudioDevice> available);

    void onMicChanged(boolean enabled);

    void onVideoChanged(boolean enabled);

    void onSharingChanged(boolean sharing);

    void onTimer(String duration);

    void onScreenTrackAdded(StringeeVideoTrack track);

    void onScreenTrackRemoved(StringeeVideoTrack track);
}
