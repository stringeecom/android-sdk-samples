package com.stringee.apptoappcallsample.stringee.common;

/** Shared intent actions, extras, notification IDs, and timeout values for the call flow. */
public final class CallConstants {
    public static final String TAG = "StringeeSample";

    public static final String ACTION_INCOMING_PUSH =
            "com.stringee.onetoonecallsample.action.INCOMING_PUSH";
    public static final String ACTION_INCOMING_SDK =
            "com.stringee.onetoonecallsample.action.INCOMING_SDK";
    public static final String ACTION_HIDE_INCOMING =
            "com.stringee.onetoonecallsample.action.HIDE_INCOMING";
    public static final String ACTION_START_IN_CALL =
            "com.stringee.onetoonecallsample.action.START_IN_CALL";
    public static final String ACTION_REJECT =
            "com.stringee.onetoonecallsample.action.REJECT";
    public static final String ACTION_HANG_UP =
            "com.stringee.onetoonecallsample.action.HANG_UP";
    public static final String ACTION_MEDIA_DISMISSED =
            "com.stringee.onetoonecallsample.action.MEDIA_DISMISSED";

    public static final String EXTRA_CALL_ID = "stringee_call_id";
    public static final String EXTRA_CALL_GENERATION = "stringee_call_generation";
    public static final String EXTRA_SESSION_GENERATION = "stringee_session_generation";
    public static final String EXTRA_PENDING_ACTION = "stringee_pending_action";
    public static final String EXTRA_CALLER = "stringee_caller";
    public static final String EXTRA_CALLER_ALIAS = "stringee_caller_alias";
    public static final String EXTRA_VIDEO = "stringee_video";
    public static final String EXTRA_ENGINE = "stringee_engine";
    public static final String EXTRA_ANSWER = "stringee_answer";
    public static final String EXTRA_STARTED_AT = "stringee_started_at";

    public static final int INCOMING_NOTIFICATION_ID = 28011996;
    public static final int ONGOING_NOTIFICATION_ID = 28011997;
    public static final int MEDIA_NOTIFICATION_ID = 14101997;

    private CallConstants() {
    }
}
