package com.stringee.kotlin_onetoonecallsample.stringee.manager

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.stringee.StringeeClient
import com.stringee.call.StringeeCall
import com.stringee.call.StringeeCall2
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.stringee.activity.CallActivity
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallConstants
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallPermissions
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallStatus
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallUtils
import com.stringee.kotlin_onetoonecallsample.stringee.common.ConnectionState
import com.stringee.kotlin_onetoonecallsample.stringee.common.StringeeCallConfig
import com.stringee.kotlin_onetoonecallsample.stringee.common.TokenStore
import com.stringee.kotlin_onetoonecallsample.stringee.listener.StringeeCallListener
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.PendingAction
import com.stringee.kotlin_onetoonecallsample.stringee.service.InCallService
import com.stringee.kotlin_onetoonecallsample.stringee.service.IncomingCallService
import com.stringee.listener.StatusListener
import com.stringee.listener.StringeeConnectionListener
import org.json.JSONObject
import kotlin.concurrent.Volatile
import kotlin.math.max

/**
 * Internal implementation behind the public facade.
 *
 * Owns the Stringee client, session generation, incoming push synchronization, and call-scoped
 * Android components.
 */
class StringeeCallManager private constructor(context: Context) : CallSession.Owner {
    private val context: Context
    private val tokenStore: TokenStore
    private val application: Application
    private var client: StringeeClient? = null
    private var connectionListener: StringeeConnectionListener? = null
    private var listener: StringeeCallListener? = null
    private var pendingAccessToken = ""
    private var connectionState: ConnectionState? = ConnectionState.DISCONNECTED
    private var initialized = false
    private var autoConnectAttempted = false
    private var startedActivityCount = 0
    private var sessionGeneration: Long = 0
    var session: CallSession? = null
        private set
    private var lifecycleCallbacks: ActivityLifecycleCallbacks? = null

    init {
        this.context = context.getApplicationContext()
        tokenStore = TokenStore(context)
        application = this.context as Application
    }

    fun initialize(listener: StringeeCallListener?) {
        this.listener = listener
        if (!initialized) {
            initialized = true
            registerActivityLifecycle()
        }
        if (!autoConnectAttempted) {
            autoConnectAttempted = true
            val savedToken = tokenStore.token
            if (!CallUtils.isEmpty(savedToken)) {
                connect(savedToken)
            }
        }
    }

    fun connect(token: String?) {
        val normalized = if (token == null) "" else token.trim { it <= ' ' }
        if (normalized.isEmpty()) {
            reportError(
                "connect", StringeeError(
                    ERROR_INVALID_TOKEN,
                    "Access token is required"
                )
            )
            return
        }
        if (client != null && client!!.isConnected()) {
            if (normalized == pendingAccessToken
                || normalized == tokenStore.token
            ) {
                notifyConnection(ConnectionState.CONNECTED, client!!.getUserId())
                return
            }
            disposeClient()
        }
        if (client != null && connectionState == ConnectionState.CONNECTING) {
            if (isSamePendingConnection(normalized, pendingAccessToken, connectionState)) {
                notifyConnection(ConnectionState.CONNECTING, "")
                return
            }
            disposeClient()
        }
        pendingAccessToken = normalized
        ensureClient()
        notifyConnection(ConnectionState.CONNECTING, "")
        if (!client!!.isConnected()) {
            try {
                client!!.connect(normalized)
            } catch (exception: RuntimeException) {
                CallUtils.reportException(StringeeCallManager::class.java, exception)
                notifyConnection(ConnectionState.ERROR, "")
                reportError(
                    "connect", StringeeError(
                        ERROR_INVALID_TOKEN,
                        if (exception.message == null)
                            "Unable to connect"
                        else
                            exception.message
                    )
                )
            }
        }
    }

    fun connectSavedTokenForPush() {
        if (client != null && client!!.isConnected()) {
            return
        }
        val token = tokenStore.token
        if (CallUtils.isEmpty(token)) {
            reportError(
                "incomingPush", StringeeError(
                    ERROR_INVALID_TOKEN,
                    "No previously connected token is available"
                )
            )
            IncomingCallService.Companion.clear(context, ActivePushCall.callId)
            return
        }
        connect(token)
    }

    private fun ensureClient() {
        if (client != null) {
            return
        }
        client = StringeeClient(context)
        connectionListener = object : StringeeConnectionListener {
            override fun onConnectionConnected(
                stringeeClient: StringeeClient,
                isReconnecting: Boolean
            ) {
                CallUtils.runOnUiThread(Runnable {
                    if (client !== stringeeClient) {
                        return@Runnable
                    }
                    if (!CallUtils.isEmpty(pendingAccessToken)) {
                        tokenStore.saveConnectedToken(pendingAccessToken)
                    }
                    notifyConnection(ConnectionState.CONNECTED, stringeeClient.getUserId())
                    registerCurrentPushToken()
                })
            }

            override fun onConnectionDisconnected(
                stringeeClient: StringeeClient?,
                isReconnecting: Boolean
            ) {
                CallUtils.runOnUiThread(Runnable {
                    if (client === stringeeClient) {
                        notifyConnection(stateAfterDisconnect(isReconnecting), "")
                    }
                })
            }

            override fun onIncomingCall(call: StringeeCall) {
                CallUtils.runOnUiThread(Runnable { prepareIncoming(call) })
            }

            override fun onIncomingCall2(call: StringeeCall2) {
                CallUtils.runOnUiThread(Runnable { prepareIncoming(call) })
            }

            override fun onConnectionError(stringeeClient: StringeeClient?, error: StringeeError?) {
                CallUtils.runOnUiThread(Runnable {
                    if (client === stringeeClient) {
                        notifyConnection(ConnectionState.ERROR, "")
                        reportError("connect", error)
                    }
                })
            }

            override fun onRequestNewToken(stringeeClient: StringeeClient?) {
                CallUtils.runOnUiThread(Runnable {
                    if (listener != null) {
                        listener!!.onRequestNewToken()
                    }
                })
            }

            override fun onCustomMessage(from: String?, message: JSONObject?) {
                Log.d(CallConstants.TAG, "onCustomMessage from " + from)
            }

            override fun onTopicMessage(from: String?, message: JSONObject?) {
                Log.d(CallConstants.TAG, "onTopicMessage from " + from)
            }
        }
        client!!.addConnectionListener(connectionListener)
    }

    fun makeCall(config: StringeeCallConfig?, callback: StatusListener?) {
        if (!this.isConnected) {
            dispatchOperationError(
                callback, "makeCall", StringeeError(
                    ERROR_NOT_CONNECTED,
                    "StringeeClient is not connected"
                )
            )
            return
        }
        if (session != null) {
            dispatchOperationError(
                callback, "makeCall", StringeeError(
                    ERROR_ALREADY_IN_CALL,
                    "Another call is active"
                )
            )
            return
        }
        if (config == null || CallUtils.isEmpty(config.to)) {
            dispatchOperationError(
                callback, "makeCall", StringeeError(
                    ERROR_INVALID_CONFIG,
                    "A recipient is required"
                )
            )
            return
        }
        session = CallSession(context, this, ++sessionGeneration, CallStatus.CALLING)
        session!!.prepareOutgoing(requireNotNull(client), config)
        session!!.setOutgoingCallback(callback)
        InCallService.Companion.startOrUpdate(context, sessionGeneration)
        openCallActivity(false)
    }

    private fun prepareIncoming(call: StringeeCall) {
        if (session != null) {
            call.reject(object : StatusListener() {
                override fun onSuccess() {
                }
            })
            IncomingCallService.Companion.clear(context, call.getCallId())
            return
        }
        if (!claimIncoming(call.getCallId())) {
            call.reject(object : StatusListener() {
                override fun onSuccess() {
                }
            })
            return
        }
        session = CallSession(context, this, ++sessionGeneration, CallStatus.INCOMING)
        session!!.prepareIncoming(call)
        finishPreparingIncoming()
    }

    private fun prepareIncoming(call: StringeeCall2) {
        if (session != null) {
            call.reject(object : StatusListener() {
                override fun onSuccess() {
                }
            })
            IncomingCallService.Companion.clear(context, call.getCallId())
            return
        }
        if (!claimIncoming(call.getCallId())) {
            call.reject(object : StatusListener() {
                override fun onSuccess() {
                }
            })
            return
        }
        session = CallSession(context, this, ++sessionGeneration, CallStatus.INCOMING)
        session!!.prepareIncoming(call)
        finishPreparingIncoming()
    }

    private fun claimIncoming(callId: String?): Boolean {
        return !callId.isNullOrBlank() &&
                (ActivePushCall.isCurrent(callId) || ActivePushCall.claim(callId))
    }

    private fun finishPreparingIncoming() {
        session!!.ringing()
        IncomingCallService.Companion.showFromSdk(
            context, session!!.from, "", session!!.isVideoCall,
            session!!.getCallId(), session!!.engine, sessionGeneration
        )
        val action = ActivePushCall.consumeAction(session!!.getCallId())
        if (action == PendingAction.REJECT) {
            session!!.reject()
        } else if (action == PendingAction.ANSWER) {
            openCallActivity(true)
        } else if (this.isAppForeground) {
            openCallActivity(false)
        }
    }

    fun handleLaunchIntent(intent: Intent?) {
        if (intent == null) {
            return
        }
        val actionName = intent.getStringExtra(CallConstants.EXTRA_PENDING_ACTION)
        val callId = intent.getStringExtra(CallConstants.EXTRA_CALL_ID)
        val generation = intent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1)
        if (!CallUtils.isEmpty(actionName)) {
            try {
                val action = PendingAction.valueOf(
                    actionName!!
                )
                ActivePushCall.requestAction(callId, generation, action)
            } catch (exception: IllegalArgumentException) {
                CallUtils.reportException(StringeeCallManager::class.java, exception)
            }
        }
        if (session != null && session!!.isIncoming
            && (CallUtils.isEmpty(callId) || callId == session!!.getCallId())
        ) {
            val action = ActivePushCall.consumeAction(
                session!!.getCallId()
            )
            openCallActivity(action == PendingAction.ANSWER)
        }
    }

    private fun openCallActivity(answer: Boolean) {
        if (session == null) {
            return
        }
        val intent = Intent(context, CallActivity::class.java)
            .addFlags(
                (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            .putExtra(CallConstants.EXTRA_ANSWER, answer)
            .putExtra(CallConstants.EXTRA_SESSION_GENERATION, sessionGeneration)
        context.startActivity(intent)
    }

    fun startPreparedOutgoing() {
        if (session == null) {
            return
        }
        session!!.startOutgoing()
    }

    fun disconnect() {
        if (session != null) {
            session!!.endForDisconnect()
        }
        if (client != null) {
            disposeClient()
        }
        notifyConnection(ConnectionState.DISCONNECTED, "")
    }

    fun release() {
        disconnect()
        if (lifecycleCallbacks != null) {
            application.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        }
        lifecycleCallbacks = null
        listener = null
        initialized = false
        instance = null
    }

    val isConnected: Boolean
        get() = client != null && client!!.isConnected()

    val connectedUserId: String
        get() = if (this.isConnected) client!!.getUserId().orEmpty() else ""

    val savedToken: String
        get() = tokenStore.token

    private fun disposeClient() {
        val disposedClient = client
        client = null
        if (disposedClient == null) {
            return
        }
        if (connectionListener != null) {
            disposedClient.removeConnectionListener(connectionListener)
            connectionListener = null
        }
        disposedClient.disconnect()
    }

    fun canPostNotifications(): Boolean {
        return CallPermissions.canPostNotifications(context)
    }

    fun canUseFullScreenIntent(): Boolean {
        return CallPermissions.canUseFullScreenIntent(context)
    }

    fun requestNotificationPermission(activity: Activity) {
        CallPermissions.requestNotificationPermission(activity)
    }

    fun openFullScreenIntentSettings(activity: Activity) {
        CallPermissions.openFullScreenIntentSettings(activity)
    }

    fun openAppSettings(activity: Activity) {
        CallPermissions.openAppSettings(activity)
    }

    fun hasActiveCall(): Boolean {
        return session != null
    }

    fun handleTerminalPush(callId: String?) {
        if (CallUtils.isEmpty(callId) || !ActivePushCall.isCurrent(callId)) {
            return
        }
        if (session != null && callId == session!!.getCallId()) {
            session!!.release()
        } else {
            IncomingCallService.Companion.clear(context, callId)
        }
    }

    fun isCurrentSession(candidate: CallSession?, generation: Long): Boolean {
        return session == candidate && sessionGeneration == generation
    }

    fun ownsSessionGeneration(generation: Long): Boolean {
        return session != null && sessionGeneration == generation
    }

    fun ownsSession(generation: Long, callId: String?): Boolean {
        return ownsSessionGeneration(generation) && session != null && (CallUtils.isEmpty(callId) || callId == session!!.getCallId())
    }

    fun onInCallServiceStartFailed(generation: Long) {
        if (ownsSessionGeneration(generation)) {
            session!!.onInCallServiceStartFailed()
        }
    }

    fun onIncomingNotificationStartFailed(generation: Long, callId: String?) {
        if (ownsSession(generation, callId) && session!!.isIncoming) {
            session!!.reject()
        }
    }

    private fun registerCurrentPushToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful || CallUtils.isEmpty(task.result)) {
                    return@addOnCompleteListener
                }
                registerPushToken(task.result)
            }
    }

    fun registerPushToken(token: String?) {
        tokenStore.saveFcmToken(token)
        if (!this.isConnected) {
            return
        }
        client!!.registerPushToken(token, object : StatusListener() {
            override fun onSuccess() {
                Log.d(CallConstants.TAG, "Push token registered")
            }

            override fun onError(error: StringeeError?) {
                reportError("registerPushToken", error)
            }
        })
    }

    private fun registerActivityLifecycle() {
        lifecycleCallbacks = object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, state: Bundle?) {
            }

            override fun onActivityStarted(activity: Activity) {
                startedActivityCount++
            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
                startedActivityCount = max(0, startedActivityCount - 1)
            }

            override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
            }
        }
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    val isAppForeground: Boolean
        get() = startedActivityCount > 0

    private fun notifyConnection(state: ConnectionState, userId: String?) {
        connectionState = state
        if (listener != null) {
            listener!!.onConnectionStateChanged(state, if (userId == null) "" else userId)
        }
    }

    private fun reportError(action: String?, error: StringeeError?) {
        if (listener != null) {
            listener!!.onError(
                action.orEmpty(),
                error ?: StringeeError(-1, "Unknown Stringee error")
            )
        }
    }

    private fun dispatchOperationError(
        callback: StatusListener?, action: String?,
        error: StringeeError?
    ) {
        if (callback != null) {
            callback.onError(error)
        }
        reportError(action, error)
    }

    override fun onSessionStateChanged(session: CallSession?, status: CallStatus) {
        if (this.session == session && listener != null) {
            listener!!.onCallStateChanged(status)
        }
    }

    override fun onSessionError(session: CallSession?, action: String?, error: StringeeError?) {
        if (this.session == session) {
            reportError(action, error)
        }
    }

    override fun onSessionReleased(session: CallSession?) {
        if (this.session == session) {
            this.session = null
        }
    }

    companion object {
        const val ERROR_INVALID_TOKEN: Int = 100
        const val ERROR_NOT_CONNECTED: Int = 101
        const val ERROR_ALREADY_IN_CALL: Int = 102
        const val ERROR_INVALID_CONFIG: Int = 103

        @Volatile
        private var instance: StringeeCallManager? = null

        @JvmStatic fun getInstance(context: Context): StringeeCallManager {
            if (instance == null) {
                synchronized(StringeeCallManager::class.java) {
                    if (instance == null) {
                        instance = StringeeCallManager(context)
                    }
                }
            }
            return requireNotNull(instance)
        }

        @JvmStatic fun isSamePendingConnection(
            requestedToken: String?, activeToken: String?,
            state: ConnectionState?
        ): Boolean {
            return state == ConnectionState.CONNECTING && requestedToken != null && requestedToken == activeToken
        }

        @JvmStatic fun stateAfterDisconnect(reconnecting: Boolean): ConnectionState {
            return if (reconnecting) ConnectionState.CONNECTING else ConnectionState.DISCONNECTED
        }
    }
}
