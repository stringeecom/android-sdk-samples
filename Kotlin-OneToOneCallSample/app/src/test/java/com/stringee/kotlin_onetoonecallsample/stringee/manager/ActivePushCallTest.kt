package com.stringee.kotlin_onetoonecallsample.stringee.manager

import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.claim
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.clear
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.clearIfMatches
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.consumeAction
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.generation
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.hasActiveCall
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.isCurrent
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.requestAction
import org.junit.After
import org.junit.Assert
import org.junit.Test

class ActivePushCallTest {
    @After
    fun tearDown() {
        clear()
    }

    @Test
    fun terminalPushOnlyClearsMatchingCall() {
        Assert.assertTrue(claim("call-1"))

        Assert.assertFalse(clearIfMatches("call-2"))
        Assert.assertTrue(isCurrent("call-1"))
        Assert.assertTrue(clearIfMatches("call-1"))
        Assert.assertFalse(hasActiveCall())
    }

    @Test
    fun pendingActionRequiresMatchingGenerationAndIsConsumedOnce() {
        Assert.assertTrue(claim("call-1"))
        val generation = generation

        Assert.assertFalse(
            requestAction(
                "call-1", generation - 1, ActivePushCall.PendingAction.ANSWER
            )
        )
        Assert.assertTrue(
            requestAction(
                "call-1", generation, ActivePushCall.PendingAction.ANSWER
            )
        )
        Assert.assertEquals(
            ActivePushCall.PendingAction.ANSWER,
            consumeAction("call-1")
        )
        Assert.assertEquals(
            ActivePushCall.PendingAction.NONE,
            consumeAction("call-1")
        )
    }

    @Test
    fun rejectFromAnOldGenerationCannotAffectANewCall() {
        Assert.assertTrue(claim("call-1"))
        val oldGeneration = generation
        Assert.assertTrue(clearIfMatches("call-1"))
        Assert.assertTrue(claim("call-2"))

        Assert.assertFalse(
            requestAction(
                "call-2", oldGeneration,
                ActivePushCall.PendingAction.REJECT
            )
        )
        Assert.assertEquals(
            ActivePushCall.PendingAction.NONE,
            consumeAction("call-2")
        )
    }
}
