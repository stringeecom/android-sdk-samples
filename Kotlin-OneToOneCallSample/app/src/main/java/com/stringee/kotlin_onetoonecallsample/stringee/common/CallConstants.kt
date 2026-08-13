package com.stringee.kotlin_onetoonecallsample.stringee.common

/** Shared intent actions, extras, notification IDs, and timeout values for the call flow. */
object CallConstants {
    const val TAG: String = "StringeeSample"

    const val ACTION_INCOMING_PUSH: String = "com.stringee.onetoonecallsample.action.INCOMING_PUSH"
    const val ACTION_INCOMING_SDK: String = "com.stringee.onetoonecallsample.action.INCOMING_SDK"
    const val ACTION_HIDE_INCOMING: String = "com.stringee.onetoonecallsample.action.HIDE_INCOMING"
    const val ACTION_START_IN_CALL: String = "com.stringee.onetoonecallsample.action.START_IN_CALL"
    const val ACTION_REJECT: String = "com.stringee.onetoonecallsample.action.REJECT"
    const val ACTION_HANG_UP: String = "com.stringee.onetoonecallsample.action.HANG_UP"
    const val ACTION_MEDIA_DISMISSED: String =
        "com.stringee.onetoonecallsample.action.MEDIA_DISMISSED"

    const val EXTRA_CALL_ID: String = "stringee_call_id"
    const val EXTRA_CALL_GENERATION: String = "stringee_call_generation"
    const val EXTRA_SESSION_GENERATION: String = "stringee_session_generation"
    const val EXTRA_PENDING_ACTION: String = "stringee_pending_action"
    const val EXTRA_CALLER: String = "stringee_caller"
    const val EXTRA_CALLER_ALIAS: String = "stringee_caller_alias"
    const val EXTRA_VIDEO: String = "stringee_video"
    const val EXTRA_ENGINE: String = "stringee_engine"
    const val EXTRA_ANSWER: String = "stringee_answer"
    const val EXTRA_STARTED_AT: String = "stringee_started_at"

    const val INCOMING_NOTIFICATION_ID: Int = 28011996
    const val ONGOING_NOTIFICATION_ID: Int = 28011997
    const val MEDIA_NOTIFICATION_ID: Int = 14101997
}
