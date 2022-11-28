package com.stringee.kotlin_onetoonecallsample

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import com.stringee.StringeeClient
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall2
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.R.id.*
import com.stringee.kotlin_onetoonecallsample.R.string
import com.stringee.kotlin_onetoonecallsample.common.*
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityMainBinding
import com.stringee.listener.StatusListener
import com.stringee.listener.StringeeConnectionListener
import org.json.JSONObject


class MainActivity : BaseActivity() {
    //put your token here
    private var launcher: ActivityResultLauncher<Intent>? = null
    private lateinit var binding: ActivityMainBinding
    private val token = "PUT_YOUR_TOKEN_HERE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnVoiceCall.setOnClickListener(this)
        binding.btnVideoCall.setOnClickListener(this)
        binding.btnVoiceCall2.setOnClickListener(this)
        binding.btnVideoCall2.setOnClickListener(this)

        // register data call back
        launcher = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_CANCELED) if (result.data != null) {
                if (result.data!!.action != null && result.data!!.action
                        .equals("open_app_setting")
                ) {
                    val builder =
                        AlertDialog.Builder(this)
                    builder.setTitle(string.app_name)
                    builder.setMessage("Permissions must be granted for the call")
                    builder.setPositiveButton(
                        "Ok"
                    ) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
                    builder.setNegativeButton(
                        "Settings"
                    ) { dialogInterface: DialogInterface, _: Int ->
                        dialogInterface.cancel()
                        // open app setting
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        val uri =
                            Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    builder.create().show()
                }
            }
        }
        initAndConnectStringee()
    }

    private fun initAndConnectStringee() {
        if (Common.client == null) {
            Common.client = StringeeClient(this)
            // Set host
//            val socketAddressList: MutableList<SocketAddress> = ArrayList()
//            socketAddressList.add( SocketAddress ("YOUR_IP", YOUR_PORT))
//            Common.client!!.setHost(socketAddressList)

            Common.client!!.setConnectionListener(object : StringeeConnectionListener {
                override fun onConnectionConnected(
                    stringeeClient: StringeeClient,
                    isReconnecting: Boolean
                ) {
                    runOnUiThread {
                        Log.d(Constant.TAG, "onConnectionConnected")
                        binding.tvUserId.text = "Connected as: " + stringeeClient.userId
                        Utils.reportMessage(
                            this@MainActivity,
                            "StringeeClient connected as " + stringeeClient.userId
                        )
                    }
                }

                override fun onConnectionDisconnected(
                    stringeeClient: StringeeClient,
                    isReconnecting: Boolean
                ) {
                    runOnUiThread {
                        Log.d(Constant.TAG, "onConnectionDisconnected")
                        binding.tvUserId.text = "Disconnected"
                        Utils.reportMessage(this@MainActivity, "StringeeClient disconnected.")
                    }
                }

                override fun onIncomingCall(stringeeCall: StringeeCall) {
                    runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onIncomingCall: callId - " + stringeeCall.callId
                        )
                        if (Common.isInCall) {
                            stringeeCall.reject(object : StatusListener() {
                                override fun onSuccess() {}
                            })
                        } else {
                            RingtoneUtils.getInstance(this@MainActivity)
                                ?.startRingtoneAndVibration()
                            Common.callsMap[stringeeCall.callId] = stringeeCall
                            val intent =
                                Intent(this@MainActivity, IncomingCallActivity::class.java)
                            intent.putExtra("call_id", stringeeCall.callId)
                            startActivity(intent)
                        }
                    }
                }

                override fun onIncomingCall2(stringeeCall2: StringeeCall2) {
                    runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onIncomingCall2: callId - " + stringeeCall2.callId
                        )
                        if (Common.isInCall) {
                            stringeeCall2.reject(object : StatusListener() {
                                override fun onSuccess() {}
                            })
                        } else {
                            RingtoneUtils.getInstance(this@MainActivity)
                                ?.startRingtoneAndVibration()
                            Common.calls2Map[stringeeCall2.callId] = stringeeCall2
                            val intent =
                                Intent(this@MainActivity, IncomingCall2Activity::class.java)
                            intent.putExtra("call_id", stringeeCall2.callId)
                            startActivity(intent)
                        }
                    }
                }

                override fun onConnectionError(
                    stringeeClient: StringeeClient,
                    stringeeError: StringeeError
                ) {
                    runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onConnectionError: " + stringeeError.message
                        )
                        binding.tvUserId.text = "Connect error: " + stringeeError.message
                        Utils.reportMessage(
                            this@MainActivity,
                            "StringeeClient fails to connect: " + stringeeError.message
                        )
                    }
                }

                override fun onRequestNewToken(stringeeClient: StringeeClient) {
                    runOnUiThread {
                        Log.d(Constant.TAG, "onRequestNewToken")
                        binding.tvUserId.text = "Request new token"
                    }

                    // Get new token here and connect to Stringee server
                }

                override fun onCustomMessage(from: String, msg: JSONObject) {
                    runOnUiThread {
                        Log.d(
                            Constant.TAG,
                            "onCustomMessage: from - $from - msg - $msg"
                        )
                    }
                }

                override fun onTopicMessage(from: String, msg: JSONObject) {}
            })
        }
        Common.client!!.connect(token)
    }

    override fun onClick(view: View) {
        when (view.id) {
            btn_voice_call -> {
                makeCall(isStringeeCall = true, isVideoCall = false)
            }
            btn_video_call -> {
                makeCall(isStringeeCall = true, isVideoCall = true)
            }
            btn_voice_call2 -> {
                makeCall(isStringeeCall = false, isVideoCall = false)
            }
            btn_video_call2 -> {
                makeCall(isStringeeCall = false, isVideoCall = true)
            }
        }
    }

    private fun makeCall(isStringeeCall: Boolean, isVideoCall: Boolean) {
        val to = binding.etTo.text.toString()
        if (to.trim { it <= ' ' }.isNotEmpty()) {
            if (Common.client!!.isConnected) {
                val intent: Intent
                if (isStringeeCall) {
                    intent = Intent(this, OutgoingCallActivity::class.java)
                } else {
                    intent = Intent(this, OutgoingCall2Activity::class.java)
                }
                intent.putExtra("from", Common.client!!.userId)
                intent.putExtra("to", to)
                intent.putExtra("is_video_call", isVideoCall)
                launcher!!.launch(intent)
            } else {
                Utils.reportMessage(this, "Stringee session not connected")
            }
        }
    }
}
