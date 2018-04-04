package com.stringee.softphone.activity;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.stringee.softphone.common.Common;
import com.stringee.softphone.database.MessageHandler;

import io.fabric.sdk.android.Fabric;

/**
 * Created by luannguyen on 7/27/2017.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
        Common.messageDb = MessageHandler.getInstance(this);
        Common.context = this;
    }
}
