package com.stringee.apptoappcallsample.stringee.common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;

import com.stringee.apptoappcallsample.R;
import com.stringee.apptoappcallsample.activity.MainActivity;
import com.stringee.apptoappcallsample.stringee.activity.CallActivity;
import com.stringee.apptoappcallsample.stringee.manager.ActivePushCall;
import com.stringee.apptoappcallsample.stringee.receiver.CallActionReceiver;

/** Builds and updates incoming, ongoing-call, and screen-sharing notifications. */
public final class CallNotificationManager {
    private static final String INCOMING_CHANNEL =
            "com.stringee.onetoonecallsample.incoming";
    private static final String ONGOING_CHANNEL =
            "com.stringee.onetoonecallsample.ongoing";
    private static final String MEDIA_CHANNEL =
            "com.stringee.onetoonecallsample.media";

    private static volatile CallNotificationManager instance;
    private final Context context;
    private final NotificationManager notificationManager;

    private CallNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        notificationManager = (NotificationManager) this.context.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    public static CallNotificationManager getInstance(Context context) {
        if (instance == null) {
            synchronized (CallNotificationManager.class) {
                if (instance == null) {
                    instance = new CallNotificationManager(context);
                }
            }
        }
        return instance;
    }

    public Notification buildIncomingFromPush(String from, String alias, boolean videoCall,
                                              String callId, long pushGeneration) {
        return buildIncoming(from, alias, videoCall, callId, pushGeneration, -1, false);
    }

    public Notification buildIncomingFromSdk(String from, String alias, boolean videoCall,
                                             String callId, long pushGeneration,
                                             long sessionGeneration) {
        return buildIncoming(from, alias, videoCall, callId, pushGeneration,
                sessionGeneration, true);
    }

    private Notification buildIncoming(String from, String alias, boolean videoCall,
                                       String callId, long pushGeneration,
                                       long sessionGeneration, boolean sdkReady) {
        createChannels();
        String displayName = CallUtils.isEmpty(alias) ? from : alias;
        Person person = new Person.Builder()
                .setName(displayName)
                .setImportant(true)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .build();

        PendingIntent content = sdkReady
                ? callActivityIntent(false, callId, pushGeneration, sessionGeneration, 2101)
                : mainActivityIntent(false, callId, pushGeneration, 2101);
        PendingIntent answer = sdkReady
                ? callActivityIntent(true, callId, pushGeneration, sessionGeneration, 2102)
                : mainActivityIntent(true, callId, pushGeneration, 2102);
        Intent rejectIntent = new Intent(context, CallActionReceiver.class)
                .setAction(CallConstants.ACTION_REJECT)
                .putExtra(CallConstants.EXTRA_CALL_ID, callId)
                .putExtra(CallConstants.EXTRA_CALL_GENERATION, pushGeneration)
                .putExtra(CallConstants.EXTRA_SESSION_GENERATION, sessionGeneration);
        if (!sdkReady) {
            rejectIntent.putExtra(CallConstants.EXTRA_PENDING_ACTION,
                    ActivePushCall.PendingAction.REJECT.name());
        }
        PendingIntent reject = PendingIntent.getBroadcast(context, 2103, rejectIntent,
                pendingIntentFlags());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, INCOMING_CHANNEL)
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(person, reject, answer)
                        .setIsVideo(videoCall))
                .addPerson(person)
                .setSmallIcon(R.mipmap.icon)
                .setContentTitle(displayName)
                .setContentText("Incoming call from " + from)
                .setContentIntent(content)
                .setOngoing(true)
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVibrate(new long[0])
                .setShowWhen(false);
        builder.setFullScreenIntent(content, true);
        return builder.build();
    }

    public Notification buildOngoing(String name, boolean videoCall, long startedAt,
                                     long generation) {
        createChannels();
        String displayName = CallUtils.isEmpty(name) ? "Stringee call" : name;
        Person person = new Person.Builder().setName(displayName).setImportant(true).build();
        Intent contentIntent = new Intent(context, CallActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(CallConstants.EXTRA_SESSION_GENERATION, generation);
        PendingIntent content = PendingIntent.getActivity(context, 2201, contentIntent,
                pendingIntentFlags());
        Intent hangUpIntent = new Intent(context, CallActionReceiver.class)
                .setAction(CallConstants.ACTION_HANG_UP)
                .putExtra(CallConstants.EXTRA_SESSION_GENERATION, generation);
        PendingIntent hangUp = PendingIntent.getBroadcast(context, 2202, hangUpIntent,
                pendingIntentFlags());
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, ONGOING_CHANNEL)
                .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangUp)
                        .setIsVideo(videoCall))
                .addPerson(person)
                .setSmallIcon(R.mipmap.icon)
                .setContentTitle(displayName)
                .setContentText("Ongoing call")
                .setContentIntent(content)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setUsesChronometer(startedAt > 0)
                .setShowWhen(startedAt > 0);
        if (startedAt > 0) {
            builder.setWhen(startedAt);
        }
        return builder.build();
    }

    public Notification buildMediaProjection() {
        createChannels();
        Intent contentIntent = new Intent(context, CallActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, 2301, contentIntent,
                pendingIntentFlags());
        return new NotificationCompat.Builder(context, MEDIA_CHANNEL)
                .setSmallIcon(R.mipmap.icon)
                .setContentTitle("Capturing screen")
                .setContentIntent(content)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setOngoing(true)
                .build();
    }

    public void cancel(int notificationId) {
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
        }
    }

    private PendingIntent mainActivityIntent(boolean answer, String callId, long generation,
                                             int requestCode) {
        Intent intent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(CallConstants.EXTRA_CALL_ID, callId)
                .putExtra(CallConstants.EXTRA_CALL_GENERATION, generation);
        if (answer) {
            intent.putExtra(CallConstants.EXTRA_PENDING_ACTION,
                    ActivePushCall.PendingAction.ANSWER.name());
        }
        return PendingIntent.getActivity(context, requestCode, intent, pendingIntentFlags());
    }

    private PendingIntent callActivityIntent(boolean answer, String callId, long pushGeneration,
                                             long sessionGeneration, int requestCode) {
        Intent intent = new Intent(context, CallActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(CallConstants.EXTRA_CALL_ID, callId)
                .putExtra(CallConstants.EXTRA_CALL_GENERATION, pushGeneration)
                .putExtra(CallConstants.EXTRA_SESSION_GENERATION, sessionGeneration)
                .putExtra(CallConstants.EXTRA_ANSWER, answer);
        return PendingIntent.getActivity(context, requestCode, intent, pendingIntentFlags());
    }

    private int pendingIntentFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || notificationManager == null) {
            return;
        }
        NotificationChannel incoming = new NotificationChannel(INCOMING_CHANNEL,
                "Incoming calls", NotificationManager.IMPORTANCE_HIGH);
        incoming.setSound(null, null);
        incoming.setDescription("Incoming Stringee calls");
        notificationManager.createNotificationChannel(incoming);

        NotificationChannel ongoing = new NotificationChannel(ONGOING_CHANNEL,
                "Ongoing calls", NotificationManager.IMPORTANCE_DEFAULT);
        ongoing.setSound(null, null);
        ongoing.setDescription("Ongoing Stringee calls");
        notificationManager.createNotificationChannel(ongoing);

        NotificationChannel media = new NotificationChannel(MEDIA_CHANNEL,
                "Screen sharing", NotificationManager.IMPORTANCE_DEFAULT);
        media.setSound(null, null);
        media.setDescription("Stringee screen sharing");
        notificationManager.createNotificationChannel(media);
    }
}
