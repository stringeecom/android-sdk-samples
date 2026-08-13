package com.stringee.kotlin_onetoonecallsample.stringee.common

import com.stringee.kotlin_onetoonecallsample.stringee.StringeeCallManager

/**
 * Immutable outgoing-call options consumed by [StringeeCallManager].
 *
 * @param to recipient Stringee user ID
 * @param callEngine SDK call engine to use
 * @param isVideoCall `true` for video, `false` for voice
 */
class StringeeCallConfig(to: String?, callEngine: CallEngine?, val isVideoCall: Boolean) {
    val to: String
    val callEngine: CallEngine
    var customData: String? = null
        private set

    init {
        this.to = if (to == null) "" else to.trim { it <= ' ' }
        this.callEngine = if (callEngine == null) CallEngine.STRINGEE_CALL else callEngine
    }

    /** Sets optional custom data forwarded with the call and returns this configuration. */
    fun setCustomData(customData: String?): StringeeCallConfig {
        this.customData = customData
        return this
    }
}
