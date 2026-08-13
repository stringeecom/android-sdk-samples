package com.stringee.apptoappcallsample.stringee.common;

/** Stable call states exposed to the host UI. */
public enum CallStatus {
    INCOMING("Incoming"),
    CALLING("Calling"),
    RINGING("Ringing"),
    STARTING("Starting"),
    STARTED("Started"),
    RECONNECTING("Reconnecting"),
    BUSY("Busy"),
    ENDED("Ended");

    private final String value;

    CallStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
