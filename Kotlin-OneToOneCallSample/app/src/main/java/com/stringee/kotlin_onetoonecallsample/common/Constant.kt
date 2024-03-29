package com.stringee.kotlin_onetoonecallsample.common


object Constant {
    const val TAG = "Stringee"
    const val PREF_BASE = "com.stringee.one_to_one_call_sample"
    const val PREF_INCOMING_CALL_CHANNEL_ID_INDEX = "$PREF_BASE.incomingcall.channel_id"
    const val INCOMING_CALL_CHANNEL_ID = "com.stringee.one_to_one_call_sample.incoming.call.notification."
    const val INCOMING_CALL_CHANNEL_NAME = "Incoming Call Notification Channel"
    const val INCOMING_CALL_CHANNEL_DESC = "Channel for incoming call notification"
    const val MEDIA_CHANNEL_ID = "com.stringee.one_to_one_call_sample.media"
    const val MEDIA_CHANNEL_NAME = "Media Projection Notification Channel"
    const val MEDIA_CHANNEL_DESC = "Channel for media projection notification"

    const val INCOMING_CALL_ID = 28011996
    const val MEDIA_SERVICE_ID = 14101997

    const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
    private const val PARAM_BASE = "com.stringee."
    const val PARAM_IS_VIDEO_CALL = PARAM_BASE + "is_video_call"
    const val PARAM_TO = PARAM_BASE + "to"
    const val PARAM_IS_INCOMING_CALL = PARAM_BASE + "is_incoming_call"
    const val PARAM_IS_STRINGEE_CALL = PARAM_BASE + "is_stringee_call"
    const val PARAM_ACTION_ANSWER_FROM_PUSH = PARAM_BASE + "action_answer_from_push"
}
