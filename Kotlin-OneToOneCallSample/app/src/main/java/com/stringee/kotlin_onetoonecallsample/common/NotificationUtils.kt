package com.stringee.kotlin_onetoonecallsample.common

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.core.app.NotificationCompat
import com.stringee.kotlin_onetoonecallsample.R
import com.stringee.kotlin_onetoonecallsample.activity.CallActivity
import com.stringee.kotlin_onetoonecallsample.service.RejectCallReceiver


class NotificationUtils private constructor(private val applicationContext: Context) {
    private var nm: NotificationManager = if (VERSION.SDK_INT >= VERSION_CODES.O) {
        applicationContext.getSystemService(NotificationManager::class.java)
    } else {
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun cancelNotification(notificationId: Int) {
        nm.cancel(notificationId)
    }

    private fun createCallNotificationChannel(): String {
        var channelIndex = PrefUtils.getInstance(applicationContext)
            .getInt(Constant.PREF_INCOMING_CALL_CHANNEL_ID_INDEX, 0)
        var channelId = Constant.INCOMING_CALL_CHANNEL_ID + channelIndex
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            //delete old channel
            var channel = nm.getNotificationChannel(channelId)
            if (channel != null && !(channel.importance == NotificationManager.IMPORTANCE_MAX || channel.importance == NotificationManager.IMPORTANCE_HIGH)) {
                nm.deleteNotificationChannel(channelId)
                channelIndex += 1
                channelId = Constant.INCOMING_CALL_CHANNEL_ID + channelIndex
            }
            //create new channel
            channel = NotificationChannel(
                channelId,
                Constant.INCOMING_CALL_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            PrefUtils.getInstance(applicationContext)
                .putInt(Constant.PREF_INCOMING_CALL_CHANNEL_ID_INDEX, channelIndex)
            channel.description = Constant.INCOMING_CALL_CHANNEL_DESC
            channel.setSound(null, null)
            nm.createNotificationChannel(channel)
        }
        return channelId
    }

    fun showIncomingCallNotification(from: String, isStringeeCall: Boolean, isVideoCall: Boolean) {
        val channelId = createCallNotificationChannel()
        var flag = PendingIntent.FLAG_UPDATE_CURRENT
        if (VERSION.SDK_INT >= VERSION_CODES.S) {
            flag = PendingIntent.FLAG_IMMUTABLE
        }
        val fullScreenIntent = Intent(applicationContext, CallActivity::class.java)
        fullScreenIntent.putExtra(Constant.PARAM_IS_VIDEO_CALL, isVideoCall)
        fullScreenIntent.putExtra(Constant.PARAM_IS_INCOMING_CALL, true)
        fullScreenIntent.putExtra(Constant.PARAM_IS_STRINGEE_CALL, isStringeeCall)
        val fullScreenPendingIntent = PendingIntent.getActivity(
            applicationContext,
            (System.currentTimeMillis() and 0xfffffffL).toInt(),
            fullScreenIntent,
            flag
        )
        val actionAnswerIntent = Intent(applicationContext, CallActivity::class.java)
        actionAnswerIntent.putExtra(Constant.PARAM_IS_VIDEO_CALL, isVideoCall)
        actionAnswerIntent.putExtra(Constant.PARAM_IS_INCOMING_CALL, true)
        actionAnswerIntent.putExtra(Constant.PARAM_IS_STRINGEE_CALL, isStringeeCall)
        actionAnswerIntent.putExtra(Constant.PARAM_ACTION_ANSWER_FROM_PUSH, true)
        val actionAnswerPendingIntent = PendingIntent.getActivity(
            applicationContext,
            (System.currentTimeMillis() and 0xfffffffL).toInt(),
            actionAnswerIntent,
            flag
        )
        val actionRejectIntent = Intent(applicationContext, RejectCallReceiver::class.java)
        actionRejectIntent.putExtra(Constant.PARAM_IS_VIDEO_CALL, isVideoCall)
        actionRejectIntent.putExtra(Constant.PARAM_IS_INCOMING_CALL, true)
        actionRejectIntent.putExtra(Constant.PARAM_IS_STRINGEE_CALL, isStringeeCall)
        val actionRejectPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            (System.currentTimeMillis() and 0xfffffffL).toInt(),
            actionRejectIntent,
            flag
        )
        val answerTitle = SpannableString("Answer")
        answerTitle.setSpan(
            ForegroundColorSpan(Color.parseColor("#57D24D")),
            0,
            answerTitle.length,
            0
        )
        val endTitle: Spannable = SpannableString("Reject")
        endTitle.setSpan(ForegroundColorSpan(Color.parseColor("#F64D64")), 0, endTitle.length, 0)
        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
        notificationBuilder.setContentTitle("Incoming call")
        notificationBuilder.setContentText("incoming call from: $from")
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher)
        notificationBuilder.setOngoing(true)
        notificationBuilder.setAutoCancel(false)
        notificationBuilder.setVibrate(LongArray(0))
        notificationBuilder.setPriority(NotificationCompat.PRIORITY_HIGH)
        notificationBuilder.setCategory(NotificationCompat.CATEGORY_CALL)
        notificationBuilder.addAction(
            R.drawable.ic_answer_call,
            answerTitle,
            actionAnswerPendingIntent
        )
        notificationBuilder.addAction(R.drawable.ic_end_call, endTitle, actionRejectPendingIntent)
        notificationBuilder.setFullScreenIntent(fullScreenPendingIntent, true)
        notificationBuilder.setShowWhen(false)
        val incomingCallNotification = notificationBuilder.build()
        AudioManagerUtils.getInstance(applicationContext).startRingtoneAndVibration()
        nm.notify(Constant.INCOMING_CALL_ID, incomingCallNotification)
    }

    companion object {
        @Volatile
        private var instance: NotificationUtils? = null

        @Synchronized
        fun getInstance(context: Context): NotificationUtils {
            return instance ?: synchronized(this) {
                instance
                    ?: NotificationUtils(context.applicationContext).also {
                        instance = it
                    }
            }
        }
    }
}

