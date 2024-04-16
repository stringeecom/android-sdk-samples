package com.stringee.video_conference_sample.main;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.stringee.exception.StringeeError;
import com.stringee.messaging.listeners.CallbackListener;
import com.stringee.video_conference_sample.stringee_wrapper.common.PermissionsUtils;
import com.stringee.video_conference_sample.stringee_wrapper.common.TokenUtils;
import com.stringee.video_conference_sample.stringee_wrapper.common.Utils;
import com.stringee.video_conference_sample.stringee_wrapper.ui.activity.CheckDeviceActivity;
import com.stringee.video_conference_sample.stringee_wrapper.ui.base.MyViewModel;
import com.stringee.video_conference_sample.stringee_wrapper.wrapper.StringeeWrapper;
import com.stringee.video_conference_sample.stringee_wrapper.wrapper.listener.ConnectionListener;

public class MainViewModel extends MyViewModel {

    public enum MainState {
        CONNECT_CLIENT,
        CONNECT_ROOM
    }

    private String userId;
    private String roomName;
    private MutableLiveData<String> roomToken = new MutableLiveData<>();
    private final MutableLiveData<String> connectStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> needRequestPermission = new MutableLiveData<>(false);
    private final MutableLiveData<MainState> mainState = new MutableLiveData<>(MainState.CONNECT_CLIENT);
    private final MutableLiveData<String> msg = new MutableLiveData<>();

    public MutableLiveData<Boolean> getNeedRequestPermission() {
        return needRequestPermission;
    }

    public MutableLiveData<MainState> getMainState() {
        return mainState;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public MutableLiveData<String> getRoomToken() {
        return roomToken;
    }

    public void setRoomToken(MutableLiveData<String> roomToken) {
        this.roomToken = roomToken;
    }

    public MutableLiveData<String> getMsg() {
        return msg;
    }

    public MutableLiveData<String> getConnectStatus() {
        return connectStatus;
    }

    public void connectClient(Context context) {
        if (Utils.isStringEmpty(userId)) {
            msg.setValue("Please enter user id");
            return;
        }
        String accessToken = TokenUtils.getInstance().genAccessToken(userId);
        if (Utils.isStringEmpty(accessToken)) {
            msg.setValue("Cannot generate access token");
            return;
        }

        StringeeWrapper.getInstance(context).setConnectionListener(new ConnectionListener() {
            @Override
            public void onConnected(String userId) {
                Utils.runOnUiThread(() -> {
                    Log.d("Stringee", "onConnected: "+ userId);
                    connectStatus.setValue("Connected as " + userId);
                });
            }

            @Override
            public void onDisconnected() {
                Utils.runOnUiThread(() -> {
                    Log.d("Stringee", "Disconnected");
                    connectStatus.setValue("Disconnected");
                });
            }

            @Override
            public void onConnectionError(String error) {
                Utils.runOnUiThread(() -> {
                    Log.d("Stringee", "onConnectionError: " + error);
                    connectStatus.setValue("Connection error: " + error);
                });
            }

            @Override
            public void onRequestNewToken() {
                Utils.runOnUiThread(() -> {
                    Log.d("Stringee", "onRequestNewToken");
                    connectStatus.setValue("Request new token");
                });
            }
        });
        StringeeWrapper.getInstance(context).connect(accessToken);
        mainState.setValue(MainState.CONNECT_ROOM);
    }

    public void createRoom(Context context) {
        if (!StringeeWrapper.getInstance(context).isConnected()) {
            msg.setValue("Please disconnect before creating room");
            return;
        }
        roomName = "android-" + System.currentTimeMillis();
        TokenUtils.getInstance().createRoom(context, roomName, new CallbackListener<String>() {
            @Override
            public void onSuccess(String s) {
                roomToken.setValue(s);
            }

            @Override
            public void onError(StringeeError stringeeError) {
                super.onError(stringeeError);
                msg.setValue("Failed to create room");
            }
        });
    }

    public void connectRoom(Context context) {
        if (!StringeeWrapper.getInstance(context).isConnected()) {
            msg.setValue("Please disconnect before connecting room");
            return;
        }

        if (Utils.isStringEmpty(roomToken.getValue())) {
            msg.setValue("Room token is empty");
            return;
        }

        if (!PermissionsUtils.getInstance().checkSelfConferencePermission(context)) {
            needRequestPermission.setValue(true);
            return;
        }

        needRequestPermission.setValue(false);
        StringeeWrapper.getInstance(context).createConferenceWrapper(roomToken.getValue());
        Intent intent = new Intent(context, CheckDeviceActivity.class);
        intent.putExtra("room_name", roomName);
        context.startActivity(intent);
    }

    public void backPress(Context context) {
        StringeeWrapper.getInstance(context).release();
        mainState.setValue(MainState.CONNECT_CLIENT);
        roomToken.setValue(null);
    }
}