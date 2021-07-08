package com.stringee.kotlin_onetoonecallsample

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.stringee.StringeeClient
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall2
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityMainBinding
import com.stringee.listener.StringeeConnectionListener
import org.json.JSONObject

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private var token = "eyJjdHkiOiJzdHJpbmdlZS1hcGk7dj0xIiwidHlwIjoiSldUIiwiYWxnIjoiSFMyNTYifQ.eyJqdGkiOiJTS0UxUmRVdFVhWXhOYVFRNFdyMTVxRjF6VUp1UWRBYVZULTE2MjUwMjUxMjYiLCJpc3MiOiJTS0UxUmRVdFVhWXhOYVFRNFdyMTVxRjF6VUp1UWRBYVZUIiwiZXhwIjoxNjI3NjE3MTI2LCJ1c2VySWQiOiJ1c2VyMSJ9.ht9SLUGRMLcZyK22zvP78_XsqQICAAfsAVI-IMsvdj0"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVideoCall.setOnClickListener(this)
        binding.btnVideoCall2.setOnClickListener(this)
        binding.btnVoiceCall.setOnClickListener(this)
        binding.btnVoiceCall2.setOnClickListener(this)

        initStringee()
    }

    private fun initStringee() {
        Common.client = StringeeClient(this)
        Common.client.setConnectionListener(object : StringeeConnectionListener {
            override fun onConnectionConnected(p0: StringeeClient, p1: Boolean) {
                runOnUiThread {
                    binding.tvUserid.text = "Connected as: ${p0.userId}"
                    Common.reportMessage(this@MainActivity, "StringeeClient is connected.")
                }
            }

            override fun onConnectionDisconnected(p0: StringeeClient, p1: Boolean) {
                runOnUiThread {
                    binding.tvUserid.text = "dissconnected"
                    Common.reportMessage(this@MainActivity, "StringeeClient is disconnected.")
                }
            }

            override fun onIncomingCall(p0: StringeeCall) {
                runOnUiThread {
                    if (Common.isInCall) p0.reject() else {
                        Common.callsMap[p0.callId] = p0
                        val intent = Intent(
                            this@MainActivity,
                            IncomingCallActivity::class.java
                        ).apply { putExtra("call_id", p0.callId) }
                        startActivity(intent)
                    }
                }
            }

            override fun onIncomingCall2(p0: StringeeCall2) {
                runOnUiThread {
                    if (Common.isInCall) p0.reject() else {
                        Common.call2sMap[p0.callId] = p0
                        val intent = Intent(
                            this@MainActivity,
                            IncomingCall2Activity::class.java
                        ).apply { putExtra("call_id", p0.callId) }
                        startActivity(intent)
                    }
                }
            }

            override fun onConnectionError(p0: StringeeClient?, p1: StringeeError) {
                runOnUiThread {
                    Common.reportMessage(
                        this@MainActivity,
                        "StringeeClient fails to connect: ${p1.getMessage()}"
                    )
                }
            }

            override fun onRequestNewToken(p0: StringeeClient?) {
                TODO("Not yet implemented")
            }

            override fun onCustomMessage(p0: String?, p1: JSONObject?) {
                TODO("Not yet implemented")
            }

            override fun onTopicMessage(p0: String?, p1: JSONObject?) {
                TODO("Not yet implemented")
            }
        })
        Common.client.connect(token)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_video_call -> {
                makeCall(true, true)
            }
            R.id.btn_video_call2 -> {
                makeCall(false, true)
            }
            R.id.btn_voice_call -> {
                makeCall(true, false)
            }
            R.id.btn_voice_call2 -> {
                makeCall(false, false)
            }
        }
    }

    fun makeCall(isStringeeCall: Boolean, isVideoCall: Boolean) {
        val to: String = binding.etTo.text.toString().trim()
        if (to.isNotBlank()) {
            if (Common.client.isConnected) {
                val intent: Intent
                if (isStringeeCall)
                    intent = Intent(
                        this@MainActivity,
                        OutgoingCallActivity::class.java
                    ) else intent = Intent(
                    this@MainActivity,
                    OutgoingCall2Activity::class.java
                )
                intent.putExtra("to", to)
                intent.putExtra("is_video_call", isVideoCall)
                startActivity(intent)
            } else {
                Common.reportMessage(this, "Stringee session not connected");
            }
        }
    }
}