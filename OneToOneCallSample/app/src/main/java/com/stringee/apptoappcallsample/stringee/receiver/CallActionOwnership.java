package com.stringee.apptoappcallsample.stringee.receiver;

/** Pure ownership rule for targeting an active incoming session from a notification action. */
final class CallActionOwnership {
    private CallActionOwnership() {
    }

    static boolean canTargetActiveSession(boolean hasMatchingIncomingSession,
                                          boolean ownsPush,
                                          long requestedSessionGeneration,
                                          long currentSessionGeneration) {
        return hasMatchingIncomingSession
                && ownsPush
                && (requestedSessionGeneration < 0
                || requestedSessionGeneration == currentSessionGeneration);
    }
}
