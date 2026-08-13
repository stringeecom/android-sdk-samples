package com.stringee.apptoappcallsample.stringee.manager;

import static org.junit.Assert.assertEquals;

import com.stringee.apptoappcallsample.stringee.common.CallStatus;

import org.junit.Test;

public class CallStateMachineTest {
    @Test
    public void answeredWithoutConnectedMediaRemainsStarting() {
        CallStateMachine stateMachine = new CallStateMachine(CallStatus.CALLING);

        assertEquals(CallStatus.STARTING, stateMachine.onSignalingAnswered());
    }

    @Test
    public void connectedMediaWithoutAnswerRemainsStarting() {
        CallStateMachine stateMachine = new CallStateMachine(CallStatus.CALLING);

        assertEquals(CallStatus.STARTING, stateMachine.onMediaConnected());
    }

    @Test
    public void answeredAndConnectedTransitionsToStartedInEitherOrder() {
        CallStateMachine signalingFirst = new CallStateMachine(CallStatus.CALLING);
        signalingFirst.onSignalingAnswered();
        assertEquals(CallStatus.STARTED, signalingFirst.onMediaConnected());

        CallStateMachine mediaFirst = new CallStateMachine(CallStatus.CALLING);
        mediaFirst.onMediaConnected();
        assertEquals(CallStatus.STARTED, mediaFirst.onSignalingAnswered());
    }

    @Test
    public void mediaLossAfterStartTransitionsToReconnecting() {
        CallStateMachine stateMachine = new CallStateMachine(CallStatus.CALLING);
        stateMachine.onSignalingAnswered();
        stateMachine.onMediaConnected();

        assertEquals(CallStatus.RECONNECTING, stateMachine.onMediaDisconnected());
        assertEquals(CallStatus.STARTED, stateMachine.onMediaConnected());
    }
}
