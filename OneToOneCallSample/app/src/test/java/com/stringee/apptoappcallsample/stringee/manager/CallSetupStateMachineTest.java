package com.stringee.apptoappcallsample.stringee.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CallSetupStateMachineTest {
    @Test
    public void successRequiresANonEmptyServerCallId() {
        CallSetupStateMachine stateMachine = new CallSetupStateMachine();

        assertEquals(CallSetupStateMachine.SuccessResult.INVALID_SERVER_ID,
                stateMachine.onSuccess(""));
        assertEquals(CallSetupStateMachine.State.FAILED, stateMachine.getState());
    }

    @Test
    public void successfulSetupStoresTheServerCallId() {
        CallSetupStateMachine stateMachine = new CallSetupStateMachine();

        assertEquals(CallSetupStateMachine.SuccessResult.ESTABLISHED,
                stateMachine.onSuccess("server-call-1"));
        assertEquals("server-call-1", stateMachine.getServerCallId());
        assertTrue(stateMachine.hasServerCallId());
    }

    @Test
    public void successAfterCancellationMustBeHungUp() {
        CallSetupStateMachine stateMachine = new CallSetupStateMachine();

        assertTrue(stateMachine.cancel());
        assertEquals(CallSetupStateMachine.SuccessResult.LATE_SUCCESS,
                stateMachine.onSuccess("server-call-2"));
        assertEquals(CallSetupStateMachine.State.CANCELLED, stateMachine.getState());
    }

    @Test
    public void onlyPendingSetupCanFailOrCancel() {
        CallSetupStateMachine failed = new CallSetupStateMachine();
        assertTrue(failed.fail());
        assertFalse(failed.fail());
        assertFalse(failed.cancel());

        CallSetupStateMachine established = new CallSetupStateMachine();
        established.onSuccess("server-call-3");
        assertFalse(established.fail());
        assertFalse(established.cancel());
    }
}
