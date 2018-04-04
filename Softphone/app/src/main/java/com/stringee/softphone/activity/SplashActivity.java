package com.stringee.softphone.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.stringee.softphone.R;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.PrefUtils;

/**
 * Created by luannguyen on 7/10/2017.
 */

public class SplashActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                boolean logined = PrefUtils.getInstance(SplashActivity.this).getBoolean(Constant.PREF_LOGINED, false);
                if (logined) {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                } else {
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
            }
        }, 1000);
    }
}
