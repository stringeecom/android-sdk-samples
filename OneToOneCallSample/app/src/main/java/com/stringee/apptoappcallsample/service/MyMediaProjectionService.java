package com.stringee.apptoappcallsample.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.stringee.apptoappcallsample.common.Constant;
import com.stringee.apptoappcallsample.common.Utils;
import com.stringee.apptoappcallsample.manager.CallManager;

public class MyMediaProjectionService extends Service {
    private CallManager callManager;

    @Override
    public void onCreate() {
        super.onCreate();
        callManager = CallManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (!Utils.isStringEmpty(action)) {
                if (action.equals(Constant.ACTION_START_FOREGROUND_SERVICE)) {
                    callManager.startCapture(this);
                }
            }
        }
        return START_NOT_STICKY;
    }

    public void stopService() {
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
