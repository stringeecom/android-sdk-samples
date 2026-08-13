package com.stringee.video_conference_sample.main;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectionTokenStateTest {

    @Test
    public void pendingTokenIsCommittedOnlyOnConnectedCallback() {
        ConnectionTokenState state = new ConnectionTokenState("old-token");

        assertEquals("new-token", state.beginConnect(" new-token "));
        assertEquals("old-token", state.getSavedToken());
        assertEquals("new-token", state.onConnected());
        assertEquals("new-token", state.getSavedToken());
    }

    @Test
    public void connectionErrorPreservesLastSuccessfulToken() {
        ConnectionTokenState state = new ConnectionTokenState("last-good-token");
        state.beginConnect("bad-token");

        state.onConnectionError();

        assertEquals("last-good-token", state.getSavedToken());
        assertFalse(state.isConnecting());
    }

    @Test
    public void connectionCannotBeginTwiceOrWithBlankToken() {
        ConnectionTokenState state = new ConnectionTokenState("");

        assertEquals("", state.beginConnect("  "));
        assertEquals("first-token", state.beginConnect("first-token"));
        assertEquals("", state.beginConnect("second-token"));
        assertTrue(state.isConnecting());
    }

    @Test
    public void savedTokenIsAutoConnectedOnlyOnce() {
        ConnectionTokenState state = new ConnectionTokenState("saved-token");

        assertEquals("saved-token", state.consumeAutoConnectToken());
        assertEquals("", state.consumeAutoConnectToken());
    }
}
