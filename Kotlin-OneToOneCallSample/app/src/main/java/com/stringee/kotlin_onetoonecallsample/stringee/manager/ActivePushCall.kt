package com.stringee.kotlin_onetoonecallsample.stringee.manager

/** Tracks incoming push ownership and pending notification actions by call generation. */
object ActivePushCall {
    @get:JvmStatic
    @get:Synchronized
    var callId: String = ""
        private set

    @get:JvmStatic
    @get:Synchronized
    var generation: Long = 0
        private set
    private var pendingAction: PendingAction? = PendingAction.NONE

    @Synchronized
    @JvmStatic
    fun claim(newCallId: String): Boolean {
        if (isEmpty(newCallId)) {
            return false
        }
        if (!isEmpty(callId)) {
            return callId == newCallId
        }
        callId = newCallId
        pendingAction = PendingAction.NONE
        generation++
        return true
    }

    @Synchronized
    @JvmStatic
    fun requestAction(
        expectedCallId: String?, expectedGeneration: Long,
        action: PendingAction?
    ): Boolean {
        if (!owns(
                expectedCallId,
                expectedGeneration
            ) || action == null || action == PendingAction.NONE
        ) {
            return false
        }
        pendingAction = action
        return true
    }

    @Synchronized
    @JvmStatic
    fun consumeAction(expectedCallId: String?): PendingAction? {
        if (!isCurrent(expectedCallId)) {
            return PendingAction.NONE
        }
        val action = pendingAction
        pendingAction = PendingAction.NONE
        return action
    }

    @Synchronized
    @JvmStatic
    fun clearIfMatches(terminalCallId: String?): Boolean {
        if (!isCurrent(terminalCallId)) {
            return false
        }
        clear()
        return true
    }

    @Synchronized
    @JvmStatic
    fun isCurrent(expectedCallId: String?): Boolean {
        return !isEmpty(expectedCallId) && expectedCallId == callId
    }

    @Synchronized
    @JvmStatic
    fun owns(expectedCallId: String?, expectedGeneration: Long): Boolean {
        return generation == expectedGeneration && isCurrent(expectedCallId)
    }

    @Synchronized
    @JvmStatic
    fun hasActiveCall(): Boolean {
        return !isEmpty(callId)
    }

    @Synchronized
    @JvmStatic
    fun clear() {
        callId = ""
        pendingAction = PendingAction.NONE
        generation++
    }

    private fun isEmpty(value: String?): Boolean {
        return value == null || value.trim { it <= ' ' }.isEmpty()
    }

    enum class PendingAction {
        NONE,
        ANSWER,
        REJECT
    }
}
