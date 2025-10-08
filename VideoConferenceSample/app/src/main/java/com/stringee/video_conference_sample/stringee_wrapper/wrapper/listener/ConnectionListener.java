package com.stringee.video_conference_sample.stringee_wrapper.wrapper.listener;

public interface ConnectionListener {
    void onConnected(String userId);

    void onDisconnected();

    void onConnectionError(String error);

    void onRequestNewToken();
}
