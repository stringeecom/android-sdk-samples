package com.stringee.kotlin_onetoonecallsample.stringee.receiver

/** Pure ownership rule for targeting an active incoming session from a notification action. */
internal object CallActionOwnership {
    @JvmStatic fun canTargetActiveSession(
        hasMatchingIncomingSession: Boolean,
        ownsPush: Boolean,
        requestedSessionGeneration: Long,
        currentSessionGeneration: Long
    ): Boolean {
        return hasMatchingIncomingSession
                && ownsPush
                && (requestedSessionGeneration < 0
                || requestedSessionGeneration == currentSessionGeneration)
    }
}
