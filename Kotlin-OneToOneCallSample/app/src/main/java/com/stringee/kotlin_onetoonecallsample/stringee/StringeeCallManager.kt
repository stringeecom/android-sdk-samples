package com.stringee.kotlin_onetoonecallsample.stringee

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.stringee.kotlin_onetoonecallsample.stringee.common.StringeeCallConfig
import com.stringee.kotlin_onetoonecallsample.stringee.listener.StringeeCallListener
import com.stringee.listener.StatusListener
import com.stringee.kotlin_onetoonecallsample.stringee.manager.StringeeCallManager as InternalCallManager

/**
 * Public Kotlin facade for the sample integration. UI code only talks to this class; SDK calls,
 * notifications, receivers and call-scoped services remain private implementation details.
 */
class StringeeCallManager private constructor(context: Context) {
    private val delegate = InternalCallManager.getInstance(context.applicationContext)

    /** Whether the Stringee client is currently connected. */
    val isConnected: Boolean
        get() = delegate.isConnected

    /** Connected Stringee user ID, or an empty string when disconnected. */
    val connectedUserId: String
        get() = delegate.connectedUserId

    /** Access token from the most recent successful connection. */
    val savedToken: String
        get() = delegate.savedToken

    /** Registers host callbacks and auto-connects the saved token once per process. */
    fun initialize(listener: StringeeCallListener) = delegate.initialize(listener)

    /** Connects with [token], which is persisted only after a successful connection. */
    fun connect(token: String) = delegate.connect(token)

    /** Disconnects the current session while retaining the last successful token. */
    fun disconnect() = delegate.disconnect()

    /** Releases lifecycle callbacks, the active call, client, and singleton instance. */
    fun release() = delegate.release()

    /** Prepares an outgoing call and opens the permission-aware call activity. */
    fun makeCall(config: StringeeCallConfig, listener: StatusListener) =
        delegate.makeCall(config, listener)

    /** Consumes Answer/Reject metadata from an incoming-call notification intent. */
    fun handleLaunchIntent(intent: Intent?) = delegate.handleLaunchIntent(intent)

    fun canPostNotifications(): Boolean = delegate.canPostNotifications()

    fun canUseFullScreenIntent(): Boolean = delegate.canUseFullScreenIntent()

    fun requestNotificationPermission(activity: Activity) =
        delegate.requestNotificationPermission(activity)

    fun openFullScreenIntentSettings(activity: Activity) =
        delegate.openFullScreenIntentSettings(activity)

    fun openAppSettings(activity: Activity) = delegate.openAppSettings(activity)

    companion object {
        @Volatile
        private var instance: StringeeCallManager? = null

        @JvmStatic
        fun getInstance(context: Context): StringeeCallManager =
            instance ?: synchronized(this) {
                instance ?: StringeeCallManager(context).also { instance = it }
            }
    }
}
