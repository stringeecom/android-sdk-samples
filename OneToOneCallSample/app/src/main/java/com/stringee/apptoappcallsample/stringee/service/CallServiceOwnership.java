package com.stringee.apptoappcallsample.stringee.service;

/** Idempotent generation guard for starting and stopping a call-scoped service. */
final class CallServiceOwnership {
    private static final long NONE = -1;

    private long requestedGeneration = NONE;
    private long runningGeneration = NONE;

    synchronized boolean request(long generation) {
        if (generation < 0) {
            return false;
        }
        if (requestedGeneration == generation || runningGeneration == generation) {
            return false;
        }
        if (requestedGeneration != NONE || runningGeneration != NONE) {
            return false;
        }
        requestedGeneration = generation;
        return true;
    }

    synchronized boolean markRunning(long generation) {
        if (generation < 0 || requestedGeneration != generation) {
            return false;
        }
        requestedGeneration = NONE;
        runningGeneration = generation;
        return true;
    }

    synchronized boolean release(long generation) {
        if (generation < 0 || !isOwnedBy(generation)) {
            return false;
        }
        requestedGeneration = NONE;
        runningGeneration = NONE;
        return true;
    }

    synchronized boolean isOwnedBy(long generation) {
        return generation >= 0
                && (requestedGeneration == generation || runningGeneration == generation);
    }

    synchronized boolean isRequested(long generation) {
        return generation >= 0 && requestedGeneration == generation;
    }

    synchronized boolean isRunning(long generation) {
        return generation >= 0 && runningGeneration == generation;
    }
}
