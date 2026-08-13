package com.stringee.apptoappcallsample.stringee.common;

import static org.junit.Assert.assertEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class TokenStoreTest {
    private TokenStore tokenStore;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences(TokenStore.PREFS, Context.MODE_PRIVATE)
                .edit().clear().commit();
        tokenStore = new TokenStore(context);
    }

    @Test
    public void tokenIsEmptyUntilSuccessfulConnectionIsSaved() {
        assertEquals("", tokenStore.getToken());

        tokenStore.saveConnectedToken("  valid-token  ");

        assertEquals("valid-token", tokenStore.getToken());
    }

    @Test
    public void emptyTokenDoesNotReplaceLastSuccessfulToken() {
        tokenStore.saveConnectedToken("valid-token");

        tokenStore.saveConnectedToken("   ");

        assertEquals("valid-token", tokenStore.getToken());
    }
}
