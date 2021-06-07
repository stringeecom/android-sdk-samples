package com.stringee.videoconference.sample;

import android.widget.FrameLayout;

import com.stringee.video.StringeeVideoTrack;

public class VideoTrack {
    private StringeeVideoTrack stringeeVideoTrack;
    private FrameLayout layout;

    public VideoTrack() {
    }

    public VideoTrack(StringeeVideoTrack stringeeVideoTrack, FrameLayout layout) {
        this.stringeeVideoTrack = stringeeVideoTrack;
        this.layout = layout;
    }

    public StringeeVideoTrack getStringeeVideoTrack() {
        return stringeeVideoTrack;
    }

    public void setStringeeVideoTrack(StringeeVideoTrack stringeeVideoTrack) {
        this.stringeeVideoTrack = stringeeVideoTrack;
    }

    public FrameLayout getLayout() {
        return layout;
    }

    public void setLayout(FrameLayout layout) {
        this.layout = layout;
    }
}
