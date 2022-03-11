package com.stringee.kotlin_onetoonecallsample

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.stringee.StringeeClient
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall2
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.R.id.*
import com.stringee.kotlin_onetoonecallsample.R.string
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityMainBinding
import com.stringee.listener.StringeeConnectionListener
import org.json.JSONObject

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private var token = "PUT_YOUR_TOKEN_HERE"
    private lateinit var launcher: ActivityResultLauncher<Intent>
    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVideoCall.setOnClickListener(this)
        binding.btnVideoCall2.setOnClickListener(this)
        binding.btnVoiceCall.setOnClickListener(this)
        binding.btnVoiceCall2.setOnClickListener(this)

        progressDialog = ProgressDialog.show(this, "", "Connecting...")
        progressDialog?.setCancelable(true)
        progressDialog?.show()

        // register data call back
        launcher = registerForActivityResult(
            StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_CANCELED) if (result.data != null) {
                if (result.data!!.action != null && result.data!!
                        .action == "open_app_setting"
                ) {
                    val builder = AlertDialog.Builder(this)
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
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                    builder.create().show()
                }
            }
        }

        initStringee()
    }

    private fun initStringee() {
        Common.client = StringeeClient(this)
        Common.client.setConnectionListener(object : StringeeConnectionListener {
            override fun onConnectionConnected(
                stringeeClient: StringeeClient,
                isReconnecting: Boolean
            ) {
                runOnUiThread {
                    Log.d(Common.TAG, "onConnectionConnected")
                    progressDialog!!.dismiss()
                    binding.tvUserid.text = "Connected as: ${stringeeClient.userId}"
                    Common.reportMessage(this@MainActivity, "StringeeClient is connected.")
                }
            }

            override fun onConnectionDisconnected(
                stringeeClient: StringeeClient,
                isReconnecting: Boolean
            ) {
                runOnUiThread {
                    Log.d(Common.TAG, "onConnectionDisconnected")
                    progressDialog!!.dismiss()
                    binding.tvUserid.text = "Disconnected"
                    Common.reportMessage(this@MainActivity, "StringeeClient is disconnected.")
                }
            }

            override fun onIncomingCall(stringeeCall: StringeeCall) {
                runOnUiThread {
                    Log.d(Common.TAG, "onIncomingCall: callId - ${stringeeCall.callId}")
                    if (Common.isInCall) stringeeCall.reject() else {
                        Common.callsMap[stringeeCall.callId] = stringeeCall
                        val intent = Intent(
                            this@MainActivity,
                            IncomingCallActivity::class.java
                        ).apply { putExtra("call_id", stringeeCall.callId) }
                        startActivity(intent)
                    }
                }
            }

            override fun onIncomingCall2(stringeeCall2: StringeeCall2) {
                runOnUiThread {
                    Log.d(Common.TAG, "onIncomingCall2: callId - ${stringeeCall2.callId}")
                    if (Common.isInCall) stringeeCall2.reject() else {
                        Common.call2sMap[stringeeCall2.callId] = stringeeCall2
                        val intent = Intent(
                            this@MainActivity,
                            IncomingCall2Activity::class.java
                        ).apply { putExtra("call_id", stringeeCall2.callId) }
                        startActivity(intent)
                    }
                }
            }

            override fun onConnectionError(
                stringeeClient: StringeeClient,
                stringeeError: StringeeError
            ) {
                runOnUiThread {
                    Log.d(Common.TAG, "onConnectionError: ${stringeeError.message}")
                    progressDialog!!.dismiss()
                    Common.reportMessage(
                        this@MainActivity,
                        "StringeeClient fails to connect: ${stringeeError.message}"
                    )
                }
            }

            override fun onRequestNewToken(stringeeClient: StringeeClient) {
                runOnUiThread {
                    Log.d(
                        Common.TAG,
                        "onRequestNewToken"
                    )
                }
                // Get new token here and connect to Stringe server
            }

            override fun onCustomMessage(from: String, msg: JSONObject) {
                runOnUiThread {
                    Log.d(
                        Common.TAG,
                        "onCustomMessage: from - $from - msg - $msg"
                    )
                }
            }

            override fun onTopicMessage(from: String, msg: JSONObject) {
            }
        })
        Common.client.connect(token)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            btn_video_call -> makeCall(isStringeeCall = true, isVideoCall = true)
            btn_video_call2 -> makeCall(isStringeeCall = false, isVideoCall = true)
            btn_voice_call -> makeCall(isStringeeCall = true, isVideoCall = false)
            btn_voice_call2 -> makeCall(isStringeeCall = false, isVideoCall = false)
        }
    }

    private fun makeCall(isStringeeCall: Boolean, isVideoCall: Boolean) {
        val to: String = binding.etTo.text.toString().trim()
        if (to.isNotBlank()) {
            if (Common.client.isConnected) {
                val intent: Intent = if (isStringeeCall)
                    Intent(
                        this@MainActivity,
                        OutgoingCallActivity::class.java
                    ) else Intent(
                    this@MainActivity,
                    OutgoingCall2Activity::class.java
                )
                intent.putExtra("from", Common.client.userId)
                intent.putExtra("to", to)
                intent.putExtra("is_video_call", isVideoCall)
                launcher.launch(intent)
            } else {
                Common.reportMessage(this, "Stringee session not connected");
            }
        }
    }
}