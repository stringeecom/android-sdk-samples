package com.stringee.callpushnotificationsample.common;

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

import com.stringee.callpushnotificationsample.MainActivity;
import com.stringee.callpushnotificationsample.R.mipmap;

public class NotificationUtils {
    private static volatile NotificationUtils instance;
    private static final Object lock = new Object();
    private final Context context;
    private final NotificationManager nm;

    private static final String INCOMING_CALL_CHANNEL_ID = "com.stringee.stringeeapp.incoming.call.notification.";
    private static final String INCOMING_CALL_CHANNEL_NAME = "Stringee App Incoming Call Notification Channel";
    private static final String INCOMING_CALL_CHANNEL_DESC = "Channel for Stringee App incoming call notification";

    public static final int INCOMING_CALL_ID = 28011996;

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
            synchronized (lock) {
                if (instance == null) {
                    instance = new NotificationUtils(context);
                }
            }
        }
        return instance;
    }

    public void cancelNotification(int notificationId) {
        if (nm != null) {
            nm.cancel(notificationId);
        }
    }

    private String createCallNotificationChannel() {
        int channelIndex = PrefUtils.getInstance(context).getInt(Constant.PREF_INCOMING_CALL_CHANNEL_ID_INDEX, 0);
        String channelId = INCOMING_CALL_CHANNEL_ID + channelIndex;
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            //delete old channel
            NotificationChannel channel = nm.getNotificationChannel(channelId);
            if (channel != null && !(channel.getImportance() == NotificationManager.IMPORTANCE_MAX || channel.getImportance() == NotificationManager.IMPORTANCE_HIGH)) {
                nm.deleteNotificationChannel(channelId);
                channelIndex = channelIndex + 1;
                channelId = INCOMING_CALL_CHANNEL_ID + channelIndex;
            }
            //create new channel
            channel = new NotificationChannel(channelId, INCOMING_CALL_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            PrefUtils.getInstance(context).putInt(Constant.PREF_INCOMING_CALL_CHANNEL_ID_INDEX, channelIndex);
            channel.setDescription(INCOMING_CALL_CHANNEL_DESC);
            channel.setSound(null, null);
            nm.createNotificationChannel(channel);
        }
        return channelId;
    }

    public void createIncomingCallNotification(String from) {
        String channelId = createCallNotificationChannel();

        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            flag = PendingIntent.FLAG_IMMUTABLE;
        }

        Intent fullScreenIntent = new Intent(context, MainActivity.class);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff), fullScreenIntent, flag);

        Builder notificationBuilder = new Builder(context, channelId)
                .setContentTitle("Incoming call")
                .setContentText("incoming call from: " + from)
                .setSmallIcon(mipmap.ic_launcher)
                .setOngoing(true)
                .setVibrate(new long[0])
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        notificationBuilder.setShowWhen(false);

        Notification incomingCallNotification = notificationBuilder.build();

        RingtoneUtils.getInstance(context).startRingtoneAndVibration();

        nm.notify(INCOMING_CALL_ID, incomingCallNotification);
    }
}
