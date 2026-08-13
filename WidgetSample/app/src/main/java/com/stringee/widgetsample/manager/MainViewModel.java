package com.stringee.widgetsample.manager;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.stringee.exception.StringeeError;
import com.stringee.listener.StatusListener;
import com.stringee.widgetsample.listener.WidgetCallListener;

/**
 * Presentation model for {@code MainActivity}.
 *
 * <p>It exposes observable form and connection state while delegating SDK work to
 * {@link WidgetCallCoordinator}.</p>
 */
public class MainViewModel extends ViewModel {
    /** Connection states rendered by the host screen. */
    public enum MainState { NOT_CONNECTED, CONNECTING, CONNECTED, ERROR }

    private final MutableLiveData<String> accessToken = new MutableLiveData<>("");
    private final MutableLiveData<String> to = new MutableLiveData<>("");
    private final MutableLiveData<String> from = new MutableLiveData<>("");
    private final MutableLiveData<String> connectStatus = new MutableLiveData<>("Disconnected");
    private final MutableLiveData<MainState> mainState =
            new MutableLiveData<>(MainState.NOT_CONNECTED);
    private final MutableLiveData<String> msg = new MutableLiveData<>();
    private WidgetCallCoordinator coordinator;

    public MutableLiveData<MainState> getMainState() { return mainState; }
    public MutableLiveData<String> getAccessToken() { return accessToken; }
    public MutableLiveData<String> getTo() { return to; }
    public MutableLiveData<String> getFrom() { return from; }
    public MutableLiveData<String> getMsg() { return msg; }
    public MutableLiveData<String> getConnectStatus() { return connectStatus; }

    /** Initializes the coordinator and restores the saved access token into the form. */
    public void initialize(Context context) {
        coordinator = WidgetCallCoordinator.getInstance(context);
        accessToken.setValue(coordinator.getSavedToken());
        coordinator.initialize(new WidgetCallListener() {
            @Override
            public void onConnectionChanged(WidgetCallCoordinator.ConnectionState state,
                                            String userId) {
                switch (state) {
                    case CONNECTED:
                        mainState.postValue(MainState.CONNECTED);
                        connectStatus.postValue("Connected as " + userId);
                        break;
                    case CONNECTING:
                        mainState.postValue(MainState.CONNECTING);
                        connectStatus.postValue("Connecting");
                        break;
                    case ERROR:
                        mainState.postValue(MainState.ERROR);
                        connectStatus.postValue("Connection error");
                        break;
                    default:
                        mainState.postValue(MainState.NOT_CONNECTED);
                        connectStatus.postValue("Disconnected");
                }
            }

            @Override
            public void onMessage(String message) {
                msg.postValue(message);
            }
        });
    }

    /** Connects with the token currently entered by the user. */
    public void connect() {
        if (coordinator != null) {
            coordinator.connect(accessToken.getValue());
        }
    }

    /** Disconnects the current Widget session. */
    public void disconnect() {
        if (coordinator != null) {
            coordinator.disconnect();
        }
    }

    /** Starts an outgoing voice or video call using the current form values. */
    public void makeCall(boolean videoCall) {
        if (coordinator == null) {
            return;
        }
        coordinator.makeCall(from.getValue(), to.getValue(), videoCall, new StatusListener() {
            @Override public void onSuccess() { }
            @Override public void onError(StringeeError error) {
                msg.postValue(error.getMessage());
            }
        });
    }
}
