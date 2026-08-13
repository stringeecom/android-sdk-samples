package com.stringee.videocallsample;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ConnectionTokenStateTest {

    @Test
    public void tokenIsSavedOnlyAfterSuccessfulConnection() {
        ConnectionTokenState state = new ConnectionTokenState("old-token");

        assertEquals("new-token", state.beginConnect(" new-token "));
        assertEquals("old-token", state.getSavedToken());
        assertEquals("new-token", state.onConnected());
        assertEquals("new-token", state.getSavedToken());
    }

    @Test
    public void failedConnectionDoesNotOverwriteSavedToken() {
        ConnectionTokenState state = new ConnectionTokenState("last-good-token");
        state.beginConnect("invalid-token");

        state.onConnectionError();

        assertEquals("last-good-token", state.getSavedToken());
        assertFalse(state.isConnecting());
    }

    @Test
    public void autoConnectTokenCanOnlyBeConsumedOnce() {
        ConnectionTokenState state = new ConnectionTokenState("saved-token");

        assertEquals("saved-token", state.consumeAutoConnectToken());
        assertEquals("", state.consumeAutoConnectToken());
    }
}
