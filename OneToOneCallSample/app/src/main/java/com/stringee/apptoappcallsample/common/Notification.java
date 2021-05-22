package com.stringee.apptoappcallsample.common;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.core.app.NotificationCompat;

import com.stringee.apptoappcallsample.MainActivity;
import com.stringee.apptoappcallsample.R;

import java.util.Set;

public class Notification {
    //replace your channel id, channel name, channel desc here
    private static final String CALL_CHANNEL_ID = "YOUR_CHANNEL_ID";
    private static final String CALL_CHANNEL_NAME = "YOUR_CHANNEL_NAME";
    private static final String CALL_CHANNEL_DESC = "YOUR_CHANNEL_DESCRIPTION";
    public static final int INCOMING_CALL_NOTIFICATION_ID = 19032021;

    public static void notifyIncomingCall(Context context, String from) {
        //set up channel
        NotificationManager nm;
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            nm = (NotificationManager) context.getSystemService(NotificationManager.class);
            NotificationChannel channel = nm.getNotificationChannel(CALL_CHANNEL_ID);
            //recreate channel
            if (channel != null && channel.getImportance() < NotificationManager.IMPORTANCE_HIGH) {
                nm.deleteNotificationChannel(CALL_CHANNEL_ID);
            }
            channel = new NotificationChannel(CALL_CHANNEL_ID, CALL_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CALL_CHANNEL_DESC);
            channel.enableVibration(false);
            channel.enableLights(false);
            channel.setBypassDnd(true);
            nm.createNotificationChannel(channel);
        } else {
            nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

        //set up for showing screen in lock screen
        Intent fullScreenIntent = new Intent(context, MainActivity.class);
        fullScreenIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(context, (int) (System.currentTimeMillis() & 0xfffffff),
                fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //create notification
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CALL_CHANNEL_ID)
                        .setSmallIcon(R.mipmap.icon)
                        .setSound(null)
                        .setOngoing(true)
                        .setContentTitle("IncomingCall")
                        .setContentText("from: " + from)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setCategory(NotificationCompat.CATEGORY_CALL)
                        .setFullScreenIntent(fullScreenPendingIntent, true);

        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
            notificationBuilder.setShowWhen(false);
        }

        android.app.Notification incomingCallNotification = notificationBuilder.build();

        nm.notify(INCOMING_CALL_NOTIFICATION_ID, incomingCallNotification);

        Utils.runOnUithread(new Runnable() {
            @Override
            public void run() {
                if (Common.audioManager == null) {
                    Common.audioManager = StringeeAudioManager.create(context);
                    Common.audioManager.start(new StringeeAudioManager.AudioManagerEvents() {
                        @Override
                        public void onAudioDeviceChanged(StringeeAudioManager.AudioDevice selectedAudioDevice, Set<StringeeAudioManager.AudioDevice> availableAudioDevices) {
                        }
                    });
                    Common.audioManager.setMode(AudioManager.MODE_RINGTONE);
                    Common.audioManager.setSpeakerphoneOn(true);
                }
                //play ringtone
                if (Common.ringtone == null) {
                    Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                    Common.ringtone = RingtoneManager.getRingtone(context, ringtoneUri);
                    Common.ringtone.play();
                }
            }
        });
    }
}
