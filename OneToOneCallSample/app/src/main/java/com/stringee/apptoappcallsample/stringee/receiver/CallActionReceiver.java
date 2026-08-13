package com.stringee.apptoappcallsample.stringee.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.stringee.apptoappcallsample.stringee.common.CallConstants;
import com.stringee.apptoappcallsample.stringee.manager.ActivePushCall;
import com.stringee.apptoappcallsample.stringee.manager.CallSession;
import com.stringee.apptoappcallsample.stringee.manager.StringeeCallManager;
import com.stringee.apptoappcallsample.stringee.service.IncomingCallService;

/** Validates notification action ownership before answering, rejecting, or ending a call. */
public class CallActionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }
        StringeeCallManager manager = StringeeCallManager.getInstance(context);
        CallSession session = manager.getSession();
        if (CallConstants.ACTION_HANG_UP.equals(intent.getAction())) {
            long generation = intent.getLongExtra(
                    CallConstants.EXTRA_SESSION_GENERATION, -1);
            if (session != null && manager.ownsSessionGeneration(generation)) {
                session.hangUp();
            }
            return;
        }
        if (!CallConstants.ACTION_REJECT.equals(intent.getAction())) {
            return;
        }
        String callId = intent.getStringExtra(CallConstants.EXTRA_CALL_ID);
        long generation = intent.getLongExtra(CallConstants.EXTRA_CALL_GENERATION, -1);
        long sessionGeneration = intent.getLongExtra(
                CallConstants.EXTRA_SESSION_GENERATION, -1);
        boolean hasMatchingIncomingSession = session != null
                && session.isIncoming()
                && callId != null
                && callId.equals(session.getCallId());
        boolean ownsPush = ActivePushCall.owns(callId, generation);
        long currentSessionGeneration = session == null ? -1 : session.getGeneration();
        if (CallActionOwnership.canTargetActiveSession(
                hasMatchingIncomingSession,
                ownsPush,
                sessionGeneration,
                currentSessionGeneration)) {
            session.reject();
            return;
        }
        // A notification produced from an SDK callback belongs to one exact session. If that
        // session is gone, never downgrade its action to a pending push action for another call.
        if (sessionGeneration >= 0) {
            return;
        }
        if (ActivePushCall.requestAction(callId, generation,
                ActivePushCall.PendingAction.REJECT)) {
            IncomingCallService.hide(context, callId);
        }
    }
}
