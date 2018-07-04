package com.stringee.softphone.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.stringee.softphone.R;
import com.stringee.softphone.common.CallBack;
import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.PrefUtils;
import com.stringee.softphone.fragment.ProgressDialogFragment;

/**
 * Created by luannguyen on 7/19/2017.
 */

public class MActivity extends AppCompatActivity implements View.OnClickListener, CallBack {

    private ProgressDialogFragment progressDialogFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        Common.isVisible = true;
        if (Common.client != null && !Common.client.isConnected()) {
            if (!Common.isConnecting) {
                long expiredTime = 1000 * PrefUtils.getInstance(this).getLong(Constant.PREF_EXPIRED_TIME, 0);
                long currentTime = System.currentTimeMillis();
                if (currentTime > expiredTime) {
                    MainActivity.getTokenAndConnect(this);
                } else {
                    Common.isConnecting = true;
                    Common.client.connect(PrefUtils.getInstance(this).getString(Constant.PREF_ACCESS_TOKEN, ""));
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Common.lastTime = System.currentTimeMillis();
        Common.isVisible = false;
    }

    public void showProgressDialog(int id) {
        String text = getString(id);
        showProgressDialog(text);
    }

    public void showProgressDialog(String str) {
        try {
            progressDialogFragment = ProgressDialogFragment.newInstance(null, str);
            progressDialogFragment.show(getSupportFragmentManager(), getString(R.string.app_name));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void dismissProgressDialog() {
        try {
            if (progressDialogFragment != null)
                progressDialogFragment.dismiss();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
    }

    @Override
    public void doWork(Object... params) {
    }

    @Override
    public void end(Object[] params) {
    }

    @Override
    public void onClick(View view) {

    }
}
