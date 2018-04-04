package com.stringee.softphone.common;

/**
 * Created by luannguyen on 7/18/2017.
 */

public class Constant {

    public static final String URL_BASE = "https://v1.stringee.com/softphone-apis/public_html";
    public static final String URL_LOGIN = "/account/login";
    public static final String URL_CONFIRM = "/account/confirm";
    public static final String URL_GET_ACCESS_TOKEN = "/account/getaccesstoken";
    public static final String URL_CHECK_PHONEBOOK = "/account/checkphonebookexisted";

    public static final String PREF_BASE = "com.stringee.softphone";
    public static final String PREF_LOGINED = PREF_BASE + ".logined";
    public static final String PREF_USER_ID = PREF_BASE + ".user_id";
    public static final String PREF_TOKEN = PREF_BASE + ".token";
    public static final String PREF_ACCESS_TOKEN = PREF_BASE + ".access_token";
    public static final String PREF_PASSWORD = PREF_BASE + ".password";
    public static final String PREF_TOKEN_REGISTERED = PREF_BASE + ".token_new.registered";
    public static final String PREF_FIREBASE_TOKEN = PREF_BASE + ".firebase.token";
    public static final String PREF_STATUS = PREF_BASE + ".status";
    public static final String PREF_SELECTED_NUMBER = PREF_BASE + ".number_selected";
    public static final String PREF_EXPIRED_TIME = PREF_BASE + ".expired_time";
    public static final String PREF_SIP_NUMBERS = PREF_BASE + ".sip_numbers";

    public static final String PARAM_CONTACT = "contact";
    public static final String PARAM_PHONE = "phone";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_URL = "url";
    public static final String PARAM_CALLOUT = "callout";
    public static final String PARAM_FROM_PUSH = "from_push";
    public static final String PARAM_PHONE_NO = "phone_no";
    public static final String PARAM_VIDEO_CALL = "video_call";
    public static final String PARAM_CALL_ID = "call_id";

    public static final int CHAT_TYPE_PRIVATE = 1;

    public static final int TYPE_MESSAGE_UNKNOWN = 1000;
    public static final int TYPE_MESSAGE_SENT = 1;
    public static final int TYPE_MESSAGE_SENT_NOT_LAST = 24;
    public static final int TYPE_MESSAGE_SENT_L = 18;
    public static final int TYPE_MESSAGE_INCOME = 2;
    public static final int TYPE_MESSAGE_INCOME_L = 19;
    public static final int TYPE_PICTURE_SENT = 3;
    public static final int TYPE_PICTURE_INCOME = 4;
    public static final int TYPE_LOCATION_SENT = 5;
    public static final int TYPE_LOCATION_INCOME = 6;
    public static final int TYPE_AUDIO_SENT = 7;
    public static final int TYPE_AUDIO_INCOME = 8;
    public static final int TYPE_STICKER_SENT = 9;
    public static final int TYPE_STICKER_INCOME = 10;
    public static final int TYPE_PICTURE_URL = 11;
    public static final int TYPE_AUDIO_URL = 12;
    public static final int TYPE_NOTIFICATION = 13;
    public static final int TYPE_FILE_SENT = 14;
    public static final int TYPE_FILE_INCOME = 15;
    public static final int TYPE_INCOMING_CALL = 16;
    public static final int TYPE_OUTGOING_CALL = 17;
    public static final int TYPE_CALL_OUT = 20;
    public static final int TYPE_READ_UNREAD = 21;
    public static final int TYPE_VIDEO_SEND = 22;
    public static final int TYPE_VIDEO_INCOME = 23;
    public static final int TYPE_CALL_PHONE_TO_APP = 24;
    public static final int TYPE_MISSED_CALL = 25;
    public static final int TYPE_TIME_HEADER = 0;

    public static final String DATETIME_FORMAT = "yyyyMMddHHmmss";
    public static final int MESSAGE_READ = 1;
    public static final int MESSAGE_UNREAD = 0;

    public static final int MESSAGE_SENDING = 1;
    public static final int MESSAGE_SENT = 2;
    public static final int MESSAGE_DELIVERED = 3;
    public static final int MESSAGE_NOT_SHOW = 4;
    public static final int MESSAGE_SCHEDULE = 5;
    public static final int MESSAGE_CALL_MISSED = 7;
    public static final int MESSAGE_CALL = 8;

    public static final int CON_MSG_SEND = 1;
    public static final int CON_MSG_RECEIVE = 2;

    public static final int TYPE_CONTACT_HEADER = -1;
}
