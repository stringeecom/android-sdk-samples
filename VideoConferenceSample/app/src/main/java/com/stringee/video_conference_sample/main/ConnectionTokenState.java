package com.stringee.video_conference_sample.main;

/** Tracks pending, successful and one-shot auto-connect token state for the conference host. */
public final class ConnectionTokenState {
    private String savedToken;
    private String pendingToken = "";
    private boolean connecting;
    private boolean autoConnectAttempted;

    public ConnectionTokenState(String savedToken) {
        this.savedToken = ConferenceInputPolicy.normalizeToken(savedToken);
    }

    public String beginConnect(String token) {
        String normalized = ConferenceInputPolicy.normalizeToken(token);
        if (connecting || normalized.isEmpty()) {
            return "";
        }
        pendingToken = normalized;
        connecting = true;
        return normalized;
    }

    public String onConnected() {
        if (!pendingToken.isEmpty()) {
            savedToken = pendingToken;
        }
        pendingToken = "";
        connecting = false;
        return savedToken;
    }

    public void onConnectionError() {
        pendingToken = "";
        connecting = false;
    }

    public String consumeAutoConnectToken() {
        if (autoConnectAttempted) {
            return "";
        }
        autoConnectAttempted = true;
        return savedToken;
    }

    public String getSavedToken() {
        return savedToken;
    }

    public boolean isConnecting() {
        return connecting;
    }
}
