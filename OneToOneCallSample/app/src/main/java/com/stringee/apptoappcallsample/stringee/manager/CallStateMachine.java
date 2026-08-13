package com.stringee.apptoappcallsample.stringee.manager;

import com.stringee.apptoappcallsample.stringee.common.CallStatus;

/** Combines signaling and media state so STARTED is emitted only when both are connected. */
final class CallStateMachine {
    private CallStatus status;
    private boolean answered;
    private boolean mediaConnected;
    private boolean startedOnce;

    CallStateMachine(CallStatus initialStatus) {
        status = initialStatus;
    }

    CallStatus onSignalingAnswered() {
        answered = true;
        return updateConnectedState();
    }

    CallStatus onMediaConnected() {
        mediaConnected = true;
        return updateConnectedState();
    }

    CallStatus onMediaDisconnected() {
        mediaConnected = false;
        if (startedOnce && answered) {
            status = CallStatus.RECONNECTING;
        }
        return status;
    }

    CallStatus setStatus(CallStatus newStatus) {
        status = newStatus;
        return status;
    }

    CallStatus getStatus() {
        return status;
    }

    private CallStatus updateConnectedState() {
        if (answered && mediaConnected) {
            startedOnce = true;
            status = CallStatus.STARTED;
        } else {
            status = CallStatus.STARTING;
        }
        return status;
    }
}
