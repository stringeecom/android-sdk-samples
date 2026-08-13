package com.stringee.kotlin_onetoonecallsample.stringee.listener

import com.stringee.common.StringeeAudioManager.AudioDevice
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallStatus
import com.stringee.kotlin_onetoonecallsample.stringee.manager.CallSession
import com.stringee.video.StringeeVideoTrack

/** Internal callbacks used to render an owned [CallSession] in the call activity. */
interface CallUiListener {
    fun onCallStatus(status: CallStatus)

    fun onError(message: String?)

    fun onLocalVideoAvailable()

    fun onRemoteVideoAvailable()

    fun onAudioDeviceChanged(selected: AudioDevice?, available: MutableSet<AudioDevice?>?)

    fun onMicChanged(enabled: Boolean)

    fun onVideoChanged(enabled: Boolean)

    fun onSharingChanged(sharing: Boolean)

    fun onTimer(duration: String?)

    fun onScreenTrackAdded(track: StringeeVideoTrack?)

    fun onScreenTrackRemoved(track: StringeeVideoTrack)
}
