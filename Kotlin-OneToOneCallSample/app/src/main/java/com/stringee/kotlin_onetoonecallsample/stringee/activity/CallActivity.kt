package com.stringee.kotlin_onetoonecallsample.stringee.activity

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stringee.common.StringeeAudioManager.AudioDevice
import com.stringee.kotlin_onetoonecallsample.R
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityVideoCallBinding
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityVoiceCallBinding
import com.stringee.kotlin_onetoonecallsample.databinding.LayoutIncomingCallBinding
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallConstants
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallEngine
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallPermissions
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallStatus
import com.stringee.kotlin_onetoonecallsample.stringee.listener.CallUiListener
import com.stringee.kotlin_onetoonecallsample.stringee.manager.CallSession
import com.stringee.kotlin_onetoonecallsample.stringee.manager.StringeeCallManager
import com.stringee.kotlin_onetoonecallsample.stringee.service.MediaProjectionCallService
import com.stringee.video.StringeeVideoTrack
import org.webrtc.RendererCommon

/** Permission-aware in-call UI for voice, video, and Call2 screen sharing. */
class CallActivity : AppCompatActivity(), View.OnClickListener, CallUiListener {
    private var videoBinding: ActivityVideoCallBinding? = null
    private var voiceBinding: ActivityVoiceCallBinding? = null
    private var incomingBinding: LayoutIncomingCallBinding? = null
    private var session: CallSession? = null
    private var videoCall = false
    private var permissionFlowCompleted = false
    private var answerRequested = false
    private var mediaProjectionLauncher: ActivityResultLauncher<Intent>? = null
    private val screenTracks: MutableList<StringeeVideoTrack?> = ArrayList<StringeeVideoTrack?>()
    private var displayedScreenTrack: StringeeVideoTrack? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager: StringeeCallManager = StringeeCallManager.Companion.getInstance(this)
        session = manager.session
        val requestedGeneration = getIntent().getLongExtra(
            CallConstants.EXTRA_SESSION_GENERATION, -1
        )
        if (session == null || (requestedGeneration >= 0
                    && !manager.ownsSessionGeneration(requestedGeneration))
        ) {
            finish()
            return
        }
        answerRequested = getIntent().getBooleanExtra(CallConstants.EXTRA_ANSWER, false)
        videoCall = session!!.isVideoCall
        videoBinding = ActivityVideoCallBinding.inflate(getLayoutInflater())
        voiceBinding = ActivityVoiceCallBinding.inflate(getLayoutInflater())
        setContentView(if (videoCall) videoBinding!!.getRoot() else voiceBinding!!.getRoot())
        incomingBinding =
            if (videoCall) videoBinding!!.vIncomingCall else voiceBinding!!.vIncomingCall

        configureWindow()
        bindActions()
        configureInitialUi()
        mediaProjectionLauncher = registerForActivityResult(
            StartActivityForResult(), ActivityResultCallback { result: ActivityResult ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    val serviceIntent = Intent(
                        this,
                        MediaProjectionCallService::class.java
                    )
                        .setAction(CallConstants.ACTION_START_IN_CALL)
                        .putExtra(
                            MediaProjectionCallService.Companion.EXTRA_PERMISSION_DATA,
                            result.data
                        )
                    ContextCompat.startForegroundService(this, serviceIntent)
                }
            })
        session!!.registerUiListener(this)
        ensureCallPermissions()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val requestedGeneration = intent.getLongExtra(
            CallConstants.EXTRA_SESSION_GENERATION, -1
        )
        if (requestedGeneration >= 0 && !StringeeCallManager.Companion.getInstance(this)
                .ownsSessionGeneration(requestedGeneration)
        ) {
            return
        }
        if (intent.getBooleanExtra(CallConstants.EXTRA_ANSWER, false)) {
            answerRequested = true
            startCallAfterPermissions()
        }
    }

    private fun configureWindow() {
        getWindow().addFlags(
            (WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        })
    }

    private fun bindActions() {
        incomingBinding!!.btnAnswer.setOnClickListener(this)
        incomingBinding!!.btnReject.setOnClickListener(this)
        if (videoCall) {
            videoBinding!!.btnEnd.setOnClickListener(this)
            videoBinding!!.btnMute.setOnClickListener(this)
            videoBinding!!.btnCamera.setOnClickListener(this)
            videoBinding!!.btnSwitch.setOnClickListener(this)
            videoBinding!!.btnShare.setOnClickListener(this)
        } else {
            voiceBinding!!.btnEnd.setOnClickListener(this)
            voiceBinding!!.btnMute.setOnClickListener(this)
            voiceBinding!!.btnSpeaker.setOnClickListener(this)
        }
    }

    private fun configureInitialUi() {
        val incoming = session!!.status == CallStatus.INCOMING
        incomingBinding!!.getRoot().setVisibility(if (incoming) View.VISIBLE else View.GONE)
        if (videoCall) {
            videoBinding!!.vInCall.setVisibility(if (incoming) View.GONE else View.VISIBLE)
            videoBinding!!.vShareBtn.setVisibility(
                if (session!!.engine == CallEngine.STRINGEE_CALL2) View.VISIBLE else View.GONE
            )
        } else {
            voiceBinding!!.vInCall.setVisibility(if (incoming) View.GONE else View.VISIBLE)
            voiceBinding!!.tvUser1.text = session!!.from
        }
        incomingBinding!!.tvUser.text = session!!.from
    }

    private fun ensureCallPermissions() {
        if (CallPermissions.hasCallPermissions(this, videoCall)) {
            permissionFlowCompleted = true
            startCallAfterPermissions()
        } else {
            CallPermissions.requestCallPermissions(this, videoCall)
        }
    }

    private fun startCallAfterPermissions() {
        if (!permissionFlowCompleted || session == null) {
            return
        }
        if (session!!.isIncoming) {
            if (answerRequested) {
                answerRequested = false
                session!!.answer()
            }
        } else {
            StringeeCallManager.Companion.getInstance(this).startPreparedOutgoing()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != CallPermissions.REQUEST_CALL_PERMISSIONS) {
            return
        }
        permissionFlowCompleted = CallPermissions.verify(grantResults)
        if (permissionFlowCompleted) {
            startCallAfterPermissions()
        } else if (session != null) {
            if (session!!.isIncoming) {
                session!!.reject()
            } else {
                session!!.release()
            }
            finishCallActivity()
        }
    }

    override fun onClick(view: View) {
        if (session == null) {
            return
        }
        val id = view.getId()
        if (id == R.id.btn_answer) {
            answerRequested = true
            startCallAfterPermissions()
        } else if (id == R.id.btn_reject) {
            session!!.reject()
        } else if (id == R.id.btn_end) {
            session!!.hangUp()
        } else if (id == R.id.btn_mute) {
            session!!.toggleMute()
        } else if (id == R.id.btn_speaker) {
            session!!.toggleSpeaker()
        } else if (id == R.id.btn_camera) {
            session!!.toggleVideo()
        } else if (id == R.id.btn_switch) {
            session!!.switchCamera()
        } else if (id == R.id.btn_share) {
            if (session!!.isSharing) {
                session!!.stopScreenShare()
            } else {
                session!!.prepareScreenShare(
                    this, requireNotNull(mediaProjectionLauncher),
                    getSystemService(MediaProjectionManager::class.java)
                )
            }
        }
    }

    override fun onCallStatus(status: CallStatus) {
        runOnUiThread(Runnable {
            if (!videoCall) {
                voiceBinding!!.tvStatus.setText(status.value)
            }
            val incoming = status == CallStatus.INCOMING
            incomingBinding!!.getRoot().setVisibility(if (incoming) View.VISIBLE else View.GONE)
            if (videoCall) {
                videoBinding!!.vInCall.setVisibility(if (incoming) View.GONE else View.VISIBLE)
            } else {
                voiceBinding!!.vInCall.setVisibility(if (incoming) View.GONE else View.VISIBLE)
            }
            if (status == CallStatus.ENDED || status == CallStatus.BUSY) {
                finishCallActivity()
            }
        })
    }

    override fun onError(message: String?) {
        runOnUiThread(Runnable { this.finishCallActivity() })
    }

    override fun onLocalVideoAvailable() {
        runOnUiThread(Runnable {
            renderView(
                videoBinding!!.vLocal,
                session!!.localView,
                true
            )
        })
    }

    override fun onRemoteVideoAvailable() {
        runOnUiThread(Runnable {
            if (displayedScreenTrack == null) {
                renderView(videoBinding!!.vRemote, session!!.remoteView, false)
            }
        })
    }

    private fun renderView(parent: FrameLayout, child: View?, local: Boolean) {
        if (!videoCall || child == null || session == null) {
            return
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        parent.removeAllViews()
        parent.addView(child, params)
        if (local) {
            session!!.renderLocalView()
        } else {
            session!!.renderRemoteView()
        }
    }

    override fun onAudioDeviceChanged(
        selected: AudioDevice?,
        available: MutableSet<AudioDevice?>?
    ) {
        if (videoCall) {
            return
        }
        runOnUiThread(Runnable {
            val speaker = selected == AudioDevice.SPEAKER_PHONE
            voiceBinding!!.btnSpeaker.setBackgroundResource(
                if (speaker)
                    R.drawable.btn_ic_selector
                else
                    R.drawable.btn_ic_selected_selector
            )
            voiceBinding!!.btnSpeaker.setImageResource(
                if (speaker)
                    R.drawable.ic_speaker_on
                else
                    R.drawable.ic_speaker_off
            )
        })
    }

    override fun onMicChanged(enabled: Boolean) {
        runOnUiThread(Runnable {
            if (videoCall) {
                videoBinding!!.btnMute.setBackgroundResource(
                    if (enabled)
                        R.drawable.btn_ic_selected_selector
                    else
                        R.drawable.btn_ic_selector
                )
                videoBinding!!.btnMute.setImageResource(
                    if (enabled)
                        R.drawable.ic_mic_on
                    else
                        R.drawable.ic_mic_off
                )
            } else {
                voiceBinding!!.btnMute.setBackgroundResource(
                    if (enabled)
                        R.drawable.btn_ic_selected_selector
                    else
                        R.drawable.btn_ic_selector
                )
                voiceBinding!!.btnMute.setImageResource(
                    if (enabled)
                        R.drawable.ic_mic_on
                    else
                        R.drawable.ic_mic_off
                )
            }
        })
    }

    override fun onVideoChanged(enabled: Boolean) {
        if (videoCall) {
            runOnUiThread(Runnable {
                videoBinding!!.btnCamera.setBackgroundResource(
                    if (enabled)
                        R.drawable.btn_ic_selected_selector
                    else
                        R.drawable.btn_ic_selector
                )
                videoBinding!!.btnCamera.setImageResource(
                    if (enabled)
                        R.drawable.ic_cam_on
                    else
                        R.drawable.ic_cam_off
                )
            })
        }
    }

    override fun onSharingChanged(sharing: Boolean) {
        if (videoCall) {
            runOnUiThread(Runnable {
                videoBinding!!.btnShare.setBackgroundResource(
                    if (sharing)
                        R.drawable.btn_ic_selector
                    else
                        R.drawable.btn_ic_selected_selector
                )
                videoBinding!!.btnShare.setImageResource(
                    if (sharing)
                        R.drawable.ic_share_off
                    else
                        R.drawable.ic_share
                )
            })
        }
    }

    override fun onTimer(duration: String?) {
        runOnUiThread(Runnable {
            if (videoCall) {
                videoBinding!!.tvTime.setText(duration)
            } else {
                voiceBinding!!.tvTime.setText(duration)
            }
        })
    }

    override fun onScreenTrackAdded(track: StringeeVideoTrack?) {
        runOnUiThread(Runnable {
            if (!screenTracks.contains(track)) {
                screenTracks.add(track)
            }
            displayScreenTrack(track)
        })
    }

    override fun onScreenTrackRemoved(track: StringeeVideoTrack) {
        runOnUiThread(Runnable {
            screenTracks.removeIf { candidate: StringeeVideoTrack? -> candidate!!.getId() == track.getId() }
            displayedScreenTrack = if (screenTracks.isEmpty())
                null
            else
                screenTracks.get(screenTracks.size - 1)
            if (displayedScreenTrack == null) {
                renderView(videoBinding!!.vRemote, session!!.remoteView, false)
            } else {
                displayScreenTrack(displayedScreenTrack)
            }
        })
    }

    private fun displayScreenTrack(track: StringeeVideoTrack?) {
        if (!videoCall || track == null) {
            return
        }
        displayedScreenTrack = track
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.CENTER
        videoBinding!!.vRemote.removeAllViews()
        videoBinding!!.vRemote.addView(track.getView2(this), params)
        track.renderView2(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
    }

    private fun finishCallActivity() {
        clearVideoViews()
        if (session != null) {
            session!!.unregisterUiListener(this)
        }
        getWindow().clearFlags(
            (WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        )
        finish()
    }

    override fun onDestroy() {
        clearVideoViews()
        if (session != null) {
            session!!.unregisterUiListener(this)
        }
        super.onDestroy()
    }

    private fun clearVideoViews() {
        if (!videoCall || videoBinding == null) {
            return
        }
        videoBinding!!.vLocal.removeAllViews()
        videoBinding!!.vRemote.removeAllViews()
        screenTracks.clear()
        displayedScreenTrack = null
    }
}
