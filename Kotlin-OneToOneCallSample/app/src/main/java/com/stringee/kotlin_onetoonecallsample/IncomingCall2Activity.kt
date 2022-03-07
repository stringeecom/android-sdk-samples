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
import androidx.core.app.ActivityCompat.checkSelfPermission
import com.stringee.call.StringeeCall2
import com.stringee.call.StringeeCall2.*
import com.stringee.common.StringeeAudioManager
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.R.drawable.*
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityCallBinding
import com.stringee.listener.StatusListener
import com.stringee.video.StringeeVideoTrack
import org.json.JSONObject

class IncomingCall2Activity : AppCompatActivity(), OnClickListener {
    private lateinit var binding: ActivityCallBinding
    private var isVideoCall = false
    private var isMute = false
    private var isSpeaker = false
    private var isVideo = false
    private var isPermissionGranted = true

    // For normal device has more than 3 cameras, 0 is back camera, 1 is front camera.
    // Some device is different, must check camera id before select.
    // When call starts, automatically use the front camera.
    private var cameraId = 1

    private var mMediaState: MediaState = MediaState.DISCONNECTED
    private lateinit var mSignalingState: SignalingState

    private lateinit var stringeeCall2: StringeeCall2
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
        if (Common.call2sMap[callId] != null) {
            stringeeCall2 = Common.call2sMap[callId]!!
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
        binding.tvName.text = stringeeCall2.from

        binding.btnAnswer.setOnClickListener(this)
        binding.btnEnd.setOnClickListener(this)
        binding.btnReject.setOnClickListener(this)
        binding.btnMute.setOnClickListener(this)
        binding.btnSpeaker.setOnClickListener(this)
        binding.btnVideo.setOnClickListener(this)
        binding.btnSwitch.setOnClickListener(this)

        isSpeaker = stringeeCall2.isVideoCall
        binding.btnSpeaker.setBackgroundResource(if (isSpeaker) btn_speaker_on else btn_speaker_off)

        isVideo = stringeeCall2.isVideoCall
        binding.btnVideo.setBackgroundResource(if (isVideo) btn_video else btn_video_off)

        binding.btnVideo.visibility = if (isVideo) VISIBLE else GONE
        binding.btnSwitch.visibility = if (isVideo) VISIBLE else GONE

        binding.vIncoming.visibility = VISIBLE
        binding.vControl.visibility = GONE
        binding.btnEnd.visibility = GONE
    }

    private fun startRinging() {
        //create audio manager to control audio device
        audioManager = StringeeAudioManager.create(this@IncomingCall2Activity)
        audioManager?.start { selectedAudioDevice, availableAudioDevices ->
            Log.d(
                Common.TAG,
                "selectedAudioDevice: $selectedAudioDevice - availableAudioDevices: $availableAudioDevices"
            )
        }
        audioManager?.setSpeakerphoneOn(isVideoCall)

        stringeeCall2.setCallListener(object : StringeeCallListener {
            override fun onSignalingStateChange(
                stringeeCall2: StringeeCall2,
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

            override fun onError(stringeeCall2: StringeeCall2, i: Int, desc: String) {
                runOnUiThread {
                    Log.d(Common.TAG, "onError: $desc")
                    Common.reportMessage(this@IncomingCall2Activity, desc)
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
                    Log.d(Common.TAG, "onHandledOnAnotherDevice: $desc")
                    if (signalingState != SignalingState.RINGING) {
                        Common.reportMessage(this@IncomingCall2Activity, desc)
                        binding.tvState.text = "Ended"
                        dismissLayout()
                    }
                }
            }

            override fun onMediaStateChange(stringeeCall2: StringeeCall2, mediaState: MediaState) {
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

            override fun onLocalStream(stringeeCall2: StringeeCall2) {
                runOnUiThread {
                    Log.d(Common.TAG, "onLocalStream")
                    if (stringeeCall2.isVideoCall) {
                        binding.vLocal.removeAllViews()
                        binding.vLocal.addView(stringeeCall2.localView)
                        stringeeCall2.renderLocalView(true)
                    }
                }
            }

            override fun onRemoteStream(stringeeCall2: StringeeCall2) {
                runOnUiThread {
                    Log.d(Common.TAG, "onRemoteStream")
                    if (stringeeCall2.isVideoCall) {
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
                        Common.TAG,
                        "onCallInfo: $jsonObject"
                    )
                }
            }
        })

        stringeeCall2.ringing(object : StatusListener() {
            override fun onSuccess() {
                Log.d("Stringee", "send ringing success")
            }

            override fun onError(stringeeError: StringeeError) {
                super.onError(stringeeError)
                runOnUiThread {
                    Log.d(Common.TAG, "ringing error: ${stringeeError.message}")
                    Common.reportMessage(this@IncomingCall2Activity, stringeeError.message)
                    endCall(false)
                }
            }
        })
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_mute -> {
                isMute = !isMute
                binding.btnMute.setBackgroundResource(if (isMute) btn_mute else btn_mic)
                stringeeCall2.mute(isMute)
            }
            R.id.btn_speaker -> {
                isSpeaker = !isSpeaker
                binding.btnSpeaker.setBackgroundResource(if (isSpeaker) btn_speaker_on else btn_speaker_off)
                audioManager?.setSpeakerphoneOn(isSpeaker)
            }
            R.id.btn_answer -> {
                binding.vControl.visibility = VISIBLE
                binding.vIncoming.visibility = GONE
                binding.btnEnd.visibility = VISIBLE
                binding.btnSwitch.visibility = if (isVideoCall) VISIBLE else GONE
                stringeeCall2.answer()
            }
            R.id.btn_end -> {
                endCall(true)
            }
            R.id.btn_reject -> {
                endCall(false)
            }
            R.id.btn_video -> {
                isVideo = !isVideo
                binding.btnVideo.setImageResource(if (isVideo) btn_video else btn_video_off)
                stringeeCall2.enableVideo(isVideo)
            }
            R.id.btn_switch -> {
                stringeeCall2.switchCamera(object : StatusListener() {
                    override fun onSuccess() {
                        cameraId = if (cameraId == 0) 1 else 0
                    }

                    override fun onError(stringeeError: StringeeError) {
                        super.onError(stringeeError)
                        runOnUiThread {
                            Log.d(Common.TAG, "switchCamera error: ${stringeeError.message}")
                            Common.reportMessage(this@IncomingCall2Activity, stringeeError.message)
                        }
                    }
                }, if (cameraId == 0) 1 else 0)
            }
        }
    }

    private fun endCall(isHangup: Boolean) {
        binding.tvState.text = "Ended"
        if (isHangup) {
            stringeeCall2.hangup()
        } else {
            stringeeCall2.reject()
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