package com.stringee.kotlin_onetoonecallsample.stringee.common

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TokenStoreTest {
    private var tokenStore: TokenStore? = null

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences(TokenStore.PREFS, Context.MODE_PRIVATE)
            .edit().clear().commit()
        tokenStore = TokenStore(context)
    }

    @Test
    fun tokenIsEmptyUntilSuccessfulConnectionIsSaved() {
        Assert.assertEquals("", tokenStore!!.token)

        tokenStore!!.saveConnectedToken("  valid-token  ")

        Assert.assertEquals("valid-token", tokenStore!!.token)
    }

    @Test
    fun emptyTokenDoesNotReplaceLastSuccessfulToken() {
        tokenStore!!.saveConnectedToken("valid-token")

        tokenStore!!.saveConnectedToken("   ")

        Assert.assertEquals("valid-token", tokenStore!!.token)
    }
}
