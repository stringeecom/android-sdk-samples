package com.stringee.kotlin_onetoonecallsample.stringee.service

import com.stringee.kotlin_onetoonecallsample.stringee.service.CallServiceRetryPolicy.shouldRetry
import org.junit.Assert
import org.junit.Test

class CallServiceRetryPolicyTest {
    @Test
    fun retriesOnlyWhileForegroundAndSessionIsStillOwned() {
        Assert.assertTrue(shouldRetry(true, true, 0))
        Assert.assertFalse(shouldRetry(false, true, 0))
        Assert.assertFalse(shouldRetry(true, false, 0))
    }

    @Test
    fun retryCountIsBounded() {
        Assert.assertTrue(shouldRetry(true, true, 2))
        Assert.assertFalse(shouldRetry(true, true, 3))
    }
}
