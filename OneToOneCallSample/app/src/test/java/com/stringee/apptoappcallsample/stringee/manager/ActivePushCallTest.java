package com.stringee.apptoappcallsample.stringee.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

public class ActivePushCallTest {
    @After
    public void tearDown() {
        ActivePushCall.clear();
    }

    @Test
    public void terminalPushOnlyClearsMatchingCall() {
        assertTrue(ActivePushCall.claim("call-1"));

        assertFalse(ActivePushCall.clearIfMatches("call-2"));
        assertTrue(ActivePushCall.isCurrent("call-1"));
        assertTrue(ActivePushCall.clearIfMatches("call-1"));
        assertFalse(ActivePushCall.hasActiveCall());
    }

    @Test
    public void pendingActionRequiresMatchingGenerationAndIsConsumedOnce() {
        assertTrue(ActivePushCall.claim("call-1"));
        long generation = ActivePushCall.getGeneration();

        assertFalse(ActivePushCall.requestAction(
                "call-1", generation - 1, ActivePushCall.PendingAction.ANSWER));
        assertTrue(ActivePushCall.requestAction(
                "call-1", generation, ActivePushCall.PendingAction.ANSWER));
        assertEquals(ActivePushCall.PendingAction.ANSWER,
                ActivePushCall.consumeAction("call-1"));
        assertEquals(ActivePushCall.PendingAction.NONE,
                ActivePushCall.consumeAction("call-1"));
    }

    @Test
    public void rejectFromAnOldGenerationCannotAffectANewCall() {
        assertTrue(ActivePushCall.claim("call-1"));
        long oldGeneration = ActivePushCall.getGeneration();
        assertTrue(ActivePushCall.clearIfMatches("call-1"));
        assertTrue(ActivePushCall.claim("call-2"));

        assertFalse(ActivePushCall.requestAction("call-2", oldGeneration,
                ActivePushCall.PendingAction.REJECT));
        assertEquals(ActivePushCall.PendingAction.NONE,
                ActivePushCall.consumeAction("call-2"));
    }
}
