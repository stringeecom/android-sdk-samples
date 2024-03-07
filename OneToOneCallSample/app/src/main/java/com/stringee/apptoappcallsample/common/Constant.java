package com.stringee.apptoappcallsample.common;

public class Constant {
    public static String TAG = "Stringee";
    public static final String PREF_BASE = "com.stringee.one_to_one_call_sample";
    public static final String PREF_INCOMING_CALL_CHANNEL_ID_INDEX = PREF_BASE + ".incomingcall.channel_id";

    public static final String INCOMING_CALL_CHANNEL_ID = "com.stringee.one_to_one_call_sample.incoming.call.notification.";
    public static final String INCOMING_CALL_CHANNEL_NAME = "Incoming Call Notification Channel";
    public static final String INCOMING_CALL_CHANNEL_DESC = "Channel for incoming call notification";
    public static final String MEDIA_CHANNEL_ID = "com.stringee.one_to_one_call_sample.media";
    public static final String MEDIA_CHANNEL_NAME = "Media Projection Notification Channel";
    public static final String MEDIA_CHANNEL_DESC = "Channel for media projection notification";

    public static final int INCOMING_CALL_ID = 28011996;
    public static final int MEDIA_SERVICE_ID = 14101997;

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

    public static final String PARAM_BASE = "com.stringee.";
    public static final String PARAM_IS_VIDEO_CALL = PARAM_BASE + "is_video_call";
    public static final String PARAM_TO = PARAM_BASE + "to";
    public static final String PARAM_IS_INCOMING_CALL = PARAM_BASE + "is_incoming_call";
    public static final String PARAM_IS_STRINGEE_CALL = PARAM_BASE + "is_stringee_call";
    public static final String PARAM_ACTION_ANSWER_FROM_PUSH = PARAM_BASE + "action_answer_from_push";
}
