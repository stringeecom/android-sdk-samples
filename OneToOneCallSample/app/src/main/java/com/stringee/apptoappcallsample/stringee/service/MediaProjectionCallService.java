package com.stringee.apptoappcallsample.stringee.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

import com.stringee.apptoappcallsample.stringee.common.CallConstants;
import com.stringee.apptoappcallsample.stringee.common.CallNotificationManager;
import com.stringee.apptoappcallsample.stringee.common.CallUtils;
import com.stringee.apptoappcallsample.stringee.manager.CallSession;
import com.stringee.apptoappcallsample.stringee.manager.StringeeCallManager;

/** Foreground service that owns the MediaProjection lifecycle for Call2 screen sharing. */
public class MediaProjectionCallService extends Service {
    public static final String EXTRA_PERMISSION_DATA = "media_projection_permission_data";
    private boolean stoppingFromSession;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !CallConstants.ACTION_START_IN_CALL.equals(intent.getAction())) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        try {
            int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                    ? ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION : 0;
            ServiceCompat.startForeground(this, CallConstants.MEDIA_NOTIFICATION_ID,
                    CallNotificationManager.getInstance(this).buildMediaProjection(), type);
        } catch (RuntimeException exception) {
            CallUtils.reportException(MediaProjectionCallService.class, exception);
            stopSelf(startId);
            return START_NOT_STICKY;
        }
        Intent permissionData;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionData = intent.getParcelableExtra(EXTRA_PERMISSION_DATA, Intent.class);
        } else {
            permissionData = getLegacyPermissionData(intent);
        }
        CallSession session = StringeeCallManager.getInstance(this).getSession();
        if (session == null || permissionData == null) {
            stopService();
            return START_NOT_STICKY;
        }
        session.startScreenShare(this, permissionData);
        return START_NOT_STICKY;
    }

    @SuppressWarnings("deprecation")
    private Intent getLegacyPermissionData(Intent intent) {
        return intent.getParcelableExtra(EXTRA_PERMISSION_DATA);
    }

    public void stopService() {
        stoppingFromSession = true;
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        CallNotificationManager.getInstance(this).cancel(CallConstants.MEDIA_NOTIFICATION_ID);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        CallNotificationManager.getInstance(this).cancel(CallConstants.MEDIA_NOTIFICATION_ID);
        if (!stoppingFromSession) {
            CallSession session = StringeeCallManager.getInstance(this).getSession();
            if (session != null) {
                session.onMediaProjectionServiceDestroyed(this);
            }
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
