package com.stringee.kotlin_onetoonecallsample.stringee.manager

import com.stringee.kotlin_onetoonecallsample.stringee.common.CallStatus
import org.junit.Assert
import org.junit.Test

class CallStateMachineTest {
    @Test
    fun answeredWithoutConnectedMediaRemainsStarting() {
        val stateMachine = CallStateMachine(CallStatus.CALLING)

        Assert.assertEquals(CallStatus.STARTING, stateMachine.onSignalingAnswered())
    }

    @Test
    fun connectedMediaWithoutAnswerRemainsStarting() {
        val stateMachine = CallStateMachine(CallStatus.CALLING)

        Assert.assertEquals(CallStatus.STARTING, stateMachine.onMediaConnected())
    }

    @Test
    fun answeredAndConnectedTransitionsToStartedInEitherOrder() {
        val signalingFirst = CallStateMachine(CallStatus.CALLING)
        signalingFirst.onSignalingAnswered()
        Assert.assertEquals(CallStatus.STARTED, signalingFirst.onMediaConnected())

        val mediaFirst = CallStateMachine(CallStatus.CALLING)
        mediaFirst.onMediaConnected()
        Assert.assertEquals(CallStatus.STARTED, mediaFirst.onSignalingAnswered())
    }

    @Test
    fun mediaLossAfterStartTransitionsToReconnecting() {
        val stateMachine = CallStateMachine(CallStatus.CALLING)
        stateMachine.onSignalingAnswered()
        stateMachine.onMediaConnected()

        Assert.assertEquals(CallStatus.RECONNECTING, stateMachine.onMediaDisconnected())
        Assert.assertEquals(CallStatus.STARTED, stateMachine.onMediaConnected())
    }
}
