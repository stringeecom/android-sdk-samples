package com.stringee.kotlin_onetoonecallsample.stringee.manager

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.stringee.exception.StringeeError
import com.stringee.kotlin_onetoonecallsample.stringee.common.CallStatus
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CallSessionCleanupTest {
    @Test
    fun releaseIsIdempotent() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val owner = CountingOwner()
        val session = CallSession(context, owner, 1, CallStatus.CALLING)

        session.release()
        session.release()

        Assert.assertEquals(1, owner.releaseCount.toLong())
    }

    private class CountingOwner : CallSession.Owner {
        var releaseCount = 0
            private set

        override fun onSessionStateChanged(session: CallSession?, status: CallStatus) {
        }

        override fun onSessionError(session: CallSession?, action: String?, error: StringeeError?) {
        }

        override fun onSessionReleased(session: CallSession?) {
            releaseCount++
        }
    }
}
