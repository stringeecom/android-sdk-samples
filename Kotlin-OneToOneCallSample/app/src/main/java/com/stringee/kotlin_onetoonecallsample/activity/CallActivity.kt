package com.stringee.kotlin_onetoonecallsample.activity

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.stringee.kotlin_onetoonecallsample.R.drawable
import com.stringee.kotlin_onetoonecallsample.R.id
import com.stringee.kotlin_onetoonecallsample.common.CallStatus
import com.stringee.kotlin_onetoonecallsample.common.Constant
import com.stringee.kotlin_onetoonecallsample.common.SensorManagerUtils
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityVideoCallBinding
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityVoiceCallBinding
import com.stringee.kotlin_onetoonecallsample.databinding.LayoutIncomingCallBinding
import com.stringee.kotlin_onetoonecallsample.listener.OnCallListener
import com.stringee.kotlin_onetoonecallsample.manager.CallManager


class CallActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var videoCallBinding: ActivityVideoCallBinding
    private lateinit var voiceCallBinding: ActivityVoiceCallBinding
    private lateinit var incomingCallBinding: LayoutIncomingCallBinding
    private var callManager: CallManager? = null
    private var sensorManagerUtils: SensorManagerUtils? = null
    private var isVideoCall = false
    private var isIncomingCall = false
    private var isStringeeCall = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        videoCallBinding = ActivityVideoCallBinding.inflate(layoutInflater)
        voiceCallBinding = ActivityVoiceCallBinding.inflate(layoutInflater)
        isVideoCall = intent.getBooleanExtra(Constant.PARAM_IS_VIDEO_CALL, false)
        setContentView(if (isVideoCall) videoCallBinding.root else voiceCallBinding.root)
        incomingCallBinding =
            if (isVideoCall) videoCallBinding.vIncomingCall else voiceCallBinding.vIncomingCall

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {

            }
        }
        onBackPressedDispatcher.addCallback(this, callback)

        isIncomingCall = intent.getBooleanExtra(Constant.PARAM_IS_INCOMING_CALL, false)
        isStringeeCall = intent.getBooleanExtra(Constant.PARAM_IS_STRINGEE_CALL, false)
        callManager = CallManager.getInstance(this)
        sensorManagerUtils = SensorManagerUtils.getInstance(this).initialize(localClassName)
        if (!isVideoCall) {
            sensorManagerUtils!!.turnOn()
        }
        incomingCallBinding.btnAnswer.setOnClickListener(this)
        incomingCallBinding.btnReject.setOnClickListener(this)
        if (!isVideoCall) {
            voiceCallBinding.btnEnd.setOnClickListener(this)
            voiceCallBinding.btnMute.setOnClickListener(this)
            voiceCallBinding.btnSpeaker.setOnClickListener(this)
        } else {
            videoCallBinding.btnEnd.setOnClickListener(this)
            videoCallBinding.btnMute.setOnClickListener(this)
            videoCallBinding.btnCamera.setOnClickListener(this)
            videoCallBinding.btnSwitch.setOnClickListener(this)
        }
        incomingCallBinding.root.visibility =
            if (callManager!!.callStatus != CallStatus.INCOMING) View.GONE else View.VISIBLE
        if (isVideoCall) {
            videoCallBinding.vInCall.visibility =
                if (callManager!!.callStatus != CallStatus.INCOMING) View.VISIBLE else View.GONE
        } else {
            voiceCallBinding.vInCall.visibility =
                if (callManager!!.callStatus != CallStatus.INCOMING) View.VISIBLE else View.GONE
        }
        initCall()
    }

    override fun onPause() {
        super.onPause()
        runOnUiThread {
            if (callManager!!.callStatus == CallStatus.STARTED || callManager!!.callStatus == CallStatus.CALLING || callManager!!.callStatus == CallStatus.RINGING) {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
                sensorManagerUtils =
                    SensorManagerUtils.getInstance(this).initialize(localClassName)
                if (!isVideoCall) {
                    sensorManagerUtils!!.turnOff()
                }
                if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
                    setShowWhenLocked(false)
                    setTurnScreenOn(false)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        runOnUiThread {
            if (callManager!!.callStatus == CallStatus.STARTED || callManager!!.callStatus == CallStatus.CALLING || callManager!!.callStatus == CallStatus.RINGING) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                            or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
                sensorManagerUtils =
                    SensorManagerUtils.getInstance(this).initialize(localClassName)
                if (!isVideoCall) {
                    sensorManagerUtils!!.turnOn()
                }
                if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true)
                    setTurnScreenOn(true)
                }
            }
        }
    }

    private fun initCall() {
        callManager!!.registerEvent(object : OnCallListener {
            override fun onCallStatus(status: CallStatus?) {
                runOnUiThread {
                    if (!isVideoCall) {
                        voiceCallBinding.tvStatus.text = status!!.value
                    }
                    incomingCallBinding.root.visibility =
                        if (status != CallStatus.INCOMING) View.GONE else View.VISIBLE
                    if (isVideoCall) {
                        videoCallBinding.vInCall.visibility =
                            if (status != CallStatus.INCOMING) View.VISIBLE else View.GONE
                    } else {
                        voiceCallBinding.vInCall.visibility =
                            if (status != CallStatus.INCOMING) View.VISIBLE else View.GONE
                    }
                    if (status == CallStatus.ENDED || status == CallStatus.BUSY) {
                        dismiss()
                    }
                }
            }

            override fun onError(message: String?) {
                runOnUiThread { dismiss() }
            }

            override fun onReceiveLocalStream() {
                runOnUiThread {
                    if (isVideoCall) {
                        val childParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        childParams.gravity = Gravity.CENTER
                        videoCallBinding.vLocal.removeAllViews()
                        videoCallBinding.vLocal.addView(callManager!!.localView, childParams)
                        callManager!!.renderLocalView()
                    }
                }
            }

            override fun onReceiveRemoteStream() {
                runOnUiThread {
                    if (isVideoCall) {
                        val childParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                        childParams.gravity = Gravity.CENTER
                        videoCallBinding.vRemote.removeAllViews()
                        videoCallBinding.vRemote.addView(callManager!!.remoteView, childParams)
                        callManager!!.renderRemoteView()
                    }
                }
            }

            override fun onSpeakerChange(isOn: Boolean) {
                runOnUiThread {
                    voiceCallBinding.btnSpeaker.setBackgroundResource(if (isOn) drawable.btn_ic_selector else drawable.btn_ic_selected_selector)
                    voiceCallBinding.btnSpeaker.setImageResource(if (isOn) drawable.ic_speaker_on else drawable.ic_speaker_off)
                }
            }

            override fun onMicChange(isOn: Boolean) {
                runOnUiThread {
                    if (isVideoCall) {
                        videoCallBinding.btnMute.setBackgroundResource(if (!isOn) drawable.btn_ic_selector else drawable.btn_ic_selected_selector)
                        videoCallBinding.btnMute.setImageResource(if (!isOn) drawable.ic_mic_off else drawable.ic_mic_on)
                    } else {
                        voiceCallBinding.btnMute.setBackgroundResource(if (!isOn) drawable.btn_ic_selector else drawable.btn_ic_selected_selector)
                        voiceCallBinding.btnMute.setImageResource(if (!isOn) drawable.ic_mic_off else drawable.ic_mic_on)
                    }
                }
            }

            override fun onVideoChange(isOn: Boolean) {
                runOnUiThread {
                    videoCallBinding.btnCamera.setBackgroundResource(if (isOn) drawable.btn_ic_selected_selector else drawable.btn_ic_selector)
                    videoCallBinding.btnCamera.setImageResource(if (isOn) drawable.ic_cam_on else drawable.ic_cam_off)
                }
            }

            override fun onTimer(duration: String?) {
                runOnUiThread {
                    if (!isVideoCall) {
                        voiceCallBinding.tvTime.text = duration
                    }
                }
            }
        })
        if (!isIncomingCall) {
            val to = intent.getStringExtra(Constant.PARAM_TO)
            if (!isVideoCall) {
                voiceCallBinding.tvUser1.text = to
            }
            callManager!!.initializedOutgoingCall(to, isVideoCall, isStringeeCall)
            callManager!!.makeCall()
        } else {
            incomingCallBinding.tvUser.text = callManager!!.from
            if (!isVideoCall) {
                voiceCallBinding.tvUser1.text = callManager!!.from
            }
            val isAnswerFromPush =
                intent.getBooleanExtra(Constant.PARAM_ACTION_ANSWER_FROM_PUSH, false)
            if (isAnswerFromPush) {
                callManager!!.answer()
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            id.btn_answer -> {
                callManager!!.answer()
            }

            id.btn_reject -> {
                callManager!!.endCall(false)
            }

            id.btn_end -> {
                callManager!!.endCall(true)
            }

            id.btn_mute -> {
                callManager!!.mute()
            }

            id.btn_speaker -> {
                callManager!!.changeSpeaker()
            }

            id.btn_camera -> {
                callManager!!.enableVideo()
            }

            id.btn_switch -> {
                callManager!!.switchCamera()
            }
        }
    }

    private fun dismiss() {
        sensorManagerUtils!!.releaseSensor()
        callManager!!.release()
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
        finish()
    }
}