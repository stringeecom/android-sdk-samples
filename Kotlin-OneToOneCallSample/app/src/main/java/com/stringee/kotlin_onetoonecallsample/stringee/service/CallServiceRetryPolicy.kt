package com.stringee.kotlin_onetoonecallsample.stringee.service

/** Retry policy that forbids background retries and retries for stale sessions. */
internal object CallServiceRetryPolicy {
    private const val MAX_RETRIES = 3

    @JvmStatic fun shouldRetry(appForeground: Boolean, sessionOwned: Boolean, attempt: Int): Boolean {
        return appForeground && sessionOwned && attempt >= 0 && attempt < MAX_RETRIES
    }
}
