package com.stringee.apptoappcallsample.common;

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
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.stringee.apptoappcallsample.R;
import com.stringee.apptoappcallsample.activity.CallActivity;
import com.stringee.apptoappcallsample.service.MediaDismissReceiver;
import com.stringee.apptoappcallsample.service.RejectCallReceiver;

public class NotificationUtils {
    private static volatile NotificationUtils instance;
    private final Context context;
    private final NotificationManager nm;

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

    public void cancelNotification(int notificationId) {
        if (nm != null) {
            nm.cancel(notificationId);
        }
    }

    private String createCallNotificationChannel() {
        int channelIndex = PrefUtils.getInstance(context).getInt(Constant.PREF_INCOMING_CALL_CHANNEL_ID_INDEX, 0);
        String channelId = Constant.INCOMING_CALL_CHANNEL_ID + channelIndex;
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            //delete old channel
            NotificationChannel channel = nm.getNotificationChannel(channelId);
            if (channel != null && !(channel.getImportance() == NotificationManager.IMPORTANCE_MAX || channel.getImportance() == NotificationManager.IMPORTANCE_HIGH)) {
                nm.deleteNotificationChannel(channelId);
                channelIndex = channelIndex + 1;
                channelId = Constant.INCOMING_CALL_CHANNEL_ID + channelIndex;
            }
            //create new channel
            channel = new NotificationChannel(channelId, Constant.INCOMING_CALL_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            PrefUtils.getInstance(context).putInt(Constant.PREF_INCOMING_CALL_CHANNEL_ID_INDEX, channelIndex);
            channel.setDescription(Constant.INCOMING_CALL_CHANNEL_DESC);
            channel.setSound(null, null);
            nm.createNotificationChannel(channel);
        }
        return channelId;
    }

    public void showIncomingCallNotification(String from, boolean isStringeeCall, boolean isVideoCall) {
        String channelId = createCallNotificationChannel();

        int flag = PendingIntent.FLAG_UPDATE_CURRENT;
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            flag = PendingIntent.FLAG_IMMUTABLE;
        }

        Intent fullScreenIntent = new Intent(context, CallActivity.class);
        fullScreenIntent.putExtra(Constant.PARAM_IS_VIDEO_CALL, isVideoCall);
        fullScreenIntent.putExtra(Constant.PARAM_IS_INCOMING_CALL, true);
        fullScreenIntent.putExtra(Constant.PARAM_IS_STRINGEE_CALL, isStringeeCall);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff), fullScreenIntent, flag);

        Intent actionAnswerIntent = new Intent(context, CallActivity.class);
        actionAnswerIntent.putExtra(Constant.PARAM_IS_VIDEO_CALL, isVideoCall);
        actionAnswerIntent.putExtra(Constant.PARAM_IS_INCOMING_CALL, true);
        actionAnswerIntent.putExtra(Constant.PARAM_IS_STRINGEE_CALL, isStringeeCall);
        actionAnswerIntent.putExtra(Constant.PARAM_ACTION_ANSWER_FROM_PUSH, true);
        PendingIntent actionAnswerPendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff), actionAnswerIntent, flag);

        Intent actionRejectIntent = new Intent(context, RejectCallReceiver.class);
        actionRejectIntent.putExtra(Constant.PARAM_IS_VIDEO_CALL, isVideoCall);
        actionRejectIntent.putExtra(Constant.PARAM_IS_INCOMING_CALL, true);
        actionRejectIntent.putExtra(Constant.PARAM_IS_STRINGEE_CALL, isStringeeCall);
        PendingIntent actionRejectPendingIntent = PendingIntent.getBroadcast(context, (int) (System.currentTimeMillis() & 0xfffffff), actionRejectIntent, flag);

        Person.Builder personBuilder = new Person.Builder();
        personBuilder.setName(from);
        personBuilder.setImportant(true);
        personBuilder.setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher));
        Person person = personBuilder.build();

        Builder notificationBuilder = new Builder(context, channelId);
        notificationBuilder.setStyle(
                NotificationCompat.CallStyle.forIncomingCall(person, actionRejectPendingIntent, actionAnswerPendingIntent)
                        .setIsVideo(isVideoCall));
        notificationBuilder.addPerson(person);
        notificationBuilder.setContentText("Incoming call from: " + from);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notificationBuilder.setOngoing(true);
        notificationBuilder.setAutoCancel(false);
        notificationBuilder.setVibrate(new long[0]);
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
        notificationBuilder.setCategory(NotificationCompat.CATEGORY_CALL);
        if (canUseFullScreenIntent()) {
            notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true);
        }
        notificationBuilder.setShowWhen(false);

        Notification incomingCallNotification = notificationBuilder.build();

        AudioManagerUtils.getInstance(context).startRingtoneAndVibration();

        nm.notify(Constant.INCOMING_CALL_ID, incomingCallNotification);
    }

    private void createMediaServiceChannel() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Constant.MEDIA_CHANNEL_ID, Constant.MEDIA_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(Constant.MEDIA_CHANNEL_DESC);
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

        Intent intent = new Intent(context, CallActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff), intent, flag);

        Intent dismissIntent = new Intent(context, MediaDismissReceiver.class);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(context, 0, dismissIntent, flag);

        Builder builder = new Builder(context, Constant.MEDIA_CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.icon);
        builder.setSound(null);
        builder.setPriority(NotificationCompat.PRIORITY_DEFAULT);
        builder.setContentTitle("Capturing screen");
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        builder.setDeleteIntent(dismissPendingIntent);
        return builder.build();
    }

    private boolean canUseFullScreenIntent() {
        if (VERSION.SDK_INT >= 34) {
            return nm != null && nm.canUseFullScreenIntent();
        }
        return true;
    }
}
