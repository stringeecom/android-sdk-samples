package com.stringee.kotlin_onetoonecallsample

import android.Manifest.*
import android.app.KeyguardManager
import android.app.KeyguardManager.KeyguardDismissCallback
import android.content.pm.PackageManager.*
import android.os.Build.*
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.view.View.*
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.*
import androidx.core.content.ContextCompat.*
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall.*
import com.stringee.common.StringeeAudioManager
import com.stringee.kotlin_onetoonecallsample.R.drawable.*
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityCallBinding
import com.stringee.listener.StatusListener
import org.json.JSONObject

class OutgoingCallActivity : AppCompatActivity(), OnClickListener {
    private lateinit var binding: ActivityCallBinding
    private lateinit var from: String
    private lateinit var to: String
    private var isVideoCall = false
    private var isMute = false
    private var isSpeaker = false
    private var isVideo = false

    private var mMediaState: MediaState = MediaState.DISCONNECTED
    private lateinit var mSignalingState: SignalingState

    private var call: StringeeCall? = null
    private var audioManager: StringeeAudioManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val lock: KeyguardManager = getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            lock.requestDismissKeyguard(this, object : KeyguardDismissCallback() {
            })
        }
        if (VERSION.SDK_INT >= VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        Common.isInCall = true

        from = Common.client.userId
        to = intent.getStringExtra("to")!!
        isVideoCall = intent.getBooleanExtra("is_video_call", false)
        isSpeaker = isVideoCall
        isVideo = isVideoCall

        initView()
        if (isPermissionGranted())
            prepareCall()
    }

    private fun initView() {
        binding.tvName.text = to
        binding.btnMute.setOnClickListener(this)
        binding.btnSpeaker.setOnClickListener(this)
        binding.btnSwitch.setOnClickListener(this)
        binding.btnVideo.setOnClickListener(this)
        binding.btnEnd.setOnClickListener(this)

        binding.btnSpeaker.setBackgroundResource(if (isSpeaker) btn_speaker_on else btn_speaker_off)
        binding.btnVideo.setBackgroundResource(if (isVideo) btn_video else btn_video_off)
        binding.btnVideo.visibility = if (isVideo) VISIBLE else GONE
        binding.btnSwitch.visibility = if (isVideo) VISIBLE else GONE
    }

    private fun isPermissionGranted(): Boolean {
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            val lstPermissions: MutableList<String> = ArrayList()
            if (checkSelfPermission(
                    this,
                    permission.RECORD_AUDIO
                ) != PERMISSION_GRANTED
            )
                lstPermissions.add(permission.RECORD_AUDIO)

            if (isVideoCall) {
                if (checkSelfPermission(
                        this,
                        permission.CAMERA
                    )
                    != PERMISSION_GRANTED
                )
                    lstPermissions.add(permission.CAMERA)
            }
            if (lstPermissions.size > 0) {
                val permissions = arrayOfNulls<String>(lstPermissions.size)
                for (i in lstPermissions.indices) {
                    permissions[i] = lstPermissions[i]
                }
                requestPermissions(
                    this,
                    permissions,
                    Common.REQUEST_PERMISSION_CALL
                )
            } else {
                return true
            }
        }
        return false
    }

    private fun prepareCall() {
        audioManager = StringeeAudioManager.create(this)
        audioManager?.start { selectedAudioDevice, availableAudioDevices ->
            Log.d(
                Common.TAG,
                "onAudioDeviceChanged: selectedAudioDevice $selectedAudioDevice - availableAudioDevices $availableAudioDevices"
            )
        }

        call = StringeeCall(Common.client, from, to)
        call?.isVideoCall = isVideoCall
        call?.setCallListener(object : StringeeCallListener {
            override fun onSignalingStateChange(
                p0: StringeeCall?,
                p1: SignalingState?,
                p2: String?,
                p3: Int,
                p4: String?
            ) {
                runOnUiThread {
                    mSignalingState = p1!!
                    Log.d(Common.TAG, "onSignalingStateChange: signalingState - $p1")
                    when (mSignalingState) {
                        SignalingState.CALLING -> {
                            binding.tvState.text = "Outgoing call"
                        }
                        SignalingState.RINGING -> {
                            binding.tvState.text = "Ringing"
                        }
                        SignalingState.ANSWERED -> {
                            binding.tvState.text = "Starting"
                            if (mMediaState == MediaState.CONNECTED)
                                binding.tvState.text = "Started"
                        }
                        SignalingState.BUSY -> {
                            binding.tvState.text = "Busy"
                            endCall()
                        }
                        SignalingState.ENDED -> {
                            binding.tvState.text = "Ended"
                            endCall()
                        }
                    }
                }
            }

            override fun onError(p0: StringeeCall?, p1: Int, p2: String?) {
                runOnUiThread {
                    Log.d(Common.TAG, "onError: error - $p2")
                    Common.reportMessage(this@OutgoingCallActivity, p2!!)
                    binding.tvState.text = p2
                    endCall()
                }
            }

            override fun onHandledOnAnotherDevice(
                p0: StringeeCall?,
                p1: SignalingState?,
                p2: String?
            ) {
                runOnUiThread {
                    Log.d(Common.TAG, "onHandledOnAnotherDevice: signalingState - $p1")
                    Common.reportMessage(this@OutgoingCallActivity, p2!!)
                    binding.tvState.text = p2
                    when (p1) {
                        SignalingState.ANSWERED -> {
                            endCall()
                        }
                        SignalingState.BUSY -> {
                            endCall()
                        }
                        SignalingState.ENDED -> {
                            endCall()
                        }
                    }
                }
            }

            override fun onMediaStateChange(p0: StringeeCall?, p1: MediaState?) {
                runOnUiThread {
                    mMediaState = p1!!
                    Log.d(Common.TAG, "onHandledOnAnotherDevice: mediaState - $p1")
                    if (mMediaState == MediaState.CONNECTED) {
                        if (mSignalingState == SignalingState.ANSWERED) {
                            binding.tvState.text = "Started"
                        }
                    }
                }
            }

            override fun onLocalStream(p0: StringeeCall?) {
                runOnUiThread {
                    if (call!!.isVideoCall) {
                        Log.d(Common.TAG, "onLocalStream")
                        binding.vLocal.removeAllViews()
                        val localView: SurfaceView = call!!.localView
                        binding.vLocal.addView(localView)
                        call?.renderLocalView(true)
                    }
                }
            }

            override fun onRemoteStream(p0: StringeeCall?) {
                runOnUiThread {
                    if (call!!.isVideoCall) {
                        Log.d(Common.TAG, "onRemoteStream")
                        binding.vRemote.removeAllViews()
                        val remoteView: SurfaceView = call!!.remoteView
                        binding.vRemote.addView(remoteView)
                        call?.renderRemoteView(false)
                    }
                }
            }

            override fun onCallInfo(p0: StringeeCall?, p1: JSONObject?) {
                TODO("Not yet implemented")
            }
        })

        call?.makeCall()
    }

    private fun endCall() {
        binding.vAnswer.visibility = GONE
        binding.btnEnd.visibility = GONE
        binding.vControl.visibility = GONE
        binding.btnSwitch.visibility = GONE

        call?.hangup()
        audioManager?.stop()

        Common.postDelay({
            Common.isInCall = false
            if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask()
            } else {
                finish()
            }
        }, 1000)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        binding.tvState.text = "Ended"
        endCall()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var isGranted = false
        if (grantResults.isNotEmpty()) {
            for (i in grantResults.indices) {
                if (grantResults[i] != PERMISSION_GRANTED) {
                    isGranted = false
                    break
                } else {
                    isGranted = true
                }
            }
        }
        if (requestCode == Common.REQUEST_PERMISSION_CALL) {
            if (!isGranted) {
                binding.tvState.text = "Permissions not granted"
                endCall()
            } else {
                prepareCall()
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_mute -> {
                isMute = !isMute
                binding.btnMute.setBackgroundResource(if (isMute) btn_mute else btn_mic)
                call?.mute(isMute)
            }
            R.id.btn_speaker -> {
                isSpeaker = !isSpeaker
                binding.btnSpeaker.setBackgroundResource(if (isSpeaker) btn_speaker_on else btn_speaker_off)
                audioManager?.setSpeakerphoneOn(isSpeaker)
            }
            R.id.btn_end -> {
                binding.tvState.text = "Ended"
                endCall()
            }
            R.id.btn_video -> {
                isVideo = !isVideo
                binding.btnVideo.setImageResource(if (isVideo) btn_video else btn_video_off)
                call?.enableVideo(isVideo)
            }
            R.id.btn_switch -> {
                call?.switchCamera(object : StatusListener() {
                    override fun onSuccess() {}
                })
            }
        }
    }
}