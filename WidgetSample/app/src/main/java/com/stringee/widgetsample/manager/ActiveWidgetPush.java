package com.stringee.widgetsample.manager;

/**
 * Process-local ownership record for the incoming call announced by FCM.
 *
 * <p>Matching by call ID prevents a stale terminal push from clearing a newer call.</p>
 */
final class ActiveWidgetPush {
    static final class Snapshot {
        final String callId;
        final String from;
        final String alias;
        final boolean video;
        final long generation;

        Snapshot(String callId, String from, String alias, boolean video, long generation) {
            this.callId = callId;
            this.from = from;
            this.alias = alias;
            this.video = video;
            this.generation = generation;
        }
    }

    private static Snapshot active;
    private static long nextGeneration;

    private ActiveWidgetPush() { }

    static synchronized boolean claim(String callId, String from, String alias, boolean video) {
        if (callId == null || callId.trim().isEmpty()) {
            return false;
        }
        if (active != null) {
            return callId.equals(active.callId);
        }
        active = new Snapshot(callId, safe(from), safe(alias), video, ++nextGeneration);
        return true;
    }

    static synchronized Snapshot snapshot() {
        return active;
    }

    static synchronized boolean clearIfMatches(String callId) {
        if (active == null || callId == null || !callId.equals(active.callId)) {
            return false;
        }
        active = null;
        return true;
    }

    static synchronized void clear() {
        active = null;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
