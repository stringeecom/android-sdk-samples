package com.stringee.kotlin_onetoonecallsample.activity

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.stringee.kotlin_onetoonecallsample.R
import com.stringee.kotlin_onetoonecallsample.common.CallStatus
import com.stringee.kotlin_onetoonecallsample.common.Constant
import com.stringee.kotlin_onetoonecallsample.common.NotificationUtils
import com.stringee.kotlin_onetoonecallsample.common.SensorManagerUtils
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityVideoCallBinding
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityVoiceCallBinding
import com.stringee.kotlin_onetoonecallsample.databinding.LayoutIncomingCallBinding
import com.stringee.kotlin_onetoonecallsample.listener.OnCallListener
import com.stringee.kotlin_onetoonecallsample.manager.CallManager
import com.stringee.kotlin_onetoonecallsample.service.MyMediaProjectionService
import com.stringee.video.StringeeVideoTrack
import org.webrtc.RendererCommon

class CallActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var videoCallBinding: ActivityVideoCallBinding
    private lateinit var voiceCallBinding: ActivityVoiceCallBinding
    private lateinit var incomingCallBinding: LayoutIncomingCallBinding
    private lateinit var callManager: CallManager
    private var sensorManagerUtils: SensorManagerUtils? = null
    private val remoteShareTrackList: MutableList<StringeeVideoTrack> =
        ArrayList<StringeeVideoTrack>()
    private var remoteShareTrack: StringeeVideoTrack? = null
    private var isVideoCall = false
    private var isIncomingCall = false
    private var isStringeeCall = false
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        videoCallBinding = ActivityVideoCallBinding.inflate(layoutInflater)
        voiceCallBinding = ActivityVoiceCallBinding.inflate(layoutInflater)

        isVideoCall = intent.getBooleanExtra(Constant.PARAM_IS_VIDEO_CALL, false)
        setContentView(if (isVideoCall) videoCallBinding.root else voiceCallBinding.root)
        incomingCallBinding =
            if (isVideoCall) videoCallBinding.vIncomingCall else voiceCallBinding.vIncomingCall

        NotificationUtils.getInstance(this).cancelNotification(Constant.INCOMING_CALL_ID)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })

        isIncomingCall = intent.getBooleanExtra(Constant.PARAM_IS_INCOMING_CALL, false)
        isStringeeCall = intent.getBooleanExtra(Constant.PARAM_IS_STRINGEE_CALL, false)

        callManager = CallManager.getInstance(this)

        activityResultLauncher = registerForActivityResult<Intent?, ActivityResult?>(
            StartActivityForResult()
        ) { result: ActivityResult? ->
            if (result!!.resultCode == RESULT_OK && result.data != null) {
                val intent = Intent(this, MyMediaProjectionService::class.java)
                intent.action = Constant.ACTION_START_FOREGROUND_SERVICE
                intent.putExtras(result.data!!)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
        sensorManagerUtils = SensorManagerUtils.getInstance(this).initialize(localClassName)

        if (!isVideoCall) {
            sensorManagerUtils?.turnOn()
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
            videoCallBinding.btnShare.setOnClickListener(this)
        }

        incomingCallBinding.getRoot().visibility =
            if (callManager.getCallStatus() !== CallStatus.INCOMING) View.GONE else View.VISIBLE
        if (isVideoCall) {
            videoCallBinding.vInCall.visibility =
                if (callManager.getCallStatus() !== CallStatus.INCOMING) View.VISIBLE else View.GONE
        } else {
            voiceCallBinding.vInCall.visibility =
                if (callManager.getCallStatus() !== CallStatus.INCOMING) View.VISIBLE else View.GONE
        }

        initCall()
        if (isVideoCall) {
            videoCallBinding.vShareBtn.visibility = if (isStringeeCall) View.GONE else View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        runOnUiThread {
            if (callManager.getCallStatus() === CallStatus.STARTED || callManager.getCallStatus() === CallStatus.CALLING || callManager.getCallStatus() === CallStatus.RINGING) {
                window.clearFlags(
                    (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                )

                sensorManagerUtils = SensorManagerUtils.getInstance(this).initialize(localClassName)
                if (!isVideoCall) {
                    sensorManagerUtils?.turnOff()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(false)
                    setTurnScreenOn(false)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        runOnUiThread {
            if (callManager.getCallStatus() === CallStatus.STARTED || callManager.getCallStatus() === CallStatus.CALLING || callManager.getCallStatus() === CallStatus.RINGING) {
                window.addFlags(
                    (WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
                )

                sensorManagerUtils = SensorManagerUtils.getInstance(this).initialize(localClassName)
                if (!isVideoCall) {
                    sensorManagerUtils?.turnOn()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(true)
                    setTurnScreenOn(true)
                }
            }
        }
    }

    private fun initCall() {
        callManager.registerEvent(object : OnCallListener {
            override fun onCallStatus(status: CallStatus) {
                runOnUiThread {
                    if (!isVideoCall) {
                        voiceCallBinding.tvStatus.text = status.value
                    }
                    incomingCallBinding.getRoot().visibility =
                        if (status !== CallStatus.INCOMING) View.GONE else View.VISIBLE
                    if (isVideoCall) {
                        videoCallBinding.vInCall.visibility =
                            if (status !== CallStatus.INCOMING) View.VISIBLE else View.GONE
                    } else {
                        voiceCallBinding.vInCall.visibility =
                            if (status !== CallStatus.INCOMING) View.VISIBLE else View.GONE
                    }
                    if (status === CallStatus.ENDED || status === CallStatus.BUSY) {
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
                        videoCallBinding.vLocal.addView(callManager.localView, childParams)
                        callManager.renderLocalView()
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
                        videoCallBinding.vRemote.addView(callManager.remoteView, childParams)
                        callManager.renderRemoteView()
                    }
                }
            }

            override fun onSpeakerChange(isOn: Boolean) {
                runOnUiThread {
                    voiceCallBinding.btnSpeaker.setBackgroundResource(if (isOn) R.drawable.btn_ic_selector else R.drawable.btn_ic_selected_selector)
                    voiceCallBinding.btnSpeaker.setImageResource(if (isOn) R.drawable.ic_speaker_on else R.drawable.ic_speaker_off)
                }
            }

            override fun onMicChange(isOn: Boolean) {
                runOnUiThread {
                    if (isVideoCall) {
                        videoCallBinding.btnMute.setBackgroundResource(if (!isOn) R.drawable.btn_ic_selector else R.drawable.btn_ic_selected_selector)
                        videoCallBinding.btnMute.setImageResource(if (!isOn) R.drawable.ic_mic_off else R.drawable.ic_mic_on)
                    } else {
                        voiceCallBinding.btnMute.setBackgroundResource(if (!isOn) R.drawable.btn_ic_selector else R.drawable.btn_ic_selected_selector)
                        voiceCallBinding.btnMute.setImageResource(if (!isOn) R.drawable.ic_mic_off else R.drawable.ic_mic_on)
                    }
                }
            }

            override fun onVideoChange(isOn: Boolean) {
                runOnUiThread {
                    videoCallBinding.btnCamera.setBackgroundResource(if (isOn) R.drawable.btn_ic_selected_selector else R.drawable.btn_ic_selector)
                    videoCallBinding.btnCamera.setImageResource(if (isOn) R.drawable.ic_cam_on else R.drawable.ic_cam_off)
                }
            }

            override fun onSharing(isSharing: Boolean) {
                runOnUiThread {
                    videoCallBinding.btnShare.setBackgroundResource(if (isSharing) R.drawable.btn_ic_selector else R.drawable.btn_ic_selected_selector)
                    videoCallBinding.btnShare.setImageResource(if (isSharing) R.drawable.ic_share_off else R.drawable.ic_share)
                }
            }

            override fun onTimer(duration: String) {
                runOnUiThread {
                    if (!isVideoCall) {
                        voiceCallBinding.tvTime.text = duration
                    } else {
                        videoCallBinding.tvTime.text = duration
                    }
                }
            }

            override fun onVideoTrackAdded(stringeeVideoTrack: StringeeVideoTrack) {
                runOnUiThread {
                    if (!isStringeeCall && isVideoCall) {
                        if (!stringeeVideoTrack.isLocal) {
                            if (remoteShareTrack == null) {
                                remoteShareTrack = stringeeVideoTrack
                                val childParams = FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT
                                )
                                childParams.gravity = Gravity.CENTER

                                videoCallBinding.vRemote.removeAllViews()
                                videoCallBinding.vRemote.addView(
                                    remoteShareTrack!!.getView2(this@CallActivity), childParams
                                )
                                remoteShareTrack!!.renderView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                            }
                            remoteShareTrackList.add(stringeeVideoTrack)
                        }
                    }
                }
            }

            override fun onVideoTrackRemoved(stringeeVideoTrack: StringeeVideoTrack) {
                runOnUiThread {
                    if (!isStringeeCall && isVideoCall) {
                        if (!stringeeVideoTrack.isLocal) {
                            val childParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.WRAP_CONTENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                            )
                            childParams.gravity = Gravity.CENTER
                            for (i in remoteShareTrackList.indices) {
                                val videoTrack = remoteShareTrackList[i]
                                if (videoTrack.id == stringeeVideoTrack.id || videoTrack.localId == stringeeVideoTrack.localId) {
                                    remoteShareTrackList.removeAt(i)
                                    break
                                }
                            }
                            if (remoteShareTrack != null) {
                                if (remoteShareTrackList.isEmpty()) {
                                    remoteShareTrack = null
                                    videoCallBinding.vRemote.removeAllViews()
                                    videoCallBinding.vRemote.addView(
                                        callManager.remoteView, childParams
                                    )
                                    callManager.renderRemoteView()
                                } else {
                                    remoteShareTrack = remoteShareTrackList[0]
                                    videoCallBinding.vRemote.removeAllViews()
                                    videoCallBinding.vRemote.addView(
                                        remoteShareTrack!!.getView2(
                                            this@CallActivity
                                        ), childParams
                                    )
                                    remoteShareTrack!!.renderView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                                }
                            } else {
                                videoCallBinding.vRemote.removeAllViews()
                                videoCallBinding.vRemote.addView(
                                    callManager.remoteView, childParams
                                )
                                callManager.renderRemoteView()
                            }
                        }
                    }
                }
            }
        })
        if (!isIncomingCall) {
            val to = intent.getStringExtra(Constant.PARAM_TO)
            if (!isVideoCall) {
                voiceCallBinding.tvUser1.text = to
            }
            callManager.initializedOutgoingCall(to, isVideoCall, isStringeeCall)
            callManager.makeCall()
        } else {
            incomingCallBinding.tvUser.text = callManager.from
            if (!isVideoCall) {
                voiceCallBinding.tvUser1.text = callManager.from
            }
            val isAnswerFromPush =
                intent.getBooleanExtra(Constant.PARAM_ACTION_ANSWER_FROM_PUSH, false)
            if (isAnswerFromPush) {
                callManager.answer()
            }
        }
    }

    override fun onClick(view: View) {
        val vId = view.id
        if (vId == R.id.btn_answer) {
            callManager.answer()
        } else if (vId == R.id.btn_reject) {
            callManager.endCall(false)
        } else if (vId == R.id.btn_end) {
            callManager.endCall(true)
        } else if (vId == R.id.btn_mute) {
            callManager.mute()
        } else if (vId == R.id.btn_speaker) {
            callManager.changeSpeaker()
        } else if (vId == R.id.btn_camera) {
            callManager.enableVideo()
        } else if (vId == R.id.btn_switch) {
            callManager.switchCamera()
        } else if (vId == R.id.btn_share) {
            if (callManager.isSharing) {
                callManager.stopSharing()
            } else {
                callManager.prepareShareScreen(
                    this,
                    activityResultLauncher,
                    getSystemService(MediaProjectionManager::class.java)
                )
            }
        }
    }

    private fun dismiss() {
        sensorManagerUtils?.releaseSensor()
        callManager.release()

        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)

        finish()
    }
}
