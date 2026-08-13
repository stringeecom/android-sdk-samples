package com.stringee.kotlin_onetoonecallsample.stringee.common

/** Stable call states exposed to the host UI. */
enum class CallStatus(value: String) {
    INCOMING("Incoming"),
    CALLING("Calling"),
    RINGING("Ringing"),
    STARTING("Starting"),
    STARTED("Started"),
    RECONNECTING("Reconnecting"),
    BUSY("Busy"),
    ENDED("Ended");

    val value: String

    init {
        this.value = value
    }
}
