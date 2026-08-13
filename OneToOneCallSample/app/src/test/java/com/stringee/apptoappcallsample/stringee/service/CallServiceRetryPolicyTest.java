package com.stringee.apptoappcallsample.stringee.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CallServiceRetryPolicyTest {
    @Test
    public void retriesOnlyWhileForegroundAndSessionIsStillOwned() {
        assertTrue(CallServiceRetryPolicy.shouldRetry(true, true, 0));
        assertFalse(CallServiceRetryPolicy.shouldRetry(false, true, 0));
        assertFalse(CallServiceRetryPolicy.shouldRetry(true, false, 0));
    }

    @Test
    public void retryCountIsBounded() {
        assertTrue(CallServiceRetryPolicy.shouldRetry(true, true, 2));
        assertFalse(CallServiceRetryPolicy.shouldRetry(true, true, 3));
    }
}
