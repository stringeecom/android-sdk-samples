package com.stringee.kotlin_onetoonecallsample.stringee.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import com.stringee.StringeeClient
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall2
import com.stringee.common.StringeeAudioManager.AudioDevice
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallAudioManager
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallConstants
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallEngine
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallStatus
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallUtils
import com.stringee.kotlin_onetoonecallsample.stringee.common.StringeeCallConfig
import com.stringee.kotlin_onetoonecallsample.stringee.listener.CallUiListener
import com.stringee.kotlin_onetoonecallsample.stringee.manager.CallSetupStateMachine.SuccessResult
import com.stringee.kotlin_onetoonecallsample.stringee.service.InCallService
import com.stringee.kotlin_onetoonecallsample.stringee.service.IncomingCallService
import com.stringee.kotlin_onetoonecallsample.stringee.service.MediaProjectionCallService
import com.stringee.listener.StatusListener
import com.stringee.messaging.listeners.CallbackListener
import com.stringee.video.StringeeScreenCapture
import com.stringee.video.StringeeVideoTrack
import com.stringee.video.StringeeVideoTrack.TrackType
import org.json.JSONObject
import org.webrtc.RendererCommon
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask

/** Owns one SDK call, its state machine, media, timer, audio, and idempotent cleanup. */
class CallSession(
    context: Context,
    private val owner: Owner,
    val generation: Long,
    initialStatus: CallStatus
) {
    interface Owner {
        fun onSessionStateChanged(session: CallSession?, status: CallStatus)

        fun onSessionError(session: CallSession?, action: String?, error: StringeeError?)

        fun onSessionReleased(session: CallSession?)
    }

    private val context: Context
    private val audioManager: CallAudioManager
    private val stateMachine: CallStateMachine
    private val setupStateMachine = CallSetupStateMachine()

    private var stringeeCall: StringeeCall? = null
    private var stringeeCall2: StringeeCall2? = null
    var from: String = ""
        private set
    private var callId: String? = ""
    var engine: CallEngine? = null
        private set
    var isVideoCall: Boolean = false
        private set
    var isIncoming: Boolean = false
        private set
    private var micEnabled = true
    private var videoEnabled = false
    var isSharing: Boolean = false
        private set
    private var switchingCamera = false
    private var released = false
    private var outgoingStarted = false
    var startedAt: Long = 0
        private set
    private var timer: Timer? = null
    private var setupTimeoutTimer: Timer? = null
    private var outgoingCallback: StatusListener? = null
    private var uiListener: CallUiListener? = null
    private var screenCapture: StringeeScreenCapture? = null
    private var mediaProjectionService: MediaProjectionCallService? = null
    private val remoteScreenTracks: MutableList<StringeeVideoTrack?> =
        ArrayList<StringeeVideoTrack?>()

    init {
        this.context = context.getApplicationContext()
        stateMachine = CallStateMachine(initialStatus)
        audioManager = CallAudioManager.Companion.getInstance(context)
        audioManager.setListener(CallAudioManager.Listener { selected: AudioDevice?, available: MutableSet<AudioDevice?>? ->
            if (isCurrent && uiListener != null) {
                uiListener!!.onAudioDeviceChanged(selected, available)
            }
        })
    }

    fun prepareOutgoing(client: StringeeClient, config: StringeeCallConfig) {
        isIncoming = false
        this.from = config.to
        engine = config.callEngine
        isVideoCall = config.isVideoCall
        videoEnabled = isVideoCall
        if (engine == CallEngine.STRINGEE_CALL) {
            stringeeCall = StringeeCall(client, client.getUserId(), config.to)
            stringeeCall!!.setVideoCall(isVideoCall)
            if (!CallUtils.isEmpty(config.customData)) {
                stringeeCall!!.setCustom(config.customData)
            }
        } else {
            stringeeCall2 = StringeeCall2(client, client.getUserId(), config.to)
            stringeeCall2!!.setVideoCall(isVideoCall)
            if (!CallUtils.isEmpty(config.customData)) {
                stringeeCall2!!.setCustom(config.customData)
            }
        }
        registerSdkListeners()
    }

    fun prepareIncoming(call: StringeeCall) {
        isIncoming = true
        this.from = call.getFrom().orEmpty()
        callId = call.getCallId()
        setupStateMachine.establishIncoming(callId.orEmpty())
        engine = CallEngine.STRINGEE_CALL
        stringeeCall = call
        isVideoCall = call.isVideoCall
        videoEnabled = isVideoCall
        stateMachine.setStatus(CallStatus.INCOMING)
        registerSdkListeners()
    }

    fun prepareIncoming(call: StringeeCall2) {
        isIncoming = true
        this.from = call.getFrom().orEmpty()
        callId = call.getCallId()
        setupStateMachine.establishIncoming(callId.orEmpty())
        engine = CallEngine.STRINGEE_CALL2
        stringeeCall2 = call
        isVideoCall = call.isVideoCall
        videoEnabled = isVideoCall
        stateMachine.setStatus(CallStatus.INCOMING)
        registerSdkListeners()
    }

    private fun registerSdkListeners() {
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall!!.setCallListener(object : StringeeCall.StringeeCallListener {
                override fun onSignalingStateChange(
                    call: StringeeCall?,
                    state: StringeeCall.SignalingState,
                    reason: String?, sipCode: Int,
                    sipReason: String?
                ) {
                    CallUtils.runOnUiThread(Runnable { handleSignaling(call, state) })
                }

                override fun onError(call: StringeeCall?, code: Int, description: String?) {
                    CallUtils.runOnUiThread(Runnable { handleSdkError(call, code, description) })
                }

                override fun onHandledOnAnotherDevice(
                    call: StringeeCall?,
                    state: StringeeCall.SignalingState?,
                    description: String?
                ) {
                    CallUtils.runOnUiThread(Runnable {
                        if (call === stringeeCall && state != StringeeCall.SignalingState.RINGING) {
                            terminal(CallStatus.ENDED)
                        }
                    })
                }

                override fun onMediaStateChange(
                    call: StringeeCall?,
                    state: StringeeCall.MediaState?
                ) {
                    CallUtils.runOnUiThread(Runnable {
                        if (call !== stringeeCall || released) {
                            return@Runnable
                        }
                        handleMediaConnected(state == StringeeCall.MediaState.CONNECTED)
                    })
                }

                override fun onLocalStream(call: StringeeCall?) {
                    CallUtils.runOnUiThread(Runnable {
                        if (call === stringeeCall && isVideoCall && uiListener != null) {
                            uiListener!!.onLocalVideoAvailable()
                        }
                    })
                }

                override fun onRemoteStream(call: StringeeCall?) {
                    CallUtils.runOnUiThread(Runnable {
                        if (call === stringeeCall && isVideoCall && uiListener != null) {
                            uiListener!!.onRemoteVideoAvailable()
                        }
                    })
                }

                override fun onCallInfo(call: StringeeCall?, info: JSONObject?) {
                    Log.d(CallConstants.TAG, "onCallInfo: " + info)
                }
            })
            return
        }
        if (stringeeCall2 == null) {
            return
        }
        stringeeCall2!!.setCaptureSessionListener(object :
            StringeeVideoTrack.CaptureSessionListener {
            override fun onCapturerStarted(trackType: TrackType?) {
                Log.d(CallConstants.TAG, "onCapturerStarted: " + trackType)
            }

            override fun onCapturerStopped(trackType: TrackType?) {
                CallUtils.runOnUiThread(Runnable {
                    if (trackType == TrackType.SCREEN && isSharing) {
                        finishSharing()
                    }
                })
            }
        })
        stringeeCall2!!.setCallListener(object : StringeeCall2.StringeeCallListener {
            override fun onSignalingStateChange(
                call: StringeeCall2?,
                state: StringeeCall2.SignalingState,
                reason: String?, sipCode: Int, sipReason: String?
            ) {
                CallUtils.runOnUiThread(Runnable { handleSignaling(call, state) })
            }

            override fun onError(call: StringeeCall2?, code: Int, description: String?) {
                CallUtils.runOnUiThread(Runnable { handleSdkError(call, code, description) })
            }

            override fun onHandledOnAnotherDevice(
                call: StringeeCall2?,
                state: StringeeCall2.SignalingState?,
                description: String?
            ) {
                CallUtils.runOnUiThread(Runnable {
                    if (call === stringeeCall2 && state != StringeeCall2.SignalingState.RINGING) {
                        terminal(CallStatus.ENDED)
                    }
                })
            }

            override fun onMediaStateChange(
                call: StringeeCall2?,
                state: StringeeCall2.MediaState?
            ) {
                CallUtils.runOnUiThread(Runnable {
                    if (call !== stringeeCall2 || released) {
                        return@Runnable
                    }
                    handleMediaConnected(state == StringeeCall2.MediaState.CONNECTED)
                })
            }

            override fun onLocalTrackAdded(call: StringeeCall2?, track: StringeeVideoTrack) {
                CallUtils.runOnUiThread(Runnable {
                    if (call !== stringeeCall2 || released
                        || track.getTrackType() == TrackType.SCREEN
                    ) {
                        return@Runnable
                    }
                    if (isVideoCall && uiListener != null) {
                        uiListener!!.onLocalVideoAvailable()
                    }
                })
            }

            override fun onRemoteTrackAdded(call: StringeeCall2?, track: StringeeVideoTrack) {
                CallUtils.runOnUiThread(Runnable {
                    if (call !== stringeeCall2 || released) {
                        return@Runnable
                    }
                    if (track.getTrackType() == TrackType.SCREEN) {
                        if (!remoteScreenTracks.contains(track)) {
                            remoteScreenTracks.add(track)
                        }
                        if (uiListener != null) {
                            uiListener!!.onScreenTrackAdded(track)
                        }
                    } else if (isVideoCall && uiListener != null) {
                        uiListener!!.onRemoteVideoAvailable()
                    }
                })
            }

            override fun onRemoteTrackRemoved(call: StringeeCall2?, track: StringeeVideoTrack) {
                CallUtils.runOnUiThread(Runnable {
                    if (call === stringeeCall2 && !released && track.getTrackType() == TrackType.SCREEN) {
                        remoteScreenTracks.remove(track)
                        if (uiListener != null) {
                            uiListener!!.onScreenTrackRemoved(track)
                        }
                    }
                })
            }

            override fun onCallInfo(call: StringeeCall2?, info: JSONObject?) {
                Log.d(CallConstants.TAG, "onCallInfo2: " + info)
            }

            override fun onTrackMediaStateChange(
                trackId: String?,
                mediaType: StringeeVideoTrack.MediaType?,
                enabled: Boolean
            ) {
            }
        })
    }

    private fun handleSignaling(call: StringeeCall?, state: StringeeCall.SignalingState) {
        if (call !== stringeeCall || released) {
            return
        }
        when (state) {
            StringeeCall.SignalingState.CALLING -> updateStatus(CallStatus.CALLING)
            StringeeCall.SignalingState.RINGING -> updateStatus(CallStatus.RINGING)
            StringeeCall.SignalingState.ANSWERED -> updateStatus(stateMachine.onSignalingAnswered())
            StringeeCall.SignalingState.BUSY -> terminal(CallStatus.BUSY)
            StringeeCall.SignalingState.ENDED -> terminal(CallStatus.ENDED)
        }
    }

    private fun handleSignaling(call: StringeeCall2?, state: StringeeCall2.SignalingState) {
        if (call !== stringeeCall2 || released) {
            return
        }
        when (state) {
            StringeeCall2.SignalingState.CALLING -> updateStatus(CallStatus.CALLING)
            StringeeCall2.SignalingState.RINGING -> updateStatus(CallStatus.RINGING)
            StringeeCall2.SignalingState.ANSWERED -> updateStatus(stateMachine.onSignalingAnswered())
            StringeeCall2.SignalingState.BUSY -> terminal(CallStatus.BUSY)
            StringeeCall2.SignalingState.ENDED -> terminal(CallStatus.ENDED)
        }
    }

    private fun handleMediaConnected(connected: Boolean) {
        val status = if (connected)
            stateMachine.onMediaConnected()
        else
            stateMachine.onMediaDisconnected()
        updateStatus(status)
    }

    private fun updateStatus(status: CallStatus) {
        if (released) {
            return
        }
        stateMachine.setStatus(status)
        if (status == CallStatus.STARTED) {
            startTimerIfNeeded()
            InCallService.Companion.startOrUpdate(context, generation)
        }
        if (uiListener != null) {
            uiListener!!.onCallStatus(status)
        }
        owner.onSessionStateChanged(this, status)
    }

    private fun handleSdkError(call: Any?, code: Int, description: String?) {
        val current = call === stringeeCall || call === stringeeCall2
        if (!current || released) {
            return
        }
        val error = StringeeError(code, description)
        if (!isIncoming && setupStateMachine.isPending) {
            handleMakeCallFailure(error)
            return
        }
        if (uiListener != null) {
            uiListener!!.onError(description)
        }
        owner.onSessionError(this, "call", error)
        terminal(CallStatus.ENDED)
    }

    fun registerUiListener(listener: CallUiListener?) {
        uiListener = listener
        if (listener != null) {
            listener.onCallStatus(stateMachine.status)
            listener.onMicChanged(micEnabled)
            listener.onVideoChanged(videoEnabled)
            listener.onAudioDeviceChanged(
                audioManager.getSelectedDevice(),
                mutableSetOf<AudioDevice?>()
            )
            if (isVideoCall && this.localView != null) {
                listener.onLocalVideoAvailable()
            }
            if (isVideoCall && this.remoteView != null) {
                listener.onRemoteVideoAvailable()
            }
            for (track in ArrayList<StringeeVideoTrack?>(remoteScreenTracks)) {
                listener.onScreenTrackAdded(track)
            }
        }
    }

    fun unregisterUiListener(listener: CallUiListener?) {
        if (uiListener === listener) {
            uiListener = null
        }
    }

    fun setOutgoingCallback(callback: StatusListener?) {
        outgoingCallback = callback
    }

    fun startOutgoing() {
        if (outgoingStarted || isIncoming || released) {
            return
        }
        outgoingStarted = true
        InCallService.Companion.startOrUpdate(context, generation)
        audioManager.start(isVideoCall)
        scheduleSetupTimeout()
        val outgoingCall = stringeeCall
        val outgoingCall2 = stringeeCall2
        val result: StatusListener = object : StatusListener() {
            override fun onSuccess() {
                CallUtils.runOnUiThread(Runnable {
                    handleMakeCallSuccess(
                        outgoingCall, outgoingCall2
                    )
                })
            }

            override fun onError(error: StringeeError?) {
                CallUtils.runOnUiThread(Runnable { handleMakeCallFailure(error) })
            }
        }
        try {
            if (engine == CallEngine.STRINGEE_CALL && outgoingCall != null) {
                outgoingCall.makeCall(result)
            } else if (outgoingCall2 != null) {
                outgoingCall2.makeCall(result)
            } else {
                handleMakeCallFailure(StringeeError(-1, "Call is not prepared"))
            }
        } catch (exception: RuntimeException) {
            CallUtils.reportException(CallSession::class.java, exception)
            handleMakeCallFailure(
                StringeeError(
                    -1,
                    if (exception.message == null)
                        "Unable to start call"
                    else
                        exception.message
                )
            )
        }
    }

    private fun handleMakeCallSuccess(
        outgoingCall: StringeeCall?,
        outgoingCall2: StringeeCall2?
    ) {
        val serverCallId = if (outgoingCall != null)
            outgoingCall.getCallId()
        else
            if (outgoingCall2 != null) outgoingCall2.getCallId() else ""
        val result = setupStateMachine.onSuccess(serverCallId)
        when (result) {
            SuccessResult.ESTABLISHED -> {
                cancelSetupTimeout()
                callId = serverCallId
                val callback = takeOutgoingCallback()
                if (callback != null) {
                    callback.onSuccess()
                }
            }

            SuccessResult.INVALID_SERVER_ID -> finishOutgoingSetupFailure(
                StringeeError(
                    -1,
                    "Stringee returned an empty call ID"
                )
            )

            SuccessResult.LATE_SUCCESS -> hangUpSdkCall(
                outgoingCall, outgoingCall2,
                "Hang up after late makeCall success"
            )

            SuccessResult.IGNORED -> {}
            else -> {}
        }
    }

    private fun handleMakeCallFailure(error: StringeeError?) {
        if (!setupStateMachine.fail()) {
            return
        }
        finishOutgoingSetupFailure(error)
    }

    private fun finishOutgoingSetupFailure(error: StringeeError?) {
        cancelSetupTimeout()
        val callback = takeOutgoingCallback()
        if (callback != null) {
            callback.onError(error)
        }
        if (released) {
            return
        }
        owner.onSessionError(this, "makeCall", error)
        terminal(CallStatus.ENDED)
    }

    private fun scheduleSetupTimeout() {
        cancelSetupTimeout()
        setupTimeoutTimer = Timer("StringeeCallSetup", true)
        setupTimeoutTimer!!.schedule(object : TimerTask() {
            override fun run() {
                CallUtils.runOnUiThread(Runnable {
                    handleMakeCallFailure(
                        StringeeError(-1, "Call setup timed out")
                    )
                })
            }
        }, OUTGOING_SETUP_TIMEOUT_MS)
    }

    private fun cancelSetupTimeout() {
        if (setupTimeoutTimer != null) {
            setupTimeoutTimer!!.cancel()
            setupTimeoutTimer = null
        }
    }

    private fun takeOutgoingCallback(): StatusListener? {
        val callback = outgoingCallback
        outgoingCallback = null
        return callback
    }

    private fun hangUpSdkCall(
        outgoingCall: StringeeCall?, outgoingCall2: StringeeCall2?,
        reason: String?
    ) {
        try {
            if (outgoingCall != null) {
                outgoingCall.hangup(reason, object : StatusListener() {
                    override fun onSuccess() {
                    }
                })
            } else if (outgoingCall2 != null) {
                outgoingCall2.hangup(reason, object : StatusListener() {
                    override fun onSuccess() {
                    }
                })
            }
        } catch (exception: RuntimeException) {
            CallUtils.reportException(CallSession::class.java, exception)
        }
    }

    fun ringing() {
        val listener = terminalOnError("ringing")
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall!!.ringing(listener)
        } else if (stringeeCall2 != null) {
            stringeeCall2!!.ringing(listener)
        }
    }

    fun answer() {
        if (!isIncoming || stateMachine.status != CallStatus.INCOMING || released) {
            return
        }
        updateStatus(CallStatus.STARTING)
        audioManager.stopRinging()
        audioManager.start(isVideoCall)
        IncomingCallService.Companion.clear(context, getCallId())
        InCallService.Companion.startOrUpdate(context, generation)
        val listener = terminalOnError("answer")
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall!!.answer(listener)
        } else if (stringeeCall2 != null) {
            stringeeCall2!!.answer(listener)
        }
    }

    fun reject() {
        if (released) {
            return
        }
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall!!.reject(terminalOnError("reject"))
        } else if (stringeeCall2 != null) {
            stringeeCall2!!.reject(terminalOnError("reject"))
        }
        terminal(CallStatus.ENDED)
    }

    fun hangUp() {
        if (released) {
            return
        }
        val cancelledPendingSetup = !isIncoming && outgoingStarted
                && setupStateMachine.cancel()
        if (!cancelledPendingSetup) {
            if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
                stringeeCall!!.hangup(terminalOnError("hangup"))
            } else if (stringeeCall2 != null) {
                stringeeCall2!!.hangup(terminalOnError("hangup"))
            }
        }
        terminal(CallStatus.ENDED)
    }

    fun endForDisconnect() {
        if (isIncoming && stateMachine.status == CallStatus.INCOMING) {
            reject()
        } else {
            hangUp()
        }
    }

    private fun terminalOnError(action: String?): StatusListener {
        return object : StatusListener() {
            override fun onSuccess() {
            }

            override fun onError(error: StringeeError?) {
                owner.onSessionError(this@CallSession, action, error)
                terminal(CallStatus.ENDED)
            }
        }
    }

    fun toggleMute() {
        if (released) {
            return
        }
        val mute = micEnabled
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall!!.mute(mute)
        } else if (stringeeCall2 != null) {
            stringeeCall2!!.mute(mute)
        }
        micEnabled = !micEnabled
        if (uiListener != null) {
            uiListener!!.onMicChanged(micEnabled)
        }
    }

    fun toggleVideo() {
        if (!isVideoCall || released) {
            return
        }
        videoEnabled = !videoEnabled
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall!!.enableVideo(videoEnabled)
        } else if (stringeeCall2 != null) {
            stringeeCall2!!.enableVideo(videoEnabled)
        }
        if (uiListener != null) {
            uiListener!!.onVideoChanged(videoEnabled)
        }
    }

    fun toggleSpeaker() {
        audioManager.toggleBuiltInDevice()
    }

    fun switchCamera() {
        if (!isVideoCall || switchingCamera || released) {
            return
        }
        switchingCamera = true
        val listener: StatusListener = object : StatusListener() {
            override fun onSuccess() {
                switchingCamera = false
            }

            override fun onError(error: StringeeError?) {
                switchingCamera = false
                owner.onSessionError(this@CallSession, "switchCamera", error)
            }
        }
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall!!.switchCamera(listener)
        } else if (stringeeCall2 != null) {
            stringeeCall2!!.switchCamera(listener)
        }
    }

    val localView: View?
        get() {
            if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
                return stringeeCall!!.getLocalView2()
            }
            return if (stringeeCall2 == null) null else stringeeCall2!!.getLocalView2()
        }

    val remoteView: View?
        get() {
            if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
                return stringeeCall!!.getRemoteView2()
            }
            return if (stringeeCall2 == null) null else stringeeCall2!!.getRemoteView2()
        }

    fun renderLocalView() {
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall!!.renderLocalView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        } else if (stringeeCall2 != null) {
            stringeeCall2!!.renderLocalView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        }
    }

    fun renderRemoteView() {
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            stringeeCall!!.renderRemoteView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        } else if (stringeeCall2 != null) {
            stringeeCall2!!.renderRemoteView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
        }
    }

    fun prepareScreenShare(
        activity: Activity, launcher: ActivityResultLauncher<Intent>,
        projectionManager: MediaProjectionManager
    ) {
        if (engine != CallEngine.STRINGEE_CALL2 || !isVideoCall || isSharing
            || stateMachine.status != CallStatus.STARTED || stringeeCall2 == null
        ) {
            return
        }
        screenCapture = StringeeScreenCapture(activity)
        launcher.launch(projectionManager.createScreenCaptureIntent())
    }

    fun startScreenShare(service: MediaProjectionCallService, permissionData: Intent?) {
        if (screenCapture == null || stringeeCall2 == null || isSharing || released) {
            service.stopService()
            return
        }
        mediaProjectionService = service
        screenCapture!!.createCapture(
            permissionData,
            object : CallbackListener<StringeeVideoTrack?>() {
                override fun onSuccess(track: StringeeVideoTrack?) {
                    stringeeCall2!!.startCaptureScreen(screenCapture, object : StatusListener() {
                        override fun onSuccess() {
                            isSharing = true
                            if (videoEnabled) {
                                videoEnabled = false
                                stringeeCall2!!.enableVideo(false)
                                if (uiListener != null) {
                                    uiListener!!.onVideoChanged(false)
                                }
                            }
                            if (uiListener != null) {
                                uiListener!!.onSharingChanged(true)
                            }
                        }

                        override fun onError(error: StringeeError?) {
                            owner.onSessionError(this@CallSession, "startScreenShare", error)
                            finishSharing()
                        }
                    })
                }

                override fun onError(error: StringeeError?) {
                    owner.onSessionError(this@CallSession, "createScreenCapture", error)
                    finishSharing()
                }
            })
    }

    fun stopScreenShare() {
        if (!isSharing || stringeeCall2 == null) {
            return
        }
        stringeeCall2!!.stopCaptureScreen(object : StatusListener() {
            override fun onSuccess() {
            }
        })
        finishSharing()
    }

    private fun finishSharing() {
        isSharing = false
        screenCapture = null
        if (mediaProjectionService != null) {
            mediaProjectionService!!.stopService()
            mediaProjectionService = null
        }
        if (uiListener != null) {
            uiListener!!.onSharingChanged(false)
        }
    }

    fun onMediaProjectionServiceDestroyed(service: MediaProjectionCallService?) {
        if (mediaProjectionService !== service) {
            return
        }
        mediaProjectionService = null
        if (isSharing && stringeeCall2 != null) {
            stringeeCall2!!.stopCaptureScreen(object : StatusListener() {
                override fun onSuccess() {
                }
            })
        }
        isSharing = false
        screenCapture = null
        if (uiListener != null) {
            uiListener!!.onSharingChanged(false)
        }
    }

    private fun startTimerIfNeeded() {
        if (startedAt == 0L) {
            startedAt = System.currentTimeMillis()
        }
        if (timer != null) {
            return
        }
        timer = Timer()
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startedAt
                val format = SimpleDateFormat("mm:ss", Locale.getDefault())
                format.setTimeZone(TimeZone.getTimeZone("GMT"))
                val value = format.format(Date(elapsed))
                CallUtils.runOnUiThread(Runnable {
                    if (isCurrent && uiListener != null) {
                        uiListener!!.onTimer(value)
                    }
                })
            }
        }, 0, 1000)
    }

    private fun terminal(terminalStatus: CallStatus) {
        if (released) {
            return
        }
        stateMachine.setStatus(terminalStatus)
        if (uiListener != null) {
            uiListener!!.onCallStatus(terminalStatus)
        }
        owner.onSessionStateChanged(this, terminalStatus)
        release()
    }

    fun release() {
        if (released) {
            return
        }
        released = true
        if (!isIncoming && outgoingStarted) {
            setupStateMachine.cancel()
        }
        outgoingCallback = null
        cancelSetupTimeout()
        if (timer != null) {
            timer!!.cancel()
            timer = null
        }
        if (isSharing && stringeeCall2 != null) {
            try {
                stringeeCall2!!.stopCaptureScreen(object : StatusListener() {
                    override fun onSuccess() {
                    }
                })
            } catch (exception: RuntimeException) {
                CallUtils.reportException(CallSession::class.java, exception)
            }
        }
        finishSharing()
        try {
            if (stringeeCall != null) {
                stringeeCall!!.setCallListener(null)
            }
            if (stringeeCall2 != null) {
                stringeeCall2!!.setCallListener(null)
                stringeeCall2!!.setCaptureSessionListener(null)
            }
        } catch (exception: RuntimeException) {
            CallUtils.reportException(CallSession::class.java, exception)
        }
        val releasedCallId = getCallId()
        stringeeCall = null
        stringeeCall2 = null
        remoteScreenTracks.clear()
        audioManager.stopRinging()
        audioManager.stop()
        IncomingCallService.Companion.clear(context, releasedCallId)
        InCallService.Companion.stop(context, generation)
        uiListener = null
        owner.onSessionReleased(this)
    }

    private val isCurrent: Boolean
        get() = !released && owner is StringeeCallManager
                && owner.isCurrentSession(
            this,
            generation
        )

    val status: CallStatus
        get() = stateMachine.status

    fun getCallId(): String? {
        if (setupStateMachine.hasServerCallId()) {
            callId = setupStateMachine.serverCallId
            return callId
        }
        if (!CallUtils.isEmpty(callId)) {
            return callId
        }
        if (engine == CallEngine.STRINGEE_CALL && stringeeCall != null) {
            callId = stringeeCall!!.getCallId()
            return callId
        }
        if (stringeeCall2 != null) {
            callId = stringeeCall2!!.getCallId()
        }
        return callId
    }

    val isOutgoingSetupPending: Boolean
        get() = !isIncoming && setupStateMachine.isPending

    fun onInCallServiceStartFailed() {
        val error = StringeeError(
            -1,
            "Unable to start the in-call foreground service"
        )
        if (this.isOutgoingSetupPending) {
            handleMakeCallFailure(error)
            return
        }
        owner.onSessionError(this, "foregroundService", error)
        hangUp()
    }

    companion object {
        private const val OUTGOING_SETUP_TIMEOUT_MS = 60000L
    }
}
