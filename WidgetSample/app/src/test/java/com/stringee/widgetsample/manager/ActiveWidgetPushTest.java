package com.stringee.widgetsample.manager;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ActiveWidgetPushTest {
    @After
    public void tearDown() {
        ActiveWidgetPush.clear();
    }

    @Test
    public void startedPushClaimsOneCallAndKeepsGenerationForDuplicate() {
        assertTrue(ActiveWidgetPush.claim("call-a", "100", "Alice", true));
        ActiveWidgetPush.Snapshot first = ActiveWidgetPush.snapshot();
        assertNotNull(first);

        assertTrue(ActiveWidgetPush.claim("call-a", "changed", "Changed", false));
        ActiveWidgetPush.Snapshot duplicate = ActiveWidgetPush.snapshot();
        assertNotNull(duplicate);
        assertEquals(first.generation, duplicate.generation);
        assertEquals("100", duplicate.from);
        assertTrue(duplicate.video);
    }

    @Test
    public void secondCallCannotReplaceOwnedIncomingCall() {
        assertTrue(ActiveWidgetPush.claim("call-a", "100", "Alice", false));
        assertFalse(ActiveWidgetPush.claim("call-b", "200", "Bob", true));
        assertEquals("call-a", ActiveWidgetPush.snapshot().callId);
    }

    @Test
    public void terminalPushOnlyClearsMatchingCall() {
        assertTrue(ActiveWidgetPush.claim("call-a", "100", "Alice", false));
        assertFalse(ActiveWidgetPush.clearIfMatches("old-call"));
        assertNotNull(ActiveWidgetPush.snapshot());
        assertTrue(ActiveWidgetPush.clearIfMatches("call-a"));
        assertNull(ActiveWidgetPush.snapshot());
    }

    @Test
    public void newOwnershipGetsNewGenerationAfterCleanup() {
        ActiveWidgetPush.claim("call-a", "100", "Alice", false);
        long firstGeneration = ActiveWidgetPush.snapshot().generation;
        ActiveWidgetPush.clearIfMatches("call-a");
        ActiveWidgetPush.claim("call-b", "200", "Bob", false);
        assertTrue(ActiveWidgetPush.snapshot().generation > firstGeneration);
    }
}
