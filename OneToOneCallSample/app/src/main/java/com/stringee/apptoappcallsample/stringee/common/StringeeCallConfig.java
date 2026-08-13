package com.stringee.apptoappcallsample.stringee.common;

/** Immutable outgoing-call options consumed by {@code StringeeCallManager}. */
public final class StringeeCallConfig {
    private final String to;
    private final CallEngine callEngine;
    private final boolean videoCall;
    private String customData;

    /** Creates a configuration for a recipient, call engine, and voice/video mode. */
    public StringeeCallConfig(String to, CallEngine callEngine, boolean videoCall) {
        this.to = to == null ? "" : to.trim();
        this.callEngine = callEngine == null ? CallEngine.STRINGEE_CALL : callEngine;
        this.videoCall = videoCall;
    }

    public String getTo() {
        return to;
    }

    public CallEngine getCallEngine() {
        return callEngine;
    }

    public boolean isVideoCall() {
        return videoCall;
    }

    public String getCustomData() {
        return customData;
    }

    /** Sets optional custom data forwarded with the call and returns this configuration. */
    public StringeeCallConfig setCustomData(String customData) {
        this.customData = customData;
        return this;
    }
}
