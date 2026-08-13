package com.stringee.apptoappcallsample.stringee.manager;

/** Tracks incoming push ownership and pending notification actions by call generation. */
public final class ActivePushCall {
    public enum PendingAction {
        NONE,
        ANSWER,
        REJECT
    }

    private static String callId = "";
    private static long generation;
    private static PendingAction pendingAction = PendingAction.NONE;

    private ActivePushCall() {
    }

    public static synchronized boolean claim(String newCallId) {
        if (isEmpty(newCallId)) {
            return false;
        }
        if (!isEmpty(callId)) {
            return callId.equals(newCallId);
        }
        callId = newCallId;
        pendingAction = PendingAction.NONE;
        generation++;
        return true;
    }

    public static synchronized boolean requestAction(String expectedCallId, long expectedGeneration,
                                              PendingAction action) {
        if (!owns(expectedCallId, expectedGeneration)
                || action == null || action == PendingAction.NONE) {
            return false;
        }
        pendingAction = action;
        return true;
    }

    public static synchronized PendingAction consumeAction(String expectedCallId) {
        if (!isCurrent(expectedCallId)) {
            return PendingAction.NONE;
        }
        PendingAction action = pendingAction;
        pendingAction = PendingAction.NONE;
        return action;
    }

    public static synchronized boolean clearIfMatches(String terminalCallId) {
        if (!isCurrent(terminalCallId)) {
            return false;
        }
        clear();
        return true;
    }

    public static synchronized boolean isCurrent(String expectedCallId) {
        return !isEmpty(expectedCallId) && expectedCallId.equals(callId);
    }

    public static synchronized boolean owns(String expectedCallId, long expectedGeneration) {
        return generation == expectedGeneration && isCurrent(expectedCallId);
    }

    public static synchronized boolean hasActiveCall() {
        return !isEmpty(callId);
    }

    public static synchronized String getCallId() {
        return callId;
    }

    public static synchronized long getGeneration() {
        return generation;
    }

    public static synchronized void clear() {
        callId = "";
        pendingAction = PendingAction.NONE;
        generation++;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
