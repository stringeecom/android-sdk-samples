package com.stringee.apptoappcallsample.stringee.listener;

import com.stringee.apptoappcallsample.stringee.common.CallStatus;
import com.stringee.apptoappcallsample.stringee.common.ConnectionState;
import com.stringee.exception.StringeeError;

/** Host-facing callbacks emitted by {@code StringeeCallManager}. */
public interface StringeeCallListener {
    /** Called when the Stringee connection state or connected user changes. */
    default void onConnectionStateChanged(ConnectionState state, String userId) {
    }

    /** Called when the active call state changes. */
    default void onCallStateChanged(CallStatus state) {
    }

    /** Called when a connection, call, push, or notification operation fails. */
    default void onError(String action, StringeeError error) {
    }

    /** Called when Stringee requests a refreshed access token. */
    default void onRequestNewToken() {
    }
}
