package com.stringee.kotlin_onetoonecallsample.stringee.listener

import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallStatus
import com.stringee.kotlin_onetoonecallsample.stringee.common.ConnectionState

/** Host-facing callbacks emitted by the public Stringee call facade. */
interface StringeeCallListener {
    /** Called when the Stringee connection state or connected user changes. */
    fun onConnectionStateChanged(state: ConnectionState, userId: String) {
    }

    /** Called when the active call state changes. */
    fun onCallStateChanged(state: CallStatus) {
    }

    /** Called when a connection, call, push, or notification operation fails. */
    fun onError(action: String, error: StringeeError) {
    }

    /** Called when Stringee requests a refreshed access token. */
    fun onRequestNewToken() {
    }
}
