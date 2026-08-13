package com.stringee.widgetsample.common;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class WidgetTokenStoreTest {
    private Context context;
    private WidgetTokenStore tokenStore;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE)
                .edit().clear().commit();
        tokenStore = new WidgetTokenStore(context);
    }

    @Test
    public void savesOnlyTrimmedSuccessfulToken() {
        assertEquals("", tokenStore.getToken());
        tokenStore.saveConnectedToken("  valid-token  ");
        assertEquals("valid-token", tokenStore.getToken());
    }

    @Test
    public void invalidTokenDoesNotReplaceLastSuccessfulToken() {
        tokenStore.saveConnectedToken("valid-token");
        tokenStore.saveConnectedToken("   ");
        assertEquals("valid-token", tokenStore.getToken());
    }

    @Test
    public void tokenRestoresFromPrivatePreferences() {
        tokenStore.saveConnectedToken("persisted-token");
        assertEquals("persisted-token", new WidgetTokenStore(context).getToken());
    }
}
