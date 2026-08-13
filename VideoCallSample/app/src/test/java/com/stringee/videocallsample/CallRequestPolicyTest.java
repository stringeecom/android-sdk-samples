package com.stringee.videocallsample;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CallRequestPolicyTest {

    @Test
    public void disconnectedClientCannotStartCall() {
        assertEquals(CallRequestPolicy.Error.NOT_CONNECTED,
                CallRequestPolicy.validate(false, false, "bob"));
    }

    @Test
    public void secondCallIsRejectedWhileCallIsActive() {
        assertEquals(CallRequestPolicy.Error.CALL_IN_PROGRESS,
                CallRequestPolicy.validate(true, true, "bob"));
    }

    @Test
    public void blankRecipientIsRejected() {
        assertEquals(CallRequestPolicy.Error.RECIPIENT_REQUIRED,
                CallRequestPolicy.validate(true, false, "  "));
    }

    @Test
    public void validRecipientIsTrimmedAndAccepted() {
        assertNull(CallRequestPolicy.validate(true, false, "  bob  "));
        assertEquals("bob", CallRequestPolicy.normalizeRecipient("  bob  "));
    }
}
