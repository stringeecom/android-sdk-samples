package com.stringee.kotlin_onetoonecallsample.stringee.service

import org.junit.Assert
import org.junit.Test

class CallServiceOwnershipTest {
    @Test
    fun staleSessionCannotReplaceOrStopCurrentService() {
        val ownership = CallServiceOwnership()

        Assert.assertTrue(ownership.request(10))
        Assert.assertTrue(ownership.isRequested(10))
        Assert.assertTrue(ownership.markRunning(10))
        Assert.assertTrue(ownership.isRunning(10))
        Assert.assertFalse(ownership.request(11))
        Assert.assertFalse(ownership.release(11))
        Assert.assertTrue(ownership.isOwnedBy(10))
    }

    @Test
    fun releasedSessionAllowsTheNextSessionToOwnService() {
        val ownership = CallServiceOwnership()

        Assert.assertTrue(ownership.request(10))
        Assert.assertTrue(ownership.markRunning(10))
        Assert.assertTrue(ownership.release(10))
        Assert.assertTrue(ownership.request(11))
        Assert.assertTrue(ownership.markRunning(11))
        Assert.assertTrue(ownership.isOwnedBy(11))
    }

    @Test
    fun markRunningRejectsAnUnrequestedGeneration() {
        val ownership = CallServiceOwnership()

        Assert.assertFalse(ownership.markRunning(3))
        Assert.assertFalse(ownership.isOwnedBy(3))
    }
}
