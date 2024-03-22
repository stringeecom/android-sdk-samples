package com.stringee.kotlin_onetoonecallsample.activity

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleObserver
import com.stringee.kotlin_onetoonecallsample.R.id
import com.stringee.kotlin_onetoonecallsample.R.string
import com.stringee.kotlin_onetoonecallsample.common.Constant
import com.stringee.kotlin_onetoonecallsample.common.PermissionsUtils
import com.stringee.kotlin_onetoonecallsample.common.Utils
import com.stringee.kotlin_onetoonecallsample.databinding.ActivityMainBinding
import com.stringee.kotlin_onetoonecallsample.manager.ClientManager


class MainActivity : AppCompatActivity(), View.OnClickListener, LifecycleObserver {
    private lateinit var binding: ActivityMainBinding
    private lateinit var clientManager: ClientManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnVoiceCall.setOnClickListener(this)
        binding.btnVideoCall.setOnClickListener(this)
        binding.btnVoiceCall2.setOnClickListener(this)
        binding.btnVideoCall2.setOnClickListener(this)
        clientManager = ClientManager.getInstance(this)
        initAndConnectStringee()
        requestPermission()
    }

    private fun requestPermission() {
        if (!PermissionsUtils.getInstance().checkSelfPermission(this)) {
            PermissionsUtils.getInstance().requestPermissions(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val isGranted: Boolean = PermissionsUtils.getInstance().verifyPermissions(grantResults)
        if (requestCode == PermissionsUtils.REQUEST_PERMISSION) {
            clientManager.isPermissionGranted = isGranted
            if (!isGranted) {
                if (PermissionsUtils.getInstance().shouldRequestPermissionRationale(this)) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(string.app_name)
                    builder.setMessage("Permissions must be granted for the call")
                    builder.setPositiveButton(
                        "Ok"
                    ) { dialogInterface: DialogInterface, _: Int -> dialogInterface.cancel() }
                    builder.setNegativeButton("Settings") { dialogInterface: DialogInterface, _: Int ->
                        dialogInterface.cancel()
                        // open app setting
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        val uri =
                            Uri.fromParts("package", packageName, null)
                        intent.setData(uri)
                        startActivity(intent)
                    }
                    builder.create().show()
                }
            }
        }
    }

    private fun initAndConnectStringee() {
        clientManager.addOnConnectionListener { status: String? ->
            runOnUiThread { binding.tvStatus.text = status }
        }
        clientManager.connect()
    }

    override fun onClick(view: View) {
        when (view.id) {
            id.btn_voice_call -> {
                makeCall(isStringeeCall = true, isVideoCall = false)
            }

            id.btn_video_call -> {
                makeCall(isStringeeCall = true, isVideoCall = true)
            }

            id.btn_voice_call2 -> {
                makeCall(isStringeeCall = false, isVideoCall = false)
            }

            id.btn_video_call2 -> {
                makeCall(isStringeeCall = false, isVideoCall = true)
            }
        }
    }

    private fun makeCall(isStringeeCall: Boolean, isVideoCall: Boolean) {
        if (Utils.isStringEmpty(binding.etTo.text) || !clientManager.stringeeClient!!.isConnected) {
            return
        }
        if (!clientManager.isPermissionGranted) {
            PermissionsUtils.getInstance().requestPermissions(this)
            return
        }
        val intent = Intent(this, CallActivity::class.java)
        intent.putExtra(Constant.PARAM_TO, binding.etTo.text.toString().trim())
        intent.putExtra(Constant.PARAM_IS_VIDEO_CALL, isVideoCall)
        intent.putExtra(Constant.PARAM_IS_INCOMING_CALL, false)
        intent.putExtra(Constant.PARAM_IS_STRINGEE_CALL, isStringeeCall)
        startActivity(intent)
    }
}

