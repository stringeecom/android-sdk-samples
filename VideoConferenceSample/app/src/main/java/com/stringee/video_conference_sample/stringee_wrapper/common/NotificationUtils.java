package com.stringee.video_conference_sample.stringee_wrapper.common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Builder;

import com.stringee.video_conference_sample.R;
import com.stringee.video_conference_sample.stringee_wrapper.ui.activity.ConferenceActivity;

public class NotificationUtils {
    private static volatile NotificationUtils instance;
    private final Context context;
    private final NotificationManager nm;
    private static final String MEDIA_CHANNEL_ID = "com.stringee.one_to_one_call_sample.media";
    private static final String MEDIA_CHANNEL_NAME = "Media Projection Notification Channel";
    private static final String MEDIA_CHANNEL_DESC = "Channel for media projection notification";
    public static final int MEDIA_SERVICE_ID = 14101997;

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

    public NotificationUtils(Context context) {
        this.context = context.getApplicationContext();
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            nm = context.getSystemService(NotificationManager.class);
        } else {
            nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }
    }

    public static synchronized NotificationUtils getInstance(Context context) {
        if (instance == null) {
            synchronized (NotificationUtils.class) {
                if (instance == null) {
                    instance = new NotificationUtils(context);
                }
            }
        }
        return instance;
    }

    private void createMediaServiceChannel() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(MEDIA_CHANNEL_ID, MEDIA_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(MEDIA_CHANNEL_DESC);
            channel.setSound(null, null);
            nm.createNotificationChannel(channel);
        }
    }

    public Notification createMediaNotification() {
        createMediaServiceChannel();

        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            flag = PendingIntent.FLAG_IMMUTABLE;
        }

        Intent intent = new Intent(context, ConferenceActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff), intent, flag);

        Builder builder = new Builder(context, MEDIA_CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setSound(null);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentTitle("Capturing screen");
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        return builder.build();
    }
}
