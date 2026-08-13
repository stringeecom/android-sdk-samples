package com.stringee.kotlin_onetoonecallsample.stringee.manager

/** Tracks asynchronous outgoing setup, cancellation, and late SDK success callbacks. */
internal class CallSetupStateMachine {
    internal enum class State {
        PENDING,
        ESTABLISHED,
        FAILED,
        CANCELLED
    }

    internal enum class SuccessResult {
        ESTABLISHED,
        INVALID_SERVER_ID,
        LATE_SUCCESS,
        IGNORED
    }

    @get:Synchronized
    var state: State = State.PENDING
        private set

    @get:Synchronized
    var serverCallId: String = ""
        private set

    @Synchronized
    fun onSuccess(callId: String): SuccessResult {
        if (state == State.PENDING) {
            if (isEmpty(callId)) {
                state = State.FAILED
                return SuccessResult.INVALID_SERVER_ID
            }
            serverCallId = callId
            state = State.ESTABLISHED
            return SuccessResult.ESTABLISHED
        }
        if ((state == State.CANCELLED || state == State.FAILED) && !isEmpty(callId)) {
            serverCallId = callId
            return SuccessResult.LATE_SUCCESS
        }
        return SuccessResult.IGNORED
    }

    @Synchronized
    fun establishIncoming(callId: String): Boolean {
        if (isEmpty(callId)) {
            return false
        }
        serverCallId = callId
        state = State.ESTABLISHED
        return true
    }

    @Synchronized
    fun fail(): Boolean {
        if (state != State.PENDING) {
            return false
        }
        state = State.FAILED
        return true
    }

    @Synchronized
    fun cancel(): Boolean {
        if (state != State.PENDING) {
            return false
        }
        state = State.CANCELLED
        return true
    }

    @get:Synchronized
    val isPending: Boolean
        get() = state == State.PENDING

    @Synchronized
    fun hasServerCallId(): Boolean {
        return !isEmpty(serverCallId)
    }

    companion object {
        private fun isEmpty(value: String?): Boolean {
            return value == null || value.trim { it <= ' ' }.isEmpty()
        }
    }
}
