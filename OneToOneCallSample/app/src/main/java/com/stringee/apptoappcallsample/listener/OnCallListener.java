package com.stringee.apptoappcallsample.listener;

import com.stringee.apptoappcallsample.common.CallStatus;

public interface OnCallListener {
    void onCallStatus(CallStatus status);

    void onError(String message);

    void onReceiveLocalStream();

    void onReceiveRemoteStream();

    void onSpeakerChange(boolean isOn);

    void onMicChange(boolean isOn);

    void onVideoChange(boolean isOn);

    void onTimer(String duration);
}
