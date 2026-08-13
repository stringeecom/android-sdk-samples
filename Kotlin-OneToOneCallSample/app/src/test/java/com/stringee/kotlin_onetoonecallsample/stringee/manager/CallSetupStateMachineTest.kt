package com.stringee.kotlin_onetoonecallsample.stringee.manager

import org.junit.Assert
import org.junit.Test

class CallSetupStateMachineTest {
    @Test
    fun successRequiresANonEmptyServerCallId() {
        val stateMachine = CallSetupStateMachine()

        Assert.assertEquals(
            CallSetupStateMachine.SuccessResult.INVALID_SERVER_ID,
            stateMachine.onSuccess("")
        )
        Assert.assertEquals(CallSetupStateMachine.State.FAILED, stateMachine.state)
    }

    @Test
    fun successfulSetupStoresTheServerCallId() {
        val stateMachine = CallSetupStateMachine()

        Assert.assertEquals(
            CallSetupStateMachine.SuccessResult.ESTABLISHED,
            stateMachine.onSuccess("server-call-1")
        )
        Assert.assertEquals("server-call-1", stateMachine.serverCallId)
        Assert.assertTrue(stateMachine.hasServerCallId())
    }

    @Test
    fun successAfterCancellationMustBeHungUp() {
        val stateMachine = CallSetupStateMachine()

        Assert.assertTrue(stateMachine.cancel())
        Assert.assertEquals(
            CallSetupStateMachine.SuccessResult.LATE_SUCCESS,
            stateMachine.onSuccess("server-call-2")
        )
        Assert.assertEquals(CallSetupStateMachine.State.CANCELLED, stateMachine.state)
    }

    @Test
    fun onlyPendingSetupCanFailOrCancel() {
        val failed = CallSetupStateMachine()
        Assert.assertTrue(failed.fail())
        Assert.assertFalse(failed.fail())
        Assert.assertFalse(failed.cancel())

        val established = CallSetupStateMachine()
        established.onSuccess("server-call-3")
        Assert.assertFalse(established.fail())
        Assert.assertFalse(established.cancel())
    }
}
