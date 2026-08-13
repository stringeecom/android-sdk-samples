package com.stringee.kotlin_onetoonecallsample.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.R
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityMainBinding
import com.stringee.kotlin_onetoonecallsample.stringee.StringeeCallManager
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallEngine
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallStatus
import com.stringee.kotlin_onetoonecallsample.stringee.common.ConnectionState
import com.stringee.kotlin_onetoonecallsample.stringee.common.StringeeCallConfig
import com.stringee.kotlin_onetoonecallsample.stringee.listener.StringeeCallListener
import com.stringee.listener.StatusListener

/** Host screen that connects a token and delegates outgoing calls to the Stringee facade. */
class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var callManager: StringeeCallManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        callManager = StringeeCallManager.getInstance(this)
        binding.etToken.setText(callManager.savedToken)
        bindActions()
        renderConnection(
            if (callManager.isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED,
            callManager.connectedUserId
        )
        callManager.initialize(object : StringeeCallListener {
            override fun onConnectionStateChanged(state: ConnectionState, userId: String) {
                runOnUiThread { renderConnection(state, userId) }
            }

            override fun onCallStateChanged(state: CallStatus) {
                runOnUiThread {
                    binding.tvCallStatus.text = getString(R.string.call_state, state.value)
                }
            }

            override fun onError(action: String, error: StringeeError) {
                runOnUiThread {
                    val message = "$action: ${error.message}"
                    binding.tvLastError.text = getString(R.string.last_error, message)
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                }
            }

            override fun onRequestNewToken() {
                runOnUiThread { binding.tvLastError.setText(R.string.token_refresh_required) }
            }
        })
        callManager.handleLaunchIntent(intent)
    }

    private fun bindActions() = with(binding) {
        btnConnect.setOnClickListener(this@MainActivity)
        btnDisconnect.setOnClickListener(this@MainActivity)
        btnNotificationPermission.setOnClickListener(this@MainActivity)
        btnFullScreenPermission.setOnClickListener(this@MainActivity)
        btnAppSettings.setOnClickListener(this@MainActivity)
        btnVoiceCall.setOnClickListener(this@MainActivity)
        btnVideoCall.setOnClickListener(this@MainActivity)
        btnVoiceCall2.setOnClickListener(this@MainActivity)
        btnVideoCall2.setOnClickListener(this@MainActivity)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        callManager.handleLaunchIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        renderNotificationAccess()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_connect -> callManager.connect(binding.etToken.text.toString())
            R.id.btn_disconnect -> callManager.disconnect()
            R.id.btn_notification_permission -> callManager.requestNotificationPermission(this)
            R.id.btn_full_screen_permission -> callManager.openFullScreenIntentSettings(this)
            R.id.btn_app_settings -> callManager.openAppSettings(this)
            R.id.btn_voice_call -> makeCall(CallEngine.STRINGEE_CALL, false)
            R.id.btn_video_call -> makeCall(CallEngine.STRINGEE_CALL, true)
            R.id.btn_voice_call2 -> makeCall(CallEngine.STRINGEE_CALL2, false)
            R.id.btn_video_call2 -> makeCall(CallEngine.STRINGEE_CALL2, true)
        }
    }

    private fun makeCall(engine: CallEngine, video: Boolean) {
        callManager.makeCall(
            StringeeCallConfig(binding.etTo.text.toString(), engine, video),
            object : StatusListener() {
                override fun onSuccess() {
                    runOnUiThread { binding.tvLastError.text = "" }
                }
            }
        )
    }

    private fun renderConnection(state: ConnectionState, userId: String?) = with(binding) {
        val connected = state == ConnectionState.CONNECTED
        val suffix = if (connected && !userId.isNullOrEmpty()) " ($userId)" else ""
        tvConnectionStatus.text = getString(R.string.connection_state, state.name + suffix)
        btnConnect.isEnabled = !connected && state != ConnectionState.CONNECTING
        btnDisconnect.isEnabled = connected || state == ConnectionState.CONNECTING
        btnDisconnect.visibility =
            if (connected || state == ConnectionState.CONNECTING) View.VISIBLE else View.GONE
        layoutConnectionCard.visibility = if (connected) View.GONE else View.VISIBLE
        layoutMakeCall.visibility = if (connected) View.VISIBLE else View.GONE
        val (statusBackground, statusColor) = when (state) {
            ConnectionState.CONNECTED ->
                R.drawable.bg_stringee_status_success to R.color.stringee_green_dark
            ConnectionState.ERROR ->
                R.drawable.bg_stringee_status_error to R.color.stringee_error
            else -> R.drawable.bg_stringee_status_info to R.color.stringee_blue
        }
        layoutConnectionStatus.setBackgroundResource(statusBackground)
        tvConnectionStatus.setTextColor(ContextCompat.getColor(this@MainActivity, statusColor))
    }

    private fun renderNotificationAccess() = with(binding) {
        val notification = callManager.canPostNotifications()
        val fullScreen = callManager.canUseFullScreenIntent()
        layoutIncomingCallAccess.visibility =
            if (!notification || !fullScreen) View.VISIBLE else View.GONE
        tvNotificationStatus.text = getString(
            R.string.notification_access_state,
            getString(if (notification) R.string.granted else R.string.not_granted),
            getString(if (fullScreen) R.string.granted else R.string.not_granted)
        )
        btnNotificationPermission.visibility = if (notification) View.GONE else View.VISIBLE
        btnFullScreenPermission.visibility = if (fullScreen) View.GONE else View.VISIBLE
    }
}
