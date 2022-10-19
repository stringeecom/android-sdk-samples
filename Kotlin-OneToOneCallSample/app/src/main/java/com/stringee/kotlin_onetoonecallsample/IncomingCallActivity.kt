package com.stringee.kotlin_onetoonecallsample

import android.Manifest.permission.*
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.View.*
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall.*
import com.stringee.common.StringeeAudioManager
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.R.drawable.*
import com.stringee.kotlin_onetoonecallsample.R.id
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityCallBinding
import com.stringee.listener.StatusListener
import org.json.JSONObject

class IncomingCallActivity : AppCompatActivity(), OnClickListener {
    private lateinit var binding: ActivityCallBinding
    private var isVideoCall = false
    private var isMute = false
    private var isSpeaker = false
    private var isVideo = false
    private var isPermissionGranted = true

    private var mMediaState: MediaState = MediaState.DISCONNECTED
    private lateinit var mSignalingState: SignalingState

    private lateinit var stringeeCall: StringeeCall
    private lateinit var sensorManagerUtils: SensorManagerUtils
    private var audioManager: StringeeAudioManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //add Flag for show on lockScreen and disable keyguard
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

        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorManagerUtils = SensorManagerUtils.getInstance(this)!!
        sensorManagerUtils.acquireProximitySensor(localClassName)
        sensorManagerUtils.disableKeyguard()

        Common.isInCall = true

        val callId = intent.getStringExtra("call_id")
        if (Common.callsMap[callId] != null) {
            stringeeCall = Common.callsMap[callId]!!
        } else {
            sensorManagerUtils.releaseSensor()
            Common.postDelay({
                Common.isInCall = false
                finish()
            }, 1000)
            return
        }

        initView()

        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            val lstPermissions: MutableList<String> = ArrayList()
            if (checkSelfPermission(this, RECORD_AUDIO) != PERMISSION_GRANTED) {
                lstPermissions.add(RECORD_AUDIO)
            }
            if (isVideoCall) {
                if (checkSelfPermission(this, CAMERA) != PERMISSION_GRANTED) {
                    lstPermissions.add(CAMERA)
                }
            }
            if (VERSION.SDK_INT >= VERSION_CODES.S) {
                if (checkSelfPermission(this, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
                    lstPermissions.add(BLUETOOTH_CONNECT)
                }
            }
            if (lstPermissions.isNotEmpty()) {
                val permissions = arrayOfNulls<String>(lstPermissions.size)
                for (i in lstPermissions.indices) {
                    permissions[i] = lstPermissions[i]
                }
                ActivityCompat.requestPermissions(this, permissions, Common.REQUEST_PERMISSION_CALL)
                return
            }
        }

        startRinging()
    }

    override fun onBackPressed() {
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
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
        if (requestCode == Common.REQUEST_PERMISSION_CALL) {
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
        binding.tvName.text = stringeeCall.from

        binding.btnAnswer.setOnClickListener(this)
        binding.btnEnd.setOnClickListener(this)
        binding.btnReject.setOnClickListener(this)
        binding.btnMute.setOnClickListener(this)
        binding.btnSpeaker.setOnClickListener(this)
        binding.btnVideo.setOnClickListener(this)
        binding.btnSwitch.setOnClickListener(this)

        isSpeaker = stringeeCall.isVideoCall
        binding.btnSpeaker.setBackgroundResource(if (isSpeaker) btn_speaker_on else btn_speaker_off)

        isVideo = stringeeCall.isVideoCall
        binding.btnVideo.setBackgroundResource(if (isVideo) btn_video else btn_video_off)

        binding.btnVideo.visibility = if (isVideo) VISIBLE else GONE
        binding.btnSwitch.visibility = if (isVideo) VISIBLE else GONE

        binding.vIncoming.visibility = VISIBLE
        binding.vControl.visibility = GONE
        binding.btnEnd.visibility = GONE
    }

    private fun startRinging() {
        //create audio manager to control audio device
        audioManager = StringeeAudioManager.create(this@IncomingCallActivity)
        audioManager?.start { selectedAudioDevice, availableAudioDevices ->
            Log.d(
                Common.TAG,
                "selectedAudioDevice: $selectedAudioDevice - availableAudioDevices: $availableAudioDevices"
            )
        }
        audioManager?.setSpeakerphoneOn(isVideoCall)

        stringeeCall.setCallListener(object : StringeeCallListener {
            override fun onSignalingStateChange(
                stringeeCall: StringeeCall,
                signalingState: SignalingState,
                reason: String,
                sipCode: Int,
                sipReason: String
            ) {
                runOnUiThread {
                    Log.d(Common.TAG, "onSignalingStateChange: $signalingState")
                    mSignalingState = signalingState
                    if (signalingState == SignalingState.ANSWERED) {
                        binding.tvState.text = "Starting"
                        if (mMediaState == MediaState.CONNECTED) {
                            binding.tvState.text = "Started"
                        }
                    } else if (signalingState == SignalingState.ENDED) {
                        endCall(true)
                    }
                }
            }

            override fun onError(stringeeCall: StringeeCall, code: Int, desc: String) {
                runOnUiThread {
                    Log.d(Common.TAG, "onError: $desc")
                    Common.reportMessage(this@IncomingCallActivity, desc)
                    binding.tvState.text = "Ended"
                    dismissLayout()
                }
            }

            override fun onHandledOnAnotherDevice(
                stringeeCall: StringeeCall,
                signalingState: SignalingState,
                desc: String
            ) {
                runOnUiThread {
                    Log.d(Common.TAG, "onHandledOnAnotherDevice: $desc")
                    if (signalingState != SignalingState.RINGING) {
                        Common.reportMessage(this@IncomingCallActivity, desc)
                        binding.tvState.text = "Ended"
                        dismissLayout()
                    }
                }
            }

            override fun onMediaStateChange(stringeeCall: StringeeCall, mediaState: MediaState) {
                runOnUiThread {
                    Log.d(Common.TAG, "onMediaStateChange: $mediaState")
                    mMediaState = mediaState
                    if (mediaState == MediaState.CONNECTED) {
                        if (mSignalingState == SignalingState.ANSWERED) {
                            binding.tvState.text = "Started"
                        }
                    } else {
                        binding.tvState.text = "Reconnecting..."
                    }
                }
            }

            override fun onLocalStream(stringeeCall: StringeeCall) {
                runOnUiThread {
                    Log.d(Common.TAG, "onLocalStream")
                    if (stringeeCall.isVideoCall) {
                        binding.vLocal.removeAllViews()
                        binding.vLocal.addView(stringeeCall.localView)
                        stringeeCall.renderLocalView(true)
                    }
                }
            }

            override fun onRemoteStream(stringeeCall: StringeeCall) {
                runOnUiThread {
                    Log.d(Common.TAG, "onRemoteStream")
                    if (stringeeCall.isVideoCall) {
                        binding.vRemote.removeAllViews()
                        binding.vRemote.addView(stringeeCall.remoteView)
                        stringeeCall.renderRemoteView(false)
                    }
                }
            }

            override fun onCallInfo(stringeeCall: StringeeCall, jsonObject: JSONObject) {
                runOnUiThread {
                    Log.d(
                        Common.TAG,
                        "onCallInfo: $jsonObject"
                    )
                }
            }
        })

        stringeeCall.ringing(object : StatusListener() {
            override fun onSuccess() {
                Log.d("Stringee", "ringing success")
            }

            override fun onError(stringeeError: StringeeError) {
                super.onError(stringeeError)
                runOnUiThread {
                    Log.d(Common.TAG, "ringing error: " + stringeeError.message)
                    Common.reportMessage(this@IncomingCallActivity, stringeeError.message)
                    endCall(false)
                }
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            id.btn_mute -> {
                isMute = !isMute
                binding.btnMute.setBackgroundResource(if (isMute) btn_mute else btn_mic)
                stringeeCall.mute(isMute)
            }
            id.btn_speaker -> {
                isSpeaker = !isSpeaker
                binding.btnSpeaker.setBackgroundResource(if (isSpeaker) btn_speaker_on else btn_speaker_off)
                audioManager?.setSpeakerphoneOn(isSpeaker)
            }
            id.btn_answer -> {
                binding.vControl.visibility = VISIBLE
                binding.vIncoming.visibility = GONE
                binding.btnEnd.visibility = VISIBLE
                binding.btnSwitch.visibility = if (isVideoCall) VISIBLE else GONE
                stringeeCall.answer(object : StatusListener() {
                    override fun onSuccess() {
                    }
                })
            }
            id.btn_end -> {
                endCall(true)
            }
            id.btn_reject -> {
                endCall(false)
            }
            id.btn_video -> {
                isVideo = !isVideo
                binding.btnVideo.setImageResource(if (isVideo) btn_video else btn_video_off)
                stringeeCall.enableVideo(isVideo)
            }
            id.btn_switch -> {
                stringeeCall.switchCamera(object : StatusListener() {
                    override fun onSuccess() {
                    }

                    override fun onError(stringeeError: StringeeError) {
                        super.onError(stringeeError)
                        runOnUiThread {
                            Log.d(Common.TAG, "switchCamera error: ${stringeeError.message}")
                            Common.reportMessage(this@IncomingCallActivity, stringeeError.message)
                        }
                    }
                })
            }
        }
    }

    private fun endCall(isHangup: Boolean) {
        binding.tvState.text = "Ended"
        if (isHangup) {
            stringeeCall.hangup(object : StatusListener() {
                override fun onSuccess() {
                }
            })
        } else {
            stringeeCall.reject(object : StatusListener() {
                override fun onSuccess() {
                }
            })
        }
        dismissLayout()
    }

    private fun dismissLayout() {
        audioManager?.stop()
        audioManager = null
        sensorManagerUtils.releaseSensor()
        binding.vControl.visibility = GONE
        binding.vIncoming.visibility = GONE
        binding.btnEnd.visibility = GONE
        binding.btnSwitch.visibility = GONE
        Common.postDelay({
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