package com.stringee.apptoappcallsample.common;

public enum CallStatus {
    INCOMING("Incoming"),
    CALLING("Calling"),
    RINGING("Ringing"),
    STARTING("Starting"),
    STARTED("Started"),
    BUSY("Busy"),
    ENDED("Ended");

    private String value;

    CallStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}