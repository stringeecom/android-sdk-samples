package com.stringee.kotlin_onetoonecallsample.listener

import com.stringee.kotlin_onetoonecallsample.common.CallStatus


interface OnCallListener {
    fun onCallStatus(status: CallStatus?)
    fun onError(message: String?)
    fun onReceiveLocalStream()
    fun onReceiveRemoteStream()
    fun onSpeakerChange(isOn: Boolean)
    fun onMicChange(isOn: Boolean)
    fun onVideoChange(isOn: Boolean)
    fun onTimer(duration: String?)
}
