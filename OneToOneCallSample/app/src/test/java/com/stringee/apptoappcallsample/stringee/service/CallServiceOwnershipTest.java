package com.stringee.apptoappcallsample.stringee.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CallServiceOwnershipTest {
    @Test
    public void staleSessionCannotReplaceOrStopCurrentService() {
        CallServiceOwnership ownership = new CallServiceOwnership();

        assertTrue(ownership.request(10));
        assertTrue(ownership.isRequested(10));
        assertTrue(ownership.markRunning(10));
        assertTrue(ownership.isRunning(10));
        assertFalse(ownership.request(11));
        assertFalse(ownership.release(11));
        assertTrue(ownership.isOwnedBy(10));
    }

    @Test
    public void releasedSessionAllowsTheNextSessionToOwnService() {
        CallServiceOwnership ownership = new CallServiceOwnership();

        assertTrue(ownership.request(10));
        assertTrue(ownership.markRunning(10));
        assertTrue(ownership.release(10));
        assertTrue(ownership.request(11));
        assertTrue(ownership.markRunning(11));
        assertTrue(ownership.isOwnedBy(11));
    }

    @Test
    public void markRunningRejectsAnUnrequestedGeneration() {
        CallServiceOwnership ownership = new CallServiceOwnership();

        assertFalse(ownership.markRunning(3));
        assertFalse(ownership.isOwnedBy(3));
    }
}
