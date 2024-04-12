package com.stringee.video_conference_sample.stringee_wrapper.ui.activity.view_model;

import android.content.Context;
import android.content.Intent;
import android.widget.FrameLayout;

import androidx.lifecycle.MutableLiveData;

import com.stringee.video.StringeeVideoTrack;
import com.stringee.video.TextureViewRenderer;
import com.stringee.video_conference_sample.stringee_wrapper.ui.activity.ConferenceActivity;
import com.stringee.video_conference_sample.stringee_wrapper.ui.base.MyViewModel;
import com.stringee.video_conference_sample.stringee_wrapper.wrapper.StringeeWrapper;

public class CheckDeviceViewModel extends MyViewModel {
    private final MutableLiveData<String> roomName = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isVideoOn = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> isMicOn = new MutableLiveData<>(true);

    public MutableLiveData<String> getRoomName() {
        return roomName;
    }

    public MutableLiveData<Boolean> getIsVideoOn() {
        return isVideoOn;
    }

    public MutableLiveData<Boolean> getIsMicOn() {
        return isMicOn;
    }

    public void enableMic(Context context) {
        boolean isOn = Boolean.FALSE.equals(isMicOn.getValue());
        StringeeWrapper.getInstance(context).getConferenceWrapper().enableMic(isOn);
        isMicOn.setValue(isOn);
    }

    public void enableCamera(Context context) {
        boolean isOn = Boolean.FALSE.equals(isVideoOn.getValue());
        StringeeWrapper.getInstance(context).getConferenceWrapper().enableVideo(isOn);
        isVideoOn.setValue(isOn);
    }

    public void switchCamera(Context context) {
        StringeeWrapper.getInstance(context).getConferenceWrapper().switchCamera();
    }

    public void start(Context context) {
        Intent intent = new Intent(context, ConferenceActivity.class);
        intent.putExtra("is_video_on", isVideoOn.getValue());
        intent.putExtra("is_mic_on", isMicOn.getValue());
        context.startActivity(intent);
    }

    public void releaseTrack(Context context) {
        StringeeWrapper.getInstance(context).getConferenceWrapper().release();
    }

    public void displayLocalTrack(Context context, FrameLayout flPreview) {
        StringeeWrapper.getInstance(context).getConferenceWrapper().displayLocalTrack(flPreview);
    }
}