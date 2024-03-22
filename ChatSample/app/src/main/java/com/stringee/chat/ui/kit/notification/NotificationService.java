package com.stringee.chat.ui.kit.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.stringee.chat.ui.kit.activity.ConversationActivity;
import com.stringee.stringeechatuikit.common.Utils;

public class NotificationService {

    private static final String CHANNEL_ID = "com.stringee.message.notification";
    private static final String CHANNEL_NAME = "Stringee Notification Channel";
    private static final String CHANNEL_DESC = "Channel for notification";

    public static void showNotification(Context context, String convId, String convName, String senderName, boolean isGroup, String text) {
        String title = null;
        String notificationText;
        Bitmap notificationIconBitmap = null;
        int notificationId = 0;
        int iconResourceId;

        title = convName;
        if (title == null || title.length() == 0) {
            title = senderName;
        }

        if (isGroup) {
            iconResourceId = context.getResources().getIdentifier("group_icon", "drawable", context.getPackageName());
        } else {
            iconResourceId = context.getResources().getIdentifier("stringee_ic_contact_picture", "drawable", context.getPackageName());
        }

        notificationText = text;
        notificationIconBitmap = BitmapFactory.decodeResource(context.getResources(), iconResourceId);

        NotificationManager mNotificationManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_MAX);
            channel.setDescription(CHANNEL_DESC);
            mNotificationManager = context.getSystemService(NotificationManager.class);
            mNotificationManager.createNotificationChannel(channel);
        } else {
            mNotificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
        }

        Intent intent = new Intent(context, ConversationActivity.class);
        intent.putExtra("convId", convId);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff),
                intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, CHANNEL_ID);

        mBuilder.setSmallIcon(Utils.getLauncherIcon(context))
                .setLargeIcon(notificationIconBitmap)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationManager.IMPORTANCE_MAX)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(notificationText).setDefaults(Notification.DEFAULT_ALL);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setAutoCancel(true);

        mNotificationManager.notify(notificationId, mBuilder.build());
    }

    public static void cancelNotification(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
    }
}
