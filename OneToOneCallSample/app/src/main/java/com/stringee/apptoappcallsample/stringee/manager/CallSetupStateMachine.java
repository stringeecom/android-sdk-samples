package com.stringee.apptoappcallsample.stringee.manager;

/** Tracks asynchronous outgoing setup, cancellation, and late SDK success callbacks. */
final class CallSetupStateMachine {
    enum State {
        PENDING,
        ESTABLISHED,
        FAILED,
        CANCELLED
    }

    enum SuccessResult {
        ESTABLISHED,
        INVALID_SERVER_ID,
        LATE_SUCCESS,
        IGNORED
    }

    private State state = State.PENDING;
    private String serverCallId = "";

    synchronized SuccessResult onSuccess(String callId) {
        if (state == State.PENDING) {
            if (isEmpty(callId)) {
                state = State.FAILED;
                return SuccessResult.INVALID_SERVER_ID;
            }
            serverCallId = callId;
            state = State.ESTABLISHED;
            return SuccessResult.ESTABLISHED;
        }
        if ((state == State.CANCELLED || state == State.FAILED) && !isEmpty(callId)) {
            serverCallId = callId;
            return SuccessResult.LATE_SUCCESS;
        }
        return SuccessResult.IGNORED;
    }

    synchronized boolean establishIncoming(String callId) {
        if (isEmpty(callId)) {
            return false;
        }
        serverCallId = callId;
        state = State.ESTABLISHED;
        return true;
    }

    synchronized boolean fail() {
        if (state != State.PENDING) {
            return false;
        }
        state = State.FAILED;
        return true;
    }

    synchronized boolean cancel() {
        if (state != State.PENDING) {
            return false;
        }
        state = State.CANCELLED;
        return true;
    }

    synchronized boolean isPending() {
        return state == State.PENDING;
    }

    synchronized boolean hasServerCallId() {
        return !isEmpty(serverCallId);
    }

    synchronized String getServerCallId() {
        return serverCallId;
    }

    synchronized State getState() {
        return state;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
