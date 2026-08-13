package com.stringee.apptoappcallsample.stringee.receiver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CallActionOwnershipTest {
    @Test
    public void provisionalPushRejectTargetsSdkCallOnceItIsAvailable() {
        assertTrue(CallActionOwnership.canTargetActiveSession(
                true, true, -1, 12));
    }

    @Test
    public void sdkNotificationMustMatchTheCurrentSessionGeneration() {
        assertTrue(CallActionOwnership.canTargetActiveSession(
                true, true, 12, 12));
        assertFalse(CallActionOwnership.canTargetActiveSession(
                true, true, 11, 12));
    }

    @Test
    public void actionCannotTargetSessionWithoutPushOwnershipOrActiveCall() {
        assertFalse(CallActionOwnership.canTargetActiveSession(
                false, true, -1, 12));
        assertFalse(CallActionOwnership.canTargetActiveSession(
                true, false, -1, 12));
    }
}
