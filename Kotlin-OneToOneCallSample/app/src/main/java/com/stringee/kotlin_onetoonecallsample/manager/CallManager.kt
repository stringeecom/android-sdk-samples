package com.stringee.kotlin_onetoonecallsample.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
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
import com.stringee.messaging.listeners.CallbackListener
import com.stringee.video.StringeeScreenCapture
import com.stringee.video.StringeeVideoTrack
import org.json.JSONObject
import org.webrtc.RendererCommon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask

class CallManager(context: Context) {
    private val context: Context = context.applicationContext
    private var stringeeCall: StringeeCall? = null
    private var stringeeCall2: StringeeCall2? = null
    private var isStringeeCall = false
    private var isVideoCall = false
    private var isSpeakerOn = false
    private var isVideoEnable = false
    private var isMicOn = true
    var isSharing: Boolean = false
        private set
    private var isSwitching = false
    private val audioManagerUtils: AudioManagerUtils = AudioManagerUtils.getInstance(context)
    private var listener: OnCallListener? = null
    private val clientManager: ClientManager
    private var callSignalingState = StringeeCall.SignalingState.CALLING
    private var callMediaState: StringeeCall.MediaState? = StringeeCall.MediaState.DISCONNECTED
    private var call2SignalingState = StringeeCall2.SignalingState.CALLING
    private var call2MediaState: StringeeCall2.MediaState? = StringeeCall2.MediaState.DISCONNECTED
    private var callStatus: CallStatus = CallStatus.CALLING
    private var timer: Timer? = null
    private var screenCapture: StringeeScreenCapture? = null
    private var mediaProjectionService: MyMediaProjectionService? = null

    init {
        this.audioManagerUtils.setAudioEvents(object : AudioManagerUtils.OnAudioEvents {
            override fun onAudioEvents(selectedAudioDevice: StringeeAudioManager.AudioDevice) {
                Utils.runOnUiThread {
                    Log.d(
                        Constant.TAG,
                        "onAudioEvents: selectedAudioDevice - " + selectedAudioDevice.name
                    )
                }
            }
        })
        this.clientManager = ClientManager.getInstance(context)
    }

    fun getCallStatus(): CallStatus? {
        return callStatus
    }

    fun initializedOutgoingCall(to: String?, isVideoCall: Boolean, isStringeeCall: Boolean) {
        clientManager.isInCall = true
        if (isStringeeCall) {
            this.stringeeCall = StringeeCall(
                clientManager.stringeeClient, clientManager.stringeeClient?.userId, to
            )
            this.stringeeCall!!.isVideoCall = isVideoCall
        } else {
            this.stringeeCall2 = StringeeCall2(
                clientManager.stringeeClient, clientManager.stringeeClient?.userId, to
            )
            this.stringeeCall2!!.setVideoCall(isVideoCall)
        }
        this.isStringeeCall = isStringeeCall
        this.isVideoCall = isVideoCall
        this.isSpeakerOn = isVideoCall
        this.isVideoEnable = isVideoCall
        this.callStatus = CallStatus.CALLING
        registerCallEvent()
    }

    fun initializedIncomingCall(stringeeCall: StringeeCall) {
        clientManager.isInCall = true
        this.isStringeeCall = true
        this.stringeeCall = stringeeCall
        this.isVideoCall = stringeeCall.isVideoCall
        this.isSpeakerOn = stringeeCall.isVideoCall
        this.isVideoEnable = stringeeCall.isVideoCall
        this.callStatus = CallStatus.INCOMING
        registerCallEvent()
    }

    fun initializedIncomingCall(stringeeCall2: StringeeCall2) {
        clientManager.isInCall = true
        this.isStringeeCall = false
        this.stringeeCall2 = stringeeCall2
        this.isVideoCall = stringeeCall2.isVideoCall
        this.isSpeakerOn = stringeeCall2.isVideoCall
        this.isVideoEnable = stringeeCall2.isVideoCall
        this.callStatus = CallStatus.INCOMING
        registerCallEvent()
    }

    fun registerEvent(listener: OnCallListener?) {
        this.listener = listener
    }

    private fun registerCallEvent() {
        if (isStringeeCall) {
            stringeeCall!!.setCallListener(object : StringeeCall.StringeeCallListener {
                override fun onSignalingStateChange(
                    stringeeCall: StringeeCall?,
                    signalingState: StringeeCall.SignalingState,
                    reason: String?,
                    sipCode: Int,
                    sipReason: String?
                ) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onSignalingStateChange: $signalingState")
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

                override fun onError(stringeeCall: StringeeCall?, code: Int, desc: String?) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onError: $desc")
                        callStatus = CallStatus.ENDED
                        listener?.onError(desc)
                        listener?.onCallStatus(callStatus)
                    }
                }

                override fun onHandledOnAnotherDevice(
                    stringeeCall: StringeeCall?,
                    signalingState: StringeeCall.SignalingState?,
                    desc: String?
                ) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onHandledOnAnotherDevice: $signalingState")
                        if (signalingState != StringeeCall.SignalingState.RINGING) {
                            callStatus = CallStatus.ENDED
                            listener?.onCallStatus(callStatus)
                        }
                    }
                }

                override fun onMediaStateChange(
                    stringeeCall: StringeeCall?, mediaState: StringeeCall.MediaState?
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

                override fun onLocalStream(stringeeCall: StringeeCall?) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onLocalStream")
                        if (isVideoCall) {

                            listener?.onReceiveLocalStream()

                        }
                    }
                }

                override fun onRemoteStream(stringeeCall: StringeeCall?) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onRemoteStream")
                        if (isVideoCall) {
                            listener?.onReceiveRemoteStream()
                        }
                    }
                }

                override fun onCallInfo(stringeeCall: StringeeCall?, jsonObject: JSONObject) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG, "onCallInfo: $jsonObject"
                        )
                    }
                }
            })
        } else {
            stringeeCall2!!.setCallListener(object : StringeeCall2.StringeeCallListener {
                override fun onSignalingStateChange(
                    stringeeCall2: StringeeCall2?,
                    signalingState: StringeeCall2.SignalingState,
                    reason: String?,
                    sipCode: Int,
                    sipReason: String?
                ) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onSignalingStateChange: $signalingState")
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

                override fun onError(stringeeCall2: StringeeCall2?, code: Int, desc: String?) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onError: $desc")
                        callStatus = CallStatus.ENDED
                        listener?.onError(desc)
                        listener?.onCallStatus(callStatus)
                    }
                }

                override fun onHandledOnAnotherDevice(
                    stringeeCall2: StringeeCall2?,
                    signalingState: StringeeCall2.SignalingState?,
                    desc: String?
                ) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onHandledOnAnotherDevice: $signalingState")
                        if (signalingState != StringeeCall2.SignalingState.RINGING) {
                            callStatus = CallStatus.ENDED
                            listener?.onCallStatus(callStatus)
                        }
                    }
                }

                override fun onMediaStateChange(
                    stringeeCall2: StringeeCall2?, mediaState: StringeeCall2.MediaState?
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

                override fun onLocalStream(stringeeCall2: StringeeCall2?) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onLocalStream")
                        if (isVideoCall) {
                            listener?.onReceiveLocalStream()
                        }
                    }
                }

                override fun onRemoteStream(stringeeCall2: StringeeCall2?) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onRemoteStream")
                        if (isVideoCall) {
                            listener?.onReceiveRemoteStream()
                        }
                    }
                }

                override fun onVideoTrackAdded(stringeeVideoTrack: StringeeVideoTrack) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onVideoTrackAdded: " + stringeeVideoTrack.id)
                        if (stringeeVideoTrack.trackType == StringeeVideoTrack.TrackType.SCREEN) {
                            listener?.onVideoTrackAdded(stringeeVideoTrack)
                        }
                    }
                }

                override fun onVideoTrackRemoved(stringeeVideoTrack: StringeeVideoTrack) {
                    Utils.runOnUiThread {
                        Log.d(Constant.TAG, "onVideoTrackRemoved: " + stringeeVideoTrack.id)
                        if (stringeeVideoTrack.trackType == StringeeVideoTrack.TrackType.SCREEN) {
                            listener?.onVideoTrackRemoved(stringeeVideoTrack)
                        }
                    }
                }

                override fun onCallInfo(stringeeCall2: StringeeCall2?, jsonObject: JSONObject) {
                    Utils.runOnUiThread {
                        Log.d(
                            Constant.TAG, "onCallInfo: $jsonObject"
                        )
                    }
                }

                override fun onTrackMediaStateChange(
                    s: String?, mediaType: StringeeVideoTrack.MediaType?, b: Boolean
                ) {
                }

                override fun onLocalTrackAdded(
                    stringeeCall2: StringeeCall2?, stringeeVideoTrack: StringeeVideoTrack?
                ) {
                }

                override fun onRemoteTrackAdded(
                    stringeeCall2: StringeeCall2?, stringeeVideoTrack: StringeeVideoTrack?
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
        if (this.isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            stringeeCall!!.makeCall(object : StatusListener() {
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
            stringeeCall2!!.makeCall(object : StatusListener() {
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
        if (this.isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            stringeeCall!!.ringing(object : StatusListener() {
                override fun onSuccess() {
                    handleResponse("initAnswer", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    handleResponse("initAnswer", false, stringeeError.message)
                }
            })
        } else {
            stringeeCall2!!.ringing(object : StatusListener() {
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
        if (this.isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        NotificationUtils.getInstance(context).cancelNotification(Constant.INCOMING_CALL_ID)
        if (isStringeeCall) {
            stringeeCall!!.answer(object : StatusListener() {
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
            stringeeCall2!!.answer(object : StatusListener() {
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
        if (this.isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            if (isHangUp) {
                stringeeCall!!.hangup(object : StatusListener() {
                    override fun onSuccess() {
                        handleResponse("hangup", true, null)
                    }

                    override fun onError(stringeeError: StringeeError) {
                        super.onError(stringeeError)
                        handleResponse("hangup", false, stringeeError.message)
                    }
                })
            } else {
                stringeeCall!!.reject(object : StatusListener() {
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
                stringeeCall2!!.hangup(object : StatusListener() {
                    override fun onSuccess() {
                        handleResponse("hangup", true, null)
                    }

                    override fun onError(stringeeError: StringeeError) {
                        super.onError(stringeeError)
                        handleResponse("hangup", false, stringeeError.message)
                    }
                })
            } else {
                stringeeCall2!!.reject(object : StatusListener() {
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
        if (this.isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            stringeeCall!!.enableVideo(!isVideoEnable)
        } else {
            stringeeCall2!!.enableVideo(!isVideoEnable)
        }
        handleResponse("enableVideo", true, null)
        isVideoEnable = !isVideoEnable
        listener?.onVideoChange(isVideoEnable)
    }

    fun mute() {
        if (this.isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isStringeeCall) {
            stringeeCall!!.mute(isMicOn)
        } else {
            stringeeCall2!!.mute(isMicOn)
        }
        handleResponse("mute", true, null)
        isMicOn = !isMicOn
        listener?.onMicChange(isMicOn)
    }

    fun changeSpeaker() {
        if (this.isCallNotInitialized) {
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
        if (this.isCallNotInitialized) {
            listener?.onCallStatus(CallStatus.ENDED)
            release()
            return
        }
        if (isSwitching) {
            return
        }
        isSwitching = true
        if (isStringeeCall) {
            stringeeCall!!.switchCamera(object : StatusListener() {
                override fun onSuccess() {
                    isSwitching = false
                    handleResponse("switchCamera", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    isSwitching = false
                    handleResponse("switchCamera", false, stringeeError.message)
                }
            })
        } else {
            stringeeCall2!!.switchCamera(object : StatusListener() {
                override fun onSuccess() {
                    isSwitching = false
                    handleResponse("switchCamera", true, null)
                }

                override fun onError(stringeeError: StringeeError) {
                    super.onError(stringeeError)
                    isSwitching = false
                    handleResponse("switchCamera", false, stringeeError.message)
                }
            })
        }
    }

    private fun handleResponse(action: String?, isSuccess: Boolean, message: String?) {
        Log.d(Constant.TAG, action + ": " + (if (isSuccess) "success" else message))
        if (!isSuccess) {
            listener?.onError(message)
            release()
        }
    }

    private val isCallNotInitialized: Boolean
        get() {
            val isCallNotInitialized: Boolean = if (isStringeeCall) {
                stringeeCall == null
            } else {
                stringeeCall2 == null
            }
            if (isCallNotInitialized) {
                listener?.onError("call is not initialized")
            }
            return isCallNotInitialized
        }

    fun stopSharing() {
        if (!isSharing) {
            return
        }
        if (!(callStatus === CallStatus.STARTED && call2MediaState != null && call2MediaState == StringeeCall2.MediaState.CONNECTED)) {
            return
        }
        if (stringeeCall2 != null) {
            stringeeCall2!!.stopCaptureScreen(object : StatusListener() {
                override fun onSuccess() {
                }
            })
            mediaProjectionService?.stopService()
        }
        isSharing = false
        listener?.onSharing(false)
    }

    fun prepareShareScreen(
        activity: Activity,
        activityResultLauncher: ActivityResultLauncher<Intent?>,
        manager: MediaProjectionManager
    ) {
        if (isSharing) {
            return
        }
        if (stringeeCall2 != null) {
            if (!(callStatus === CallStatus.STARTED && call2MediaState != null && call2MediaState == StringeeCall2.MediaState.CONNECTED)) {
                return
            }
            screenCapture = StringeeScreenCapture(activity)
            activityResultLauncher.launch(manager.createScreenCaptureIntent())
        }
    }

    fun startCapture(
        mediaProjectionService: MyMediaProjectionService?,
        mediaProjectionPermissionResultData: Intent?
    ) {
        if (isSharing) {
            return
        }
        if (!(callStatus === CallStatus.STARTED && call2MediaState != null && call2MediaState == StringeeCall2.MediaState.CONNECTED)) {
            return
        }
        this.mediaProjectionService = mediaProjectionService
        if (stringeeCall2 != null) {
            screenCapture!!.createCapture(
                mediaProjectionPermissionResultData,
                object : CallbackListener<StringeeVideoTrack?>() {
                    override fun onSuccess(stringeeVideoTrack: StringeeVideoTrack?) {
                        stringeeCall2!!.startCaptureScreen(
                            screenCapture, object : StatusListener() {
                                override fun onSuccess() {
                                    isSharing = true
                                    stringeeCall2!!.enableVideo(false)
                                    isVideoEnable = false
                                    listener?.onVideoChange(false)
                                    listener?.onSharing(true)
                                }

                                override fun onError(stringeeError: StringeeError?) {
                                    super.onError(stringeeError)
                                    isSharing = false
                                    listener?.onSharing(false)
                                    mediaProjectionService?.stopService()
                                }
                            })
                    }

                    override fun onError(errorInfo: StringeeError?) {
                        super.onError(errorInfo)
                        isSharing = false
                        listener?.onSharing(false)
                        mediaProjectionService?.stopService()
                    }
                })
        }
    }

    fun release() {
        Log.d(Constant.TAG, "release callManager")
        if (isSharing && !isStringeeCall && isVideoCall) {
            if (stringeeCall2 != null) {
                stringeeCall2!!.stopCaptureScreen(object : StatusListener() {
                    override fun onSuccess() {
                    }
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
        NotificationUtils.getInstance(context).cancelNotification(Constant.INCOMING_CALL_ID)
        if (timer != null) {
            timer!!.cancel()
        }
        if (isStringeeCall) {
            stringeeCall = null
        } else {
            stringeeCall2 = null
        }
        instance = null
    }

    val from: String?
        get() {
            return if (isStringeeCall) {
                stringeeCall!!.from
            } else {
                stringeeCall2!!.from
            }
        }

    val localView: View?
        get() {
            return if (isStringeeCall) {
                stringeeCall!!.getLocalView2()
            } else {
                stringeeCall2!!.getLocalView2()
            }
        }

    val remoteView: View?
        get() {
            return if (isStringeeCall) {
                stringeeCall!!.getRemoteView2()
            } else {
                stringeeCall2!!.getRemoteView2()
            }
        }

    fun renderLocalView() {
        if (isStringeeCall) {
            stringeeCall!!.renderLocalView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
            stringeeCall!!.getLocalView2().setMirror(false)
        } else {
            stringeeCall2!!.renderLocalView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        }
    }

    fun renderRemoteView() {
        if (isStringeeCall) {
            stringeeCall!!.renderRemoteView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        } else {
            stringeeCall2!!.renderRemoteView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        }
    }

    private fun startTimer() {
        if (timer == null) {
            val startTime = System.currentTimeMillis()

            timer = Timer()
            val timerTask: TimerTask = object : TimerTask() {
                override fun run() {
                    Utils.runOnUiThread {
                        val time = System.currentTimeMillis() - startTime
                        val format = SimpleDateFormat("mm:ss", Locale.getDefault())
                        format.timeZone = TimeZone.getTimeZone("GMT")
                        listener?.onTimer(format.format(Date(time)))
                    }
                }
            }
            timer!!.schedule(timerTask, 0, 1000)
        }
    }

    companion object {
        @Volatile
        private var instance: CallManager? = null
        fun getInstance(context: Context): CallManager {
            return instance ?: synchronized(this) {
                instance ?: CallManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
