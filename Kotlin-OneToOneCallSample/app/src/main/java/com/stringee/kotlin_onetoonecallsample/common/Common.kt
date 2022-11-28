package com.stringee.kotlin_onetoonecallsample.common

import com.stringee.StringeeClient
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall2

object Common {
    var client: StringeeClient? = null
    var callsMap: MutableMap<String, StringeeCall> = HashMap()
    var calls2Map: MutableMap<String, StringeeCall2> = HashMap()
    var isInCall = false
}
