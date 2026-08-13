package com.stringee.apptoappcallsample.stringee.service;

/** Retry policy that forbids background retries and retries for stale sessions. */
final class CallServiceRetryPolicy {
    private static final int MAX_RETRIES = 3;

    private CallServiceRetryPolicy() {
    }

    static boolean shouldRetry(boolean appForeground, boolean sessionOwned, int attempt) {
        return appForeground && sessionOwned && attempt >= 0 && attempt < MAX_RETRIES;
    }
}
