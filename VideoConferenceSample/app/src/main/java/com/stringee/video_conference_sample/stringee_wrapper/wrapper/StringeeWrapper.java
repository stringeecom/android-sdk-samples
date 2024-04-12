package com.stringee.video_conference_sample.stringee_wrapper.wrapper;

import android.content.Context;

import com.stringee.StringeeClient;
import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StringeeConnectionListener;
import com.stringee.video_conference_sample.stringee_wrapper.common.Utils;
import com.stringee.video_conference_sample.stringee_wrapper.wrapper.listener.ConnectionListener;

import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class StringeeWrapper {
    private static volatile StringeeWrapper instance;
    private final Context context;
    private ConnectionListener connectionListener;
    private StringeeClient stringeeClient;
    private ConferenceWrapper conferenceWrapper;
    private final ScheduledExecutorService executor;

    public void setConnectionListener(ConnectionListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    public StringeeWrapper(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    public static StringeeWrapper getInstance(Context context) {
        if (instance == null) {
            synchronized (StringeeWrapper.class) {
                if (instance == null) {
                    instance = new StringeeWrapper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public ScheduledExecutorService getExecutor() {
        return executor;
    }

    public void connect(String token) {
        Utils.runOnUiThread(() -> {
            if (stringeeClient == null) {
                stringeeClient = new StringeeClient(context);
                stringeeClient.setConnectionListener(new StringeeConnectionListener() {
                    @Override
                    public void onConnectionConnected(StringeeClient stringeeClient, boolean b) {
                        if (connectionListener != null) {
                            connectionListener.onConnected(stringeeClient.getUserId());
                        }
                    }

                    @Override
                    public void onConnectionDisconnected(StringeeClient stringeeClient, boolean b) {
                        if (connectionListener != null) {
                            connectionListener.onDisconnected();
                        }
                    }

                    @Override
                    public void onIncomingCall(StringeeCall stringeeCall) {

                    }

                    @Override
                    public void onIncomingCall2(StringeeCall2 stringeeCall2) {

                    }

                    @Override
                    public void onConnectionError(StringeeClient stringeeClient, StringeeError stringeeError) {
                        if (connectionListener != null) {
                            connectionListener.onConnectionError(stringeeError.getMessage());
                        }
                    }

                    @Override
                    public void onRequestNewToken(StringeeClient stringeeClient) {
                        if (connectionListener != null) {
                            connectionListener.onRequestNewToken();
                        }
                    }

                    @Override
                    public void onCustomMessage(String s, JSONObject jsonObject) {

                    }

                    @Override
                    public void onTopicMessage(String s, JSONObject jsonObject) {

                    }
                });
            }
            if (stringeeClient != null) {
                stringeeClient.connect(token);
            }
        });
    }

    public void release() {
        if (stringeeClient != null) {
            stringeeClient.disconnect();
        }
        conferenceWrapper = null;
        stringeeClient = null;
    }

    public ConferenceWrapper getConferenceWrapper() {
        return conferenceWrapper;
    }

    public void createConferenceWrapper(String roomToken) {
        conferenceWrapper = ConferenceWrapper.create(context, stringeeClient, roomToken);
    }

    public void releaseRoom() {
        if (conferenceWrapper == null) {
            return;
        }
        conferenceWrapper = null;
    }
}
