package com.stringee.kotlin_onetoonecallsample.common


enum class CallStatus(val value: String) {
    INCOMING("Incoming"),
    CALLING("Calling"),
    RINGING("Ringing"),
    STARTING("Starting"),
    STARTED("Started"),
    BUSY("Busy"),
    ENDED("Ended")

}