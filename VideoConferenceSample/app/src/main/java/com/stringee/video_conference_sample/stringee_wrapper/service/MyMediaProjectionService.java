package com.stringee.video_conference_sample.stringee_wrapper.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

import com.stringee.video_conference_sample.stringee_wrapper.common.NotificationUtils;
import com.stringee.video_conference_sample.stringee_wrapper.common.Utils;
import com.stringee.video_conference_sample.stringee_wrapper.wrapper.ConferenceWrapper;
import com.stringee.video_conference_sample.stringee_wrapper.wrapper.StringeeWrapper;

public class MyMediaProjectionService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (!Utils.isStringEmpty(action)) {
                if (action.equals(NotificationUtils.ACTION_START_FOREGROUND_SERVICE)) {
                    Notification notification = NotificationUtils.getInstance(this).createMediaNotification();
                    int type = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
                    }
                    try {
                        ServiceCompat.startForeground(this, NotificationUtils.MEDIA_SERVICE_ID, notification, type);
                    } catch (Exception e) {
                        Utils.reportException(MyMediaProjectionService.class, e);
                    }
                    ConferenceWrapper conferenceWrapper = StringeeWrapper.getInstance(this).getConferenceWrapper();
                    if (conferenceWrapper!= null) {
                        conferenceWrapper.startCapture(this, intent);
                    }else {
                        stopService();
                    }
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
