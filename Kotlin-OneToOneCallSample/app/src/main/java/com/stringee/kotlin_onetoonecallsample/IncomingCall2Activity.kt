package com.stringee.kotlin_onetoonecallsample

import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.WindowManager
import com.stringee.call.StringeeCall2
import com.stringee.call.StringeeCall2.*
import com.stringee.common.StringeeAudioManager
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.R.drawable.*
import com.stringee.kotlin_onetoonecallsample.R.id
import com.stringee.kotlin_onetoonecallsample.common.*
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityIncomingCallBinding
import com.stringee.listener.StatusListener
import com.stringee.video.StringeeVideoTrack
import org.json.JSONObject

class IncomingCall2Activity : BaseActivity() {
    private lateinit var binding: ActivityIncomingCallBinding
    private var stringeeCall2: StringeeCall2? = null
    private var sensorManagerUtils: StringeeSensorManager? = null
    private var audioManager: StringeeAudioManager? = null
    private var isMute = false
    private var isSpeaker = false
    private var isVideo = false
    private var isPermissionGranted = true
    private var isVideoCall = false
    private var mMediaState: MediaState? = null
    private var mSignalingState: SignalingState? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //add Flag for show on lockScreen, disable keyguard, keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sensorManagerUtils = StringeeSensorManager.getInstance(this)?.initialize()!!
        Common.isInCall = true
        val callId: String? = intent.getStringExtra("call_id")
        stringeeCall2 = Common.calls2Map[callId]
        if (stringeeCall2 == null) {
            sensorManagerUtils!!.releaseSensor()
            Utils.postDelay({
                Common.isInCall = false
                RingtoneUtils.getInstance(this@IncomingCall2Activity)?.stopRinging()
                finish()
            }, 1000)
            return
        }
        isVideoCall = stringeeCall2!!.isVideoCall
        initView()

        // Check permission
        if (isVideoCall) {
            if (!PermissionsUtils.isVideoCallPermissionGranted(this)) {
                PermissionsUtils.requestVideoCallPermission(this)
                return
            }
        } else {
            if (!PermissionsUtils.isVoiceCallPermissionGranted(this)) {
                PermissionsUtils.requestVoiceCallPermission(this)
                return
            }
        }
        startRinging()
    }

    override fun onPause() {
        super.onPause()
        runOnUiThread {
            if (mSignalingState == SignalingState.CALLING || mSignalingState == SignalingState.RINGING || mSignalingState == SignalingState.ANSWERED) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
                    setShowWhenLocked(false)
                    setTurnScreenOn(false)
                }
                sensorManagerUtils =
                    StringeeSensorManager.getInstance(this@IncomingCall2Activity)?.initialize()!!
                sensorManagerUtils!!.turnOff()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        runOnUiThread {
            if (mSignalingState == SignalingState.CALLING || mSignalingState == SignalingState.RINGING || mSignalingState == SignalingState.ANSWERED) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true)
                    setTurnScreenOn(true)
                }
                sensorManagerUtils =
                    StringeeSensorManager.getInstance(this@IncomingCall2Activity)?.initialize()!!
                if (mSignalingState == SignalingState.ANSWERED && mMediaState == MediaState.CONNECTED && !isVideoCall) {
                    sensorManagerUtils!!.turnOn()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {}
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var isGranted = false
        if (grantResults.isNotEmpty()) {
            for (grantResult in grantResults) {
                if (grantResult != PERMISSION_GRANTED) {
                    isGranted = false
                    break
                } else {
                    isGranted = true
                }
            }
        }
        if (requestCode == Constant.REQUEST_PERMISSION_CALL) {
            if (!isGranted) {
                isPermissionGranted = false
                endCall(false)
            } else {
                isPermissionGranted = true
                startRinging()
            }
        }
    }

    private fun initView() {
        binding.tvFrom.text = stringeeCall2!!.from
        binding.btnAnswer.setOnClickListener(this)
        binding.btnEnd.setOnClickListener(this)
        binding.btnReject.setOnClickListener(this)
        binding.btnMute.setOnClickListener(this)
        binding.btnSpeaker.setOnClickListener(this)
        binding.btnVideo.setOnClickListener(this)
        binding.btnSwitch.setOnClickListener(this)
        isSpeaker = isVideoCall
        binding.btnSpeaker.setBackgroundResource(if (isSpeaker) btn_speaker_on else btn_speaker_off)
        isVideo = isVideoCall
        binding.btnVideo.setImageResource(if (isVideo) btn_video else btn_video_off)
        binding.btnVideo.visibility = if (isVideo) VISIBLE else GONE
        binding.btnSwitch.visibility = if (isVideo) VISIBLE else GONE
    }

    private fun startRinging() {
        //create audio manager to control audio device
        audioManager = StringeeAudioManager.create(this@IncomingCall2Activity)
        audioManager?.start { selectedAudioDevice: StringeeAudioManager.AudioDevice, availableAudioDevices: Set<StringeeAudioManager.AudioDevice?> ->
            Log.d(
                Constant.TAG,
                "selectedAudioDevice: $selectedAudioDevice - availableAudioDevices: $availableAudioDevices"
            )
        }
        audioManager?.setSpeakerphoneOn(isVideo)
        stringeeCall2!!.setCallListener(object : StringeeCallListener {
            override fun onSignalingStateChange(
                stringeeCall2: StringeeCall2,
                signalingState: SignalingState,
                reason: String,
                sipCode: Int,
                sipReason: String
            ) {
                runOnUiThread {
                    Log.d(Constant.TAG, "onSignalingStateChange: $signalingState")
                    mSignalingState = signalingState
                    if (signalingState == SignalingState.ANSWERED) {
                        binding.tvState.text = "Starting"
                        if (mMediaState == MediaState.CONNECTED) {
                            binding.tvState.text = "Started"
                            RingtoneUtils.getInstance(this@IncomingCall2Activity)?.stopRinging()
                            if (!isVideoCall) {
                                sensorManagerUtils?.turnOn()
                            }
                        }
                    } else if (signalingState == SignalingState.ENDED) {
                        endCall(true)
                    }
                }
            }

            override fun onError(stringeeCall2: StringeeCall2, i: Int, desc: String) {
                runOnUiThread {
                    Log.d(Constant.TAG, "onError: $desc")
                    Utils.reportMessage(this@IncomingCall2Activity, desc)
                    binding.tvState.text = "Ended"
                    dismissLayout()
                }
            }

            override fun onHandledOnAnotherDevice(
                stringeeCall2: StringeeCall2,
                signalingState: SignalingState,
                desc: String
            ) {
                runOnUiThread {
                    Log.d(Constant.TAG, "onHandledOnAnotherDevice: $desc")
                    if (signalingState != SignalingState.RINGING) {
                        Utils.reportMessage(this@IncomingCall2Activity, desc)
                        binding.tvState.text = "Ended"
                        dismissLayout()
                    }
                }
            }

            override fun onMediaStateChange(stringeeCall2: StringeeCall2, mediaState: MediaState) {
                runOnUiThread {
                    Log.d(Constant.TAG, "onMediaStateChange: $mediaState")
                    mMediaState = mediaState
                    if (mediaState == MediaState.CONNECTED) {
                        if (mSignalingState == SignalingState.ANSWERED) {
                            binding.tvState.text = "Started"
                            RingtoneUtils.getInstance(this@IncomingCall2Activity)?.stopRinging()
                            if (!isVideoCall) {
                                sensorManagerUtils?.turnOn()
                            }
                        }
                    } else {
                        binding.tvState.text = "Reconnecting..."
                    }
                }
            }

            override fun onLocalStream(stringeeCall2: StringeeCall2) {
                runOnUiThread {
                    Log.d(Constant.TAG, "onLocalStream")
                    if (isVideoCall) {
                        binding.vLocal.removeAllViews()
                        binding.vLocal.addView(stringeeCall2.localView)
                        stringeeCall2.renderLocalView(true)
                    }
                }
            }

            override fun onRemoteStream(stringeeCall2: StringeeCall2) {
                runOnUiThread {
                    Log.d(Constant.TAG, "onRemoteStream")
                    if (isVideoCall) {
                        binding.vRemote.removeAllViews()
                        binding.vRemote.addView(stringeeCall2.remoteView)
                        stringeeCall2.renderRemoteView(false)
                    }
                }
            }

            override fun onVideoTrackAdded(stringeeVideoTrack: StringeeVideoTrack) {}
            override fun onVideoTrackRemoved(stringeeVideoTrack: StringeeVideoTrack) {}
            override fun onCallInfo(stringeeCall2: StringeeCall2, jsonObject: JSONObject) {
                runOnUiThread {
                    Log.d(
                        Constant.TAG,
                        "onCallInfo: $jsonObject"
                    )
                }
            }

            override fun onTrackMediaStateChange(
                from: String,
                mediaType: StringeeVideoTrack.MediaType,
                enable: Boolean
            ) {
            }
        })
        stringeeCall2!!.ringing(object : StatusListener() {
            override fun onSuccess() {
                Log.d("Stringee", "send ringing success")
            }

            override fun onError(stringeeError: StringeeError) {
                super.onError(stringeeError)
                runOnUiThread {
                    Log.d(Constant.TAG, "ringing error: " + stringeeError.message)
                    Utils.reportMessage(this@IncomingCall2Activity, stringeeError.message)
                    endCall(false)
                }
            }
        })
    }

    override fun onClick(view: View) {
        val vId = view.id
        if (vId == id.btn_mute) {
            isMute = !isMute
            binding.btnMute.setBackgroundResource(if (isMute) btn_mute else btn_mic)
            if (stringeeCall2 != null) {
                stringeeCall2!!.mute(isMute)
            }
        } else if (vId == id.btn_speaker) {
            isSpeaker = !isSpeaker
            binding.btnSpeaker.setBackgroundResource(if (isSpeaker) btn_speaker_on else btn_speaker_off)
            if (audioManager != null) {
                audioManager!!.setSpeakerphoneOn(isSpeaker)
            }
        } else if (vId == id.btn_answer) {
            if (stringeeCall2 != null) {
                binding.vControl.visibility = VISIBLE
                binding.vIncoming.visibility = GONE
                binding.btnEnd.visibility = VISIBLE
                binding.btnSwitch.visibility = if (isVideoCall) VISIBLE else GONE
                stringeeCall2!!.answer(object : StatusListener() {
                    override fun onSuccess() {}
                })
            }
        } else if (vId == id.btn_end) {
            endCall(true)
        } else if (vId == id.btn_reject) {
            endCall(false)
        } else if (vId == id.btn_video) {
            isVideo = !isVideo
            binding.btnVideo.setImageResource(if (isVideo) btn_video else btn_video_off)
            if (stringeeCall2 != null) {
                stringeeCall2!!.enableVideo(isVideo)
            }
        } else if (vId == id.btn_switch) {
            if (stringeeCall2 != null) {
                stringeeCall2!!.switchCamera(object : StatusListener() {
                    override fun onSuccess() {}
                    override fun onError(stringeeError: StringeeError) {
                        super.onError(stringeeError)
                        runOnUiThread {
                            Log.d(
                                Constant.TAG,
                                "switchCamera error: " + stringeeError.message
                            )
                            Utils.reportMessage(
                                this@IncomingCall2Activity,
                                stringeeError.message
                            )
                        }
                    }
                })
            }
        }
    }

    private fun endCall(isHangup: Boolean) {
        binding.tvState.text = "Ended"
        if (stringeeCall2 != null) {
            if (isHangup) {
                stringeeCall2!!.hangup(object : StatusListener() {
                    override fun onSuccess() {}
                })
            } else {
                stringeeCall2!!.reject(object : StatusListener() {
                    override fun onSuccess() {}
                })
            }
        }
        dismissLayout()
    }

    private fun dismissLayout() {
        if (audioManager != null) {
            audioManager!!.stop()
            audioManager = null
        }
        RingtoneUtils.getInstance(this@IncomingCall2Activity)?.stopRinging()
        binding.vControl.visibility = GONE
        binding.vIncoming.visibility = GONE
        binding.btnEnd.visibility = GONE
        binding.btnSwitch.visibility = GONE
        sensorManagerUtils?.releaseSensor()
        Utils.postDelay({
            Common.isInCall = false
            if (!isPermissionGranted) {
                val intent = Intent()
                intent.action = "open_app_setting"
                setResult(RESULT_CANCELED, intent)
            }
            finish()
        }, 1000)
    }
}
