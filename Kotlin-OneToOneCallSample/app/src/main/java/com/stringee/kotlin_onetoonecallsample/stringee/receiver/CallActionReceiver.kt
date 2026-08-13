package com.stringee.kotlin_onetoonecallsample.stringee.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallConstants
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall
import com.stringee.kotlin_onetoonecallsample.stringee.manager.ActivePushCall.PendingAction
import com.stringee.kotlin_onetoonecallsample.stringee.manager.StringeeCallManager
import com.stringee.kotlin_onetoonecallsample.stringee.service.IncomingCallService

/** Validates notification ownership before rejecting or ending a call. */
class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null || intent.action == null) {
            return
        }
        val manager: StringeeCallManager = StringeeCallManager.Companion.getInstance(context)
        val session = manager.session
        if (CallConstants.ACTION_HANG_UP == intent.getAction()) {
            val generation = intent.getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1
            )
            if (session != null && manager.ownsSessionGeneration(generation)) {
                session.hangUp()
            }
            return
        }
        if (CallConstants.ACTION_REJECT != intent.getAction()) {
            return
        }
        val callId = intent.getStringExtra(CallConstants.EXTRA_CALL_ID)
        val generation = intent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1)
        val sessionGeneration = intent.getLongExtra(
            CallConstants.EXTRA_SESSION_GENERATION, -1
        )
        val hasMatchingIncomingSession = session != null && session.isIncoming
                && callId != null && callId == session.getCallId()
        val ownsPush = ActivePushCall.owns(callId, generation)
        val currentSessionGeneration = if (session == null) -1 else session.generation
        if (CallActionOwnership.canTargetActiveSession(
                hasMatchingIncomingSession,
                ownsPush,
                sessionGeneration,
                currentSessionGeneration
            )
        ) {
            session!!.reject()
            return
        }
        // A notification produced from an SDK callback belongs to one exact session. If that
        // session is gone, never downgrade its action to a pending push action for another call.
        if (sessionGeneration >= 0) {
            return
        }
        if (ActivePushCall.requestAction(
                callId, generation,
                PendingAction.REJECT
            )
        ) {
            IncomingCallService.Companion.hide(context, callId)
        }
    }
}
