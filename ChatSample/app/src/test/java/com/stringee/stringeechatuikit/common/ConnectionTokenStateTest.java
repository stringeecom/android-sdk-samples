package com.stringee.stringeechatuikit.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionTokenStateTest {

    @Test
    public void successfulConnectionCommitsTrimmedPendingToken() {
        ConnectionTokenState state = new ConnectionTokenState("old-token");

        assertEquals("new-token", state.beginConnect("  new-token  "));
        assertEquals("old-token", state.getSavedToken());

        assertEquals("new-token", state.onConnected());
        assertEquals("new-token", state.getSavedToken());
        assertFalse(state.isConnecting());
    }

    @Test
    public void connectionErrorKeepsLastSuccessfulToken() {
        ConnectionTokenState state = new ConnectionTokenState("last-good-token");
        state.beginConnect("bad-token");

        state.onConnectionError();

        assertEquals("last-good-token", state.getSavedToken());
        assertFalse(state.isConnecting());
    }

    @Test
    public void blankTokenDoesNotStartConnection() {
        ConnectionTokenState state = new ConnectionTokenState("saved-token");

        assertEquals("", state.beginConnect("   "));
        assertFalse(state.isConnecting());
    }

    @Test
    public void savedTokenIsConsumedForAutoConnectOnlyOnce() {
        ConnectionTokenState state = new ConnectionTokenState("saved-token");

        assertEquals("saved-token", state.consumeAutoConnectToken());
        assertEquals("", state.consumeAutoConnectToken());
        assertTrue(state.hasAutoConnectBeenAttempted());
    }
}
