package com.stringee.apptoappcallsample.stringee.manager;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.stringee.apptoappcallsample.stringee.common.CallStatus;
import com.stringee.exception.StringeeError;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class CallSessionCleanupTest {
    @Test
    public void releaseIsIdempotent() {
        Context context = ApplicationProvider.getApplicationContext();
        CountingOwner owner = new CountingOwner();
        CallSession session = new CallSession(context, owner, 1, CallStatus.CALLING);

        session.release();
        session.release();

        assertEquals(1, owner.releaseCount);
    }

    private static final class CountingOwner implements CallSession.Owner {
        private int releaseCount;

        @Override
        public void onSessionStateChanged(CallSession session, CallStatus status) {
        }

        @Override
        public void onSessionError(CallSession session, String action, StringeeError error) {
        }

        @Override
        public void onSessionReleased(CallSession session) {
            releaseCount++;
        }
    }
}
