package com.stringee.video_conference_sample.stringee_wrapper.wrapper.listener;

import com.stringee.video.StringeeVideoTrack;

public interface ConferenceListener {
    void onTrackAdded(StringeeVideoTrack stringeeVideoTrack);

    void onTrackRemoved(StringeeVideoTrack stringeeVideoTrack);

    void onLeaveRoom();
    void onSharingScreen(boolean onSharing);
}
