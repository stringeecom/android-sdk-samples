package com.stringee.kotlin_onetoonecallsample.stringee.manager

import com.stringee.kotlin_onetoonecallsample.stringee.common.CallStatus

/** Combines signaling and media so STARTED is emitted only when both are connected. */
internal class CallStateMachine(var status: CallStatus) {
    private var answered = false
    private var mediaConnected = false
    private var startedOnce = false

    fun onSignalingAnswered(): CallStatus {
        answered = true
        return updateConnectedState()
    }

    fun onMediaConnected(): CallStatus {
        mediaConnected = true
        return updateConnectedState()
    }

    fun onMediaDisconnected(): CallStatus {
        mediaConnected = false
        if (startedOnce && answered) {
            status = CallStatus.RECONNECTING
        }
        return status
    }

    fun setStatus(newStatus: CallStatus): CallStatus {
        status = newStatus
        return status
    }

    private fun updateConnectedState(): CallStatus {
        if (answered && mediaConnected) {
            startedOnce = true
            status = CallStatus.STARTED
        } else {
            status = CallStatus.STARTING
        }
        return status
    }
}
