package com.stringee.kotlin_onetoonecallsample.common

import android.view.View
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AppCompatActivity(), View.OnClickListener {
    override fun onClick(view: View) {}
}