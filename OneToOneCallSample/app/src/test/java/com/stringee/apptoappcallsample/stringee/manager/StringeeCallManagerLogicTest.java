package com.stringee.apptoappcallsample.stringee.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.stringee.apptoappcallsample.stringee.common.ConnectionState;

import org.junit.Test;

public class StringeeCallManagerLogicTest {
    @Test
    public void sameTokenIsIdempotentWhileConnectionIsPending() {
        assertTrue(StringeeCallManager.isSamePendingConnection(
                "token", "token", ConnectionState.CONNECTING));
        assertFalse(StringeeCallManager.isSamePendingConnection(
                "new-token", "token", ConnectionState.CONNECTING));
        assertFalse(StringeeCallManager.isSamePendingConnection(
                "token", "token", ConnectionState.ERROR));
    }

    @Test
    public void reconnectingDisconnectDoesNotLeaveUiInConnectedState() {
        assertTrue(StringeeCallManager.stateAfterDisconnect(true)
                == ConnectionState.CONNECTING);
        assertTrue(StringeeCallManager.stateAfterDisconnect(false)
                == ConnectionState.DISCONNECTED);
    }
}
