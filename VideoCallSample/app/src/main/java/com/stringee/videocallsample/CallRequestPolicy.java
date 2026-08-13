package com.stringee.videocallsample;

/** Validates host-side preconditions before opening the outgoing call activity. */
public final class CallRequestPolicy {
    public enum Error {
        NOT_CONNECTED,
        CALL_IN_PROGRESS,
        RECIPIENT_REQUIRED
    }

    private CallRequestPolicy() {
    }

    public static Error validate(boolean connected, boolean callInProgress, String recipient) {
        if (!connected) {
            return Error.NOT_CONNECTED;
        }
        if (callInProgress) {
            return Error.CALL_IN_PROGRESS;
        }
        if (normalizeRecipient(recipient).isEmpty()) {
            return Error.RECIPIENT_REQUIRED;
        }
        return null;
    }

    public static String normalizeRecipient(String recipient) {
        return recipient == null ? "" : recipient.trim();
    }
}
