package com.stringee.kotlin_onetoonecallsample.stringee.receiver

import com.stringee.kotlin_onetoonecallsample.stringee.receiver.CallActionOwnership.canTargetActiveSession
import org.junit.Assert
import org.junit.Test

class CallActionOwnershipTest {
    @Test
    fun provisionalPushRejectTargetsSdkCallOnceItIsAvailable() {
        Assert.assertTrue(
            canTargetActiveSession(
                true, true, -1, 12
            )
        )
    }

    @Test
    fun sdkNotificationMustMatchTheCurrentSessionGeneration() {
        Assert.assertTrue(
            canTargetActiveSession(
                true, true, 12, 12
            )
        )
        Assert.assertFalse(
            canTargetActiveSession(
                true, true, 11, 12
            )
        )
    }

    @Test
    fun actionCannotTargetSessionWithoutPushOwnershipOrActiveCall() {
        Assert.assertFalse(
            canTargetActiveSession(
                false, true, -1, 12
            )
        )
        Assert.assertFalse(
            canTargetActiveSession(
                true, false, -1, 12
            )
        )
    }
}
