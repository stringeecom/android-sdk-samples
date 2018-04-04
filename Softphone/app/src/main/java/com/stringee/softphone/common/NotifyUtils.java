package com.stringee.softphone.common;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.stringee.softphone.R;
import com.stringee.softphone.activity.MainActivity;
import com.stringee.softphone.model.Message;

/**
 * Created by luannguyen on 7/27/2017.
 */

public class NotifyUtils {

    public static void notifyUpdateRecents() {
        Intent intent = new Intent(Notify.UPDATE_RECENTS.getValue());
        LocalBroadcastManager.getInstance(Common.context).sendBroadcast(intent);
    }

    public static void showNotification(Context context, Message imMessage) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(Common.context);
        builder.setSmallIcon(R.mipmap.icon);
//        builder.setLargeIcon(BitmapFactory.decodeResource(Common.context.getResources(), R.mipmap.icon));
        builder.setContentTitle(context.getString(R.string.missed_call));
        if (imMessage.getFullname() != null) {
            builder.setContentText(imMessage.getFullname());
        } else {
            builder.setContentText(imMessage.getPhoneNumber());
        }
        builder.setAutoCancel(true);

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(Common.context, imMessage.getId(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) Common.context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        android.app.Notification notification = builder.build();
        notification.flags |= android.app.Notification.FLAG_AUTO_CANCEL;
        mNotificationManager.notify(imMessage.getId(), notification);
    }

    public static void notifyEndCall() {
        Intent intent = new Intent(Notify.END_CALL.getValue());
        LocalBroadcastManager.getInstance(Common.context).sendBroadcast(intent);
    }

    public static void showCallNotify(Context context, String name, Intent intent, int id) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(Common.context);
        builder.setSmallIcon(R.mipmap.icon);
        builder.setContentTitle(context.getString(R.string.ongoing_call));
        builder.setContentText(name);

        PendingIntent pendingIntent = PendingIntent.getActivity(Common.context, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        NotificationManager mNotificationManager = (NotificationManager) Common.context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        android.app.Notification notification = builder.build();
        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(id, notification);
    }
}
