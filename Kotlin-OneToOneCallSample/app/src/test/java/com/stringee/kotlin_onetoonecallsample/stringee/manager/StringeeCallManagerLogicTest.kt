package com.stringee.kotlin_onetoonecallsample.stringee.manager

import com.stringee.kotlin_onetoonecallsample.stringee.common.ConnectionState
import com.stringee.kotlin_onetoonecallsample.stringee.manager.StringeeCallManager.Companion.isSamePendingConnection
import com.stringee.kotlin_onetoonecallsample.stringee.manager.StringeeCallManager.Companion.stateAfterDisconnect
import org.junit.Assert
import org.junit.Test

class StringeeCallManagerLogicTest {
    @Test
    fun sameTokenIsIdempotentWhileConnectionIsPending() {
        Assert.assertTrue(
            isSamePendingConnection(
                "token", "token", ConnectionState.CONNECTING
            )
        )
        Assert.assertFalse(
            isSamePendingConnection(
                "new-token", "token", ConnectionState.CONNECTING
            )
        )
        Assert.assertFalse(
            isSamePendingConnection(
                "token", "token", ConnectionState.ERROR
            )
        )
    }

    @Test
    fun reconnectingDisconnectDoesNotLeaveUiInConnectedState() {
        Assert.assertTrue(
            stateAfterDisconnect(true)
                    == ConnectionState.CONNECTING
        )
        Assert.assertTrue(
            stateAfterDisconnect(false)
                    == ConnectionState.DISCONNECTED
        )
    }
}
