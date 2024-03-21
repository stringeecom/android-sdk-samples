package com.stringee.kotlin_onetoonecallsample.manager

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall2
import com.stringee.common.StringeeAudioManager
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.common.AudioManagerUtils
import com.stringee.kotlin_onetoonecallsample.common.CallStatus
import com.stringee.kotlin_onetoonecallsample.common.Constant
import com.stringee.kotlin_onetoonecallsample.common.NotificationUtils
import com.stringee.kotlin_onetoonecallsample.common.Utils
import com.stringee.kotlin_onetoonecallsample.listener.OnCallListener
import com.stringee.kotlin_onetoonecallsample.service.MyMediaProjectionService
import com.stringee.listener.StatusListener
import com.stringee.video.StringeeScreenCapture
import com.stringee.video.StringeeVideoTrack
import org.json.JSONObject
import org.webrtc.RendererCommon.ScalingType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask


class CallManager private constructor(private val applicationContext: Context) {
    private var stringeeCall: StringeeCall? = null
    private var stringeeCall2: StringeeCall2? = null
    private var isStringeeCall = false
    private var isVideoCall = false
    private var isSpeakerOn = false
    private var isVideoEnable = false
    private var isMicOn = true
    private var isSharing = false
    private val audioManagerUtils: AudioManagerUtils =
        AudioManagerUtils.getInstance(applicationContext)
    private var listener: OnCallListener? = null
    private val clientManager: ClientManager = ClientManager.getInstance(applicationContext)
    private var callSignalingState = StringeeCall.SignalingState.CALLING
    private var callMediaState = StringeeCall.MediaState.DISCONNECTED
    private var call2SignalingState = StringeeCall2.SignalingState.CALLING
    private var call2MediaState = StringeeCall2.MediaState.DISCONNECTED
    var callStatus = CallStatus.CALLING
        private set
    private var timer: Timer? = null
    private var screenCapture: StringeeScreenCapture? = null
    private var mediaProjectionService: MyMediaProjectionService? = null

    init {
        audioManagerUtils.setAudioEvents(object : AudioManagerUtils.OnAudioEvents {
            override fun onAudioEvents(selectedAudioDevice: StringeeAudioManager.AudioDevice) {
                Utils.runOnUiThread {
                    Log.d(
                        Constant.TAG,
                        "onAudioEvents: selectedAudioDevice - " + selectedAudioDevice.name
                    )
                }
            }
        })
    }

    fun initializedOutgoingCall(to: String?, isVideoCall: Boolean, isStringeeCall: Boolean) {
        clientManager.isInCall = true
        if (isStringeeCall) {
            stringeeCall = StringeeCall(
                clientManager.stringeeClient,
                clientManager.stringeeClient?.userId,
                to
            )
            stringeeCall?.isVideoCall = isVideoCall
        } else {
            stringeeCall2 = StringeeCall2(
                clientManager.stringeeClient,
                clientManager.stringeeClient?.userId,
                to
            )
            stringeeCall2?.isVideoCall = isVideoCall
        }
        this.isStringeeCall = isStringeeCall
        this.isVideoCall = isVideoCall
        isSpeakerOn = isVideoCall
        isVideoEnable = isVideoCall
        callStatus = CallStatus.CALLING
        registerCallEvent()
    }

    fun initializedIncomingCall(stringeeCall: StringeeCall) {
        clientManager.isInCall = true
        isStringeeCall = true
        this.stringeeCall = stringeeCall
        isVideoCall = stringeeCall.isVideoCall
        isSpeakerOn = stringeeCall.isVideoCall
        isVideoEnable = stringeeCall.isVideoCall
        callStatus = CallStatus.INCOMING
        registerCallEvent()
    }

    fun initializedIncomingCall(stringeeCall2: StringeeCall2) {
        clientManager.isInCall = true
        isStringeeCall = false
        this.stringeeCall2 = stringeeCall2
        isVideoCall = stringeeCall2.isVideoCall
        isSpeakerOn = stringeeCall2.isVideoCall
        isVideoEnable = stringeeCall2.isVideoCall
        callStatus = CallStatus.INCOMING
        registerCallEvent()
    }

    fun registerEvent(listener: OnCallListener?) {
        this.listener = listener
    }

    private fun registerCallEvent() {
        if (isStringeeCall) {
            stringeeCall?.setCallListener(object : StringeeCall.StringeeCallListener {
                override fun onSignalingStateChange(
                    stringeeCall: StringeeCall,
                    signalingState: StringeeCall.SignalingState,
                    reason: String,
                    sipCode: Int,
                    sipReason: String
                ) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onSignalingStateChange: $signalingState"
                        )
                        callSignalingState = signalingState
                        when (callSignalingState) {
                            StringeeCall.SignalingState.CALLING -> callStatus = CallStatus.CALLING
                            StringeeCall.SignalingState.RINGING -> callStatus = CallStatus.RINGING
                            StringeeCall.SignalingState.ANSWERED -> {
                                callStatus = CallStatus.STARTING
                                if (callMediaState == StringeeCall.MediaState.CONNECTED) {
                                    startTimer()
                                    callStatus = CallStatus.STARTED
                                }
                            }

                            StringeeCall.SignalingState.BUSY -> {
                                callStatus = CallStatus.BUSY
                                release()
                            }

                            StringeeCall.SignalingState.ENDED -> {
                                callStatus = CallStatus.ENDED
                                release()
                            }
                        }
                        listener?.onCallStatus(callStatus)
                    }
                }

                override fun onError(stringeeCall: StringeeCall, code: Int, desc: String) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onError: $desc")
                        callStatus = CallStatus.ENDED
                        listener?.onError(desc)
                        listener?.onCallStatus(callStatus)
                    }
                }

                override fun onHandledOnAnotherDevice(
                    stringeeCall: StringeeCall,
                    signalingState: StringeeCall.SignalingState,
                    desc: String
                ) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onHandledOnAnotherDevice: $signalingState"
                        )
                        if (signalingState != StringeeCall.SignalingState.RINGING) {
                            callStatus = CallStatus.ENDED
                            listener?.onCallStatus(callStatus)
                        }
                    }
                }

                override fun onMediaStateChange(
                    stringeeCall: StringeeCall,
                    mediaState: StringeeCall.MediaState
                ) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onMediaStateChange: $mediaState")
                        callMediaState = mediaState
                        if (callSignalingState == StringeeCall.SignalingState.ANSWERED) {
                            callStatus = CallStatus.STARTED
                            startTimer()
                            listener?.onCallStatus(callStatus)
                        }
                    }
                }

                override fun onLocalStream(stringeeCall: StringeeCall) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onLocalStream")
                        if (isVideoCall) {
                            listener?.onReceiveLocalStream()
                        }
                    }
                }

                override fun onRemoteStream(stringeeCall: StringeeCall) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onRemoteStream")
                        if (isVideoCall) {
                            listener?.onReceiveRemoteStream()
                        }
                    }
                }

                override fun onCallInfo(stringeeCall: StringeeCall, jsonObject: JSONObject) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onCallInfo: $jsonObject"
                        )
                    }
                }
            })
        } else {
            stringeeCall2?.setCallListener(object : StringeeCall2.StringeeCallListener {
                override fun onSignalingStateChange(
                    stringeeCall2: StringeeCall2,
                    signalingState: StringeeCall2.SignalingState,
                    reason: String,
                    sipCode: Int,
                    sipReason: String
                ) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onSignalingStateChange: $signalingState"
                        )
                        call2SignalingState = signalingState
                        when (call2SignalingState) {
                            StringeeCall2.SignalingState.CALLING -> callStatus = CallStatus.CALLING
                            StringeeCall2.SignalingState.RINGING -> callStatus = CallStatus.RINGING
                            StringeeCall2.SignalingState.ANSWERED -> {
                                callStatus = CallStatus.STARTING
                                if (call2MediaState == StringeeCall2.MediaState.CONNECTED) {
                                    startTimer()
                                    callStatus = CallStatus.STARTED
                                }
                            }

                            StringeeCall2.SignalingState.BUSY -> {
                                callStatus = CallStatus.BUSY
                                release()
                            }

                            StringeeCall2.SignalingState.ENDED -> {
                                callStatus = CallStatus.ENDED
                                release()
                            }
                        }
                        listener?.onCallStatus(callStatus)
                    }
                }

                override fun onError(stringeeCall2: StringeeCall2, code: Int, desc: String) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onError: $desc")
                        callStatus = CallStatus.ENDED
                        listener?.onError(desc)
                        listener?.onCallStatus(callStatus)
                    }
                }

                override fun onHandledOnAnotherDevice(
                    stringeeCall2: StringeeCall2,
                    signalingState: StringeeCall2.SignalingState,
                    desc: String
                ) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onHandledOnAnotherDevice: $signalingState"
                        )
                        if (signalingState != StringeeCall2.SignalingState.RINGING) {
                            callStatus = CallStatus.ENDED
                            listener?.onCallStatus(callStatus)
                        }
                    }
                }

                override fun onMediaStateChange(
                    stringeeCall2: StringeeCall2,
                    mediaState: StringeeCall2.MediaState
                ) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onMediaStateChange: $mediaState")
                        call2MediaState = mediaState
                        if (call2SignalingState == StringeeCall2.SignalingState.ANSWERED) {
                            callStatus = CallStatus.STARTED
                            startTimer()
                            listener?.onCallStatus(callStatus)
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onLocalStream(stringeeCall2: StringeeCall2) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onLocalStream")
                        if (isVideoCall) {
                            listener?.onReceiveLocalStream()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onRemoteStream(stringeeCall2: StringeeCall2) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onRemoteStream")
                        if (isVideoCall) {
                            listener?.onReceiveRemoteStream()
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onVideoTrackAdded(stringeeVideoTrack: StringeeVideoTrack) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onVideoTrackAdded: " + stringeeVideoTrack.id
                        )
                        if (stringeeVideoTrack.trackType == StringeeVideoTrack.TrackType.SCREEN) {
                            if (listener != null) {
                                listener!!.onVideoTrackAdded(stringeeVideoTrack)
                            }
                        }
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onVideoTrackRemoved(stringeeVideoTrack: StringeeVideoTrack) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onVideoTrackRemoved: " + stringeeVideoTrack.id
                        )
                        if (stringeeVideoTrack.trackType == StringeeVideoTrack.TrackType.SCREEN) {
                            if (listener != null) {
                                listener!!.onVideoTrackRemoved(stringeeVideoTrack)
                            }
                        }
                    }
                }

                override fun onCallInfo(stringeeCall2: StringeeCall2, jsonObject: JSONObject) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onCallInfo: $jsonObject"
                        )
                    }
                }

                override fun onTrackMediaStateChange(
                    s: String,
                    mediaType: StringeeVideoTrack.MediaType,
                    b: Boolean
                ) {
                }

                override fun onLocalTrackAdded(
                    stringeeCall2: StringeeCall2,
                    stringeeVideoTrack: StringeeVideoTrack
                ) {
                }

                override fun onRemoteTrackAdded(
                    stringeeCall2: StringeeCall2,
                    stringeeVideoTrack: StringeeVideoTrack
                ) {
                }
            })
        }
    }

    private fun startAudioManager() {
        audioManagerUtils.startAudioManager()
        audioManagerUtils.setSpeakerphoneOn(isSpeakerOn)
    }

    fun makeCall() {
        if (isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            stringeeCall?.makeCall(object : StatusListener() {
                override fun onSuccess() {
                    startAudioManager()
                    handleResponse("makeCall", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    handleResponse("makeCall", false, stringeeError.message)
                }
            })
        } else {
            stringeeCall2?.makeCall(object : StatusListener() {
                override fun onSuccess() {
                    startAudioManager()
                    handleResponse("makeCall", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    handleResponse("makeCall", false, stringeeError.message)
                }
            })
        }
    }

    fun initAnswer() {
        if (isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            stringeeCall?.ringing(object : StatusListener() {
                override fun onSuccess() {
                    handleResponse("initAnswer", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    handleResponse("initAnswer", false, stringeeError.message)
                }
            })
        } else {
            stringeeCall2?.ringing(object : StatusListener() {
                override fun onSuccess() {
                    handleResponse("initAnswer", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    handleResponse("initAnswer", false, stringeeError.message)
                }
            })
        }
    }

    fun answer() {
        if (isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        NotificationUtils.getInstance(this.applicationContext)
            .cancelNotification(Constant.INCOMING_CALL_ID)
        if (isStringeeCall) {
            stringeeCall?.answer(object : StatusListener() {
                override fun onSuccess() {
                    startAudioManager()
                    audioManagerUtils.stopRinging()
                    handleResponse("answer", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    handleResponse("answer", false, stringeeError.message)
                }
            })
        } else {
            stringeeCall2?.answer(object : StatusListener() {
                override fun onSuccess() {
                    startAudioManager()
                    audioManagerUtils.stopRinging()
                    handleResponse("answer", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    handleResponse("answer", false, stringeeError.message)
                }
            })
        }
    }

    fun endCall(isHangUp: Boolean) {
        if (isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            if (isHangUp) {
                stringeeCall?.hangup(object : StatusListener() {
                    override fun onSuccess() {
                        handleResponse("hangup", true, null)
                    }

                    override fun onError(stringeeError: StringeeError) {
                        super.onError(stringeeError)
                        handleResponse("hangup", false, stringeeError.message)
                    }
                })
            } else {
                stringeeCall?.reject(object : StatusListener() {
                    override fun onSuccess() {
                        handleResponse("reject", true, null)
                    }

                    override fun onError(stringeeError: StringeeError) {
                        super.onError(stringeeError)
                        handleResponse("reject", false, stringeeError.message)
                    }
                })
            }
        } else {
            if (isHangUp) {
                stringeeCall2?.hangup(object : StatusListener() {
                    override fun onSuccess() {
                        handleResponse("hangup", true, null)
                    }

                    override fun onError(stringeeError: StringeeError) {
                        super.onError(stringeeError)
                        handleResponse("hangup", false, stringeeError.message)
                    }
                })
            } else {
                stringeeCall2?.reject(object : StatusListener() {
                    override fun onSuccess() {
                        handleResponse("reject", true, null)
                    }

                    override fun onError(stringeeError: StringeeError) {
                        super.onError(stringeeError)
                        handleResponse("reject", false, stringeeError.message)
                    }
                })
            }
        }
        listener?.onCallStatus(CallStatus.ENDED)
        release()
    }

    fun enableVideo() {
        if (isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            stringeeCall?.enableVideo(!isVideoEnable)
        } else {
            stringeeCall2?.enableVideo(!isVideoEnable)
        }
        handleResponse("enableVideo", true, null)
        isVideoEnable = !isVideoEnable
        listener?.onVideoChange(isVideoEnable)
    }

    fun mute() {
        if (isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            stringeeCall?.mute(isMicOn)
        } else {
            stringeeCall2?.mute(isMicOn)
        }
        handleResponse("mute", true, null)
        isMicOn = !isMicOn
        listener?.onMicChange(isMicOn)
    }

    fun changeSpeaker() {
        if (isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            audioManagerUtils.setSpeakerphoneOn(!isSpeakerOn)
        } else {
            audioManagerUtils.setSpeakerphoneOn(!isSpeakerOn)
        }
        handleResponse("changeSpeaker", true, null)
        isSpeakerOn = !isSpeakerOn
        listener?.onSpeakerChange(isSpeakerOn)
    }

    fun switchCamera() {
        if (isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            stringeeCall?.switchCamera(object : StatusListener() {
                override fun onSuccess() {
                    handleResponse("switchCamera", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    handleResponse("switchCamera", false, stringeeError.message)
                }
            })
        } else {
            stringeeCall2?.switchCamera(object : StatusListener() {
                override fun onSuccess() {
                    handleResponse("switchCamera", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    handleResponse("switchCamera", false, stringeeError.message)
                }
            })
        }
    }

    private fun handleResponse(action: String, isSuccess: Boolean, message: String?) {
        Log.d(Constant.TAG, action + ": " + if (isSuccess) "success" else message)
        if (!isSuccess) {
            listener?.onError(message)
            release()
        }
    }

    private val isCallNotInitialized: Boolean
        get() {
            val isCallNotInitialized: Boolean = if (isStringeeCall) {
                Log.d(Constant.TAG, "isCallNotInitialized1: $stringeeCall")
                stringeeCall == null
            } else {
                Log.d(Constant.TAG, "isCallNotInitialized2: $stringeeCall2")
                stringeeCall2 == null
            }
            if (isCallNotInitialized) {
                listener?.onError("call is not initialized")
            }
            return isCallNotInitialized
        }

    fun initializeScreenCapture(activity: AppCompatActivity?) {
        if (screenCapture == null) {
            screenCapture = StringeeScreenCapture.Builder().buildWithAppCompatActivity(activity)
        }
    }

    fun shareScreen() {
        if (stringeeCall2 != null) {
            if (!(callStatus === CallStatus.STARTED && call2MediaState == StringeeCall2.MediaState.CONNECTED)) {
                return
            }
            if (isSharing) {
                stringeeCall2!!.stopCaptureScreen(object : StatusListener() {
                    override fun onSuccess() {}
                })
                mediaProjectionService?.stopService()
            } else {
                val intent = Intent(applicationContext, MyMediaProjectionService::class.java)
                intent.setAction(Constant.ACTION_START_FOREGROUND_SERVICE)
                applicationContext.startService(intent)
            }
            isSharing = !isSharing
        }
        if (listener != null) {
            listener!!.onSharing(isSharing)
        }
    }

    fun startCapture(mediaProjectionService: MyMediaProjectionService?) {
        this.mediaProjectionService = mediaProjectionService
        Utils.postDelay({
            if (stringeeCall2 != null) {
                stringeeCall2!!.startCaptureScreen(screenCapture, object : StatusListener() {
                    override fun onSuccess() {}
                    override fun onError(stringeeError: StringeeError) {
                        super.onError(stringeeError)
                        isSharing = false
                        if (listener != null) {
                            listener!!.onSharing(false)
                        }
                        mediaProjectionService?.stopService()
                    }
                })
            }
        }, 500)
    }

    fun release() {
        Log.d(Constant.TAG, "release callManager")
        if (isSharing && !isStringeeCall && isVideoCall) {
            if (stringeeCall2 != null) {
                stringeeCall2!!.stopCaptureScreen(object : StatusListener() {
                    override fun onSuccess() {}
                })
            }
            if (screenCapture != null) {
                screenCapture = null
            }
            mediaProjectionService?.stopService()
        }
        clientManager.isInCall = false
        audioManagerUtils.stopAudioManager()
        audioManagerUtils.stopRinging()
        NotificationUtils.getInstance(this.applicationContext)
            .cancelNotification(Constant.INCOMING_CALL_ID)
        if (timer != null) {
            timer?.cancel()
        }
        if (isStringeeCall) {
            stringeeCall = null
        } else {
            stringeeCall2 = null
        }
        instance = null
    }

    val from: String
        get() = if (isStringeeCall) {
            stringeeCall?.from!!
        } else {
            stringeeCall2?.from!!
        }
    val localView: View
        get() {
            return if (isStringeeCall) {
                stringeeCall?.localView2!!
            } else {
                stringeeCall2?.localView2!!
            }
        }
    val remoteView: View
        get() {
            return if (isStringeeCall) {
                stringeeCall?.remoteView2!!
            } else {
                stringeeCall2?.remoteView2!!
            }
        }

    fun renderLocalView() {
        if (isStringeeCall) {
            stringeeCall?.renderLocalView2(ScalingType.SCALE_ASPECT_FIT)
            stringeeCall?.localView2!!.setMirror(false)
        } else {
            stringeeCall2?.renderLocalView2(ScalingType.SCALE_ASPECT_FIT)
        }
    }

    fun renderRemoteView() {
        if (isStringeeCall) {
            stringeeCall?.renderRemoteView2(ScalingType.SCALE_ASPECT_FIT)
        } else {
            stringeeCall2?.renderRemoteView2(ScalingType.SCALE_ASPECT_FIT)
        }
    }

    private fun startTimer() {
        if (timer == null) {
            val startTime = System.currentTimeMillis()
            timer = Timer()
            val timerTask: TimerTask = object : TimerTask() {
                override fun run() {
                    Utils.runOnUiThread {
                        val time: Long = System.currentTimeMillis() - startTime
                        val format =
                            SimpleDateFormat("mm:ss", Locale.getDefault())
                        format.timeZone = TimeZone.getTimeZone("GMT")
                        listener?.onTimer(format.format(Date(time)))
                    }
                }
            }
            timer?.schedule(timerTask, 0, 1000)
        }
    }

    companion object {
        @Volatile
        private var instance: CallManager? = null
        fun getInstance(context: Context): CallManager {
            return instance ?: synchronized(this) {
                instance
                    ?: CallManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

