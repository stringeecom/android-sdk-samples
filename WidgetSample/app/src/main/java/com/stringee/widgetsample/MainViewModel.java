package com.stringee.widgetsample;

import android.content.Context;
import android.util.Log;
import android.widget.Button;

import androidx.databinding.BindingAdapter;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.stringee.call.StringeeCall;
import com.stringee.call.StringeeCall2;
import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.widget.StringeeListener;
import com.stringee.widget.StringeeWidget;
import com.stringee.widget.call.CallConfig;
import com.stringee.widgetsample.common.Constant;
import com.stringee.widgetsample.common.PrefUtils;
import com.stringee.widgetsample.common.Utils;

public class MainViewModel extends ViewModel {

    public enum MainState {
        CONNECTED,
        NOT_CONNECTED
    }

    private MutableLiveData<String> accessToken = new MutableLiveData<>();
    private MutableLiveData<String> to = new MutableLiveData<>();

    private MutableLiveData<String> from = new MutableLiveData<>();
    private final MutableLiveData<String> connectStatus = new MutableLiveData<>();
    private final MutableLiveData<Boolean> needRequestPermission = new MutableLiveData<>(false);
    private final MutableLiveData<MainState> mainState = new MutableLiveData<>(MainState.NOT_CONNECTED);
    private final MutableLiveData<String> msg = new MutableLiveData<>();

    public MutableLiveData<Boolean> getNeedRequestPermission() {
        return needRequestPermission;
    }

    public MutableLiveData<MainState> getMainState() {
        return mainState;
    }

    public MutableLiveData<String> getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(MutableLiveData<String> accessToken) {
        this.accessToken = accessToken;
    }

    public MutableLiveData<String> getTo() {
        return to;
    }

    public void setTo(MutableLiveData<String> to) {
        this.to = to;
    }

    public MutableLiveData<String> getFrom() {
        return from;
    }

    public void setFrom(MutableLiveData<String> from) {
        this.from = from;
    }

    public MutableLiveData<String> getMsg() {
        return msg;
    }

    public MutableLiveData<String> getConnectStatus() {
        return connectStatus;
    }

    @BindingAdapter("android:text")
    public static void setText(Button button, String text) {
        button.setText(text);
    }

    public void connect(Context context) {
        if (Utils.isStringEmpty(accessToken.getValue())) {
            msg.setValue("Access token is empty");
            return;
        }
        initStringee(context);
        StringeeWidget.getInstance(context).connect(accessToken.getValue());
    }

    private void initStringee(Context context) {
//        List<SocketAddress> socketAddressList = new ArrayList<>();
//        socketAddressList.add(new SocketAddress("your host", your_port);
//        StringeeWidget.getInstance(context).setHost(socketAddressList);
        StringeeWidget.getInstance(context).setListener(new StringeeListener() {
            @Override
            public void onConnectionConnected() {
                Utils.runOnUiThread(() -> {
                    String userId = StringeeWidget.getInstance(context).getClient().getUserId();
                    Log.d("Stringee", "onConnected: " + userId);
                    PrefUtils.getInstance(context).putString(Constant.PREF_TOKEN, accessToken.getValue());
                    connectStatus.setValue("Connected as " + userId);
                    mainState.setValue(MainState.CONNECTED);
                });
            }

            @Override
            public void onConnectionDisconnected() {
                Utils.runOnUiThread(() -> {
                    Log.d("Stringee", "Disconnected");
                    connectStatus.setValue("Disconnected");
                    mainState.setValue(MainState.CONNECTED);
                });
            }

            @Override
            public void onConnectionError(StringeeError stringeeError) {
                Utils.runOnUiThread(() -> {
                    Log.d("Stringee", "onConnectionError: " + stringeeError.getMessage());
                    connectStatus.setValue("Connection error: " + stringeeError.getMessage());
                    logout(context);
                });
            }

            @Override
            public void onRequestNewToken() {
                Utils.runOnUiThread(() -> {
                    Log.d("Stringee", "onRequestNewToken");
                    connectStatus.setValue("Request new token");
                });
            }

            @Override
            public void onCallStateChange(StringeeCall stringeeCall, StringeeCall.SignalingState signalingState) {
                Utils.runOnUiThread(() -> Log.d("Stringee", "onCallStateChange: " + signalingState.toString()));
            }

            @Override
            public void onCallStateChange2(StringeeCall2 stringeeCall2, StringeeCall2.SignalingState signalingState) {
                Utils.runOnUiThread(() -> Log.d("Stringee", "onCallStateChange2: " + signalingState.toString()));
            }
        });
    }

    public void makeCall(Context context, boolean isVideoCall) {
        if (Utils.isStringEmpty(accessToken.getValue())) {
            msg.setValue("Access token is empty");
            return;
        }

        if (Utils.isStringEmpty(to.getValue())) {
            msg.setValue("To is empty");
            return;
        }

        String fromNumber = from.getValue();
        if (Utils.isStringEmpty(fromNumber)) {
            fromNumber = StringeeWidget.getInstance(context).getClient().getUserId();
        }
        CallConfig config = new CallConfig(fromNumber, to.getValue());
        config.setVideoCall(isVideoCall);

        if (mainState.getValue() != MainState.CONNECTED) {
            initStringee(context);
            StringeeWidget.getInstance(context).makeCall(accessToken.getValue(), config, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        } else {
            StringeeWidget.getInstance(context).makeCall(config, new StatusListener() {
                @Override
                public void onSuccess() {

                }
            });
        }
    }

    public void logout(Context context) {
        StringeeWidget.getInstance(context).finalize();
        PrefUtils.getInstance(context).clearData();
        mainState.setValue(MainState.NOT_CONNECTED);
    }
}