package com.stringee.kotlin_onetoonecallsample.listener

import com.stringee.kotlin_onetoonecallsample.common.CallStatus
import com.stringee.video.StringeeVideoTrack


interface OnCallListener {
    fun onCallStatus(status: CallStatus)
    fun onError(message: String?)
    fun onReceiveLocalStream()
    fun onReceiveRemoteStream()
    fun onSpeakerChange(isOn: Boolean)
    fun onMicChange(isOn: Boolean)
    fun onVideoChange(isOn: Boolean)
    fun onSharing(isSharing: Boolean)
    fun onTimer(duration: String)
    fun onVideoTrackAdded(stringeeVideoTrack: StringeeVideoTrack)
    fun onVideoTrackRemoved(stringeeVideoTrack: StringeeVideoTrack)
}
