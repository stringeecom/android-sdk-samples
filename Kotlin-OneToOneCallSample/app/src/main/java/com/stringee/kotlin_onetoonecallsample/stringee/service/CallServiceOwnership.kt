package com.stringee.kotlin_onetoonecallsample.stringee.service

/** Idempotent generation guard for starting and stopping a call-scoped service. */
internal class CallServiceOwnership {
    private var requestedGeneration: Long = NONE
    private var runningGeneration: Long = NONE

    @Synchronized
    fun request(generation: Long): Boolean {
        if (generation < 0) {
            return false
        }
        if (requestedGeneration == generation || runningGeneration == generation) {
            return false
        }
        if (requestedGeneration != NONE || runningGeneration != NONE) {
            return false
        }
        requestedGeneration = generation
        return true
    }

    @Synchronized
    fun markRunning(generation: Long): Boolean {
        if (generation < 0 || requestedGeneration != generation) {
            return false
        }
        requestedGeneration = NONE
        runningGeneration = generation
        return true
    }

    @Synchronized
    fun release(generation: Long): Boolean {
        if (generation < 0 || !isOwnedBy(generation)) {
            return false
        }
        requestedGeneration = NONE
        runningGeneration = NONE
        return true
    }

    @Synchronized
    fun isOwnedBy(generation: Long): Boolean {
        return generation >= 0
                && (requestedGeneration == generation || runningGeneration == generation)
    }

    @Synchronized
    fun isRequested(generation: Long): Boolean {
        return generation >= 0 && requestedGeneration == generation
    }

    @Synchronized
    fun isRunning(generation: Long): Boolean {
        return generation >= 0 && runningGeneration == generation
    }

    companion object {
        private val NONE: Long = -1
    }
}
