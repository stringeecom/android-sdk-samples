package com.stringee.softphone.database;

public class MessageConstant {
    public static final String TABLE_NAME = "messages";
    public static final String ID = "id";
    public static final String MESSAGE_ID = "msg_id";
    public static final String CONVERSATION_ID = "conversation_id";
    public static final String FROM_USER_ID = "from_user_id";// userid or
    // groupid
    public static final String TO_USER_ID = "to_user_id";// userid or groupid
    public static final String ATTACHMENT = "attachment";
    public static final String DATETIME = "date_time";
    public static final String STATE_DATE = "state_date";
    public static final String TEXT = "content";
    public static final String STATE = "state";
    public static final String TYPE = "type";
    public static final String IS_READ = "is_read";
    public static final String PATH = "attach_path";
    public static final String SHORT_DATE = "short_date";
    public static final String LATITUDE = "latitude";//image thi luu size vao day
    public static final String LONGITUDE = "longitude";//image thi luu size vao day
    public static final String ADDRESS = "address";
    public static final String DURATION = "duration";
    public static final String STICKER_CATEGORY = "sticker_category";
    public static final String STICKER_NAME = "sticker_name";
    public static final String SCHEDULE_TIME = "schedule_time";
    public static final String SCHEDULE_TIME_TEXT = "schedule_time_text";

    // v3
    public static final String GROUP_SEND_ID = "group_send_id";// json
    public static final String GROUP_DILIVERY_USERS = "group_dilivery_users";// json
    public static final String SENDER_ID = "group_from_id";
    public static final String CHAT_TYPE = "chat_type";// default = chat private
    public static final String GROUP_NUM_DILIVERY = "group_num_dilivery";
    public static final String IS_DOWNLOAD_FILE = "is_download_file";
    public static final String FILE_SIZE = "file_size";
    public static final String TEXT_ASCII = "text_ascii";
    public static final String FORWARD_FROM_ID = "forward_from_id";
    public static final String FORWARD_FROM_NAME = "forward_from_name";
    public static final String FILE_URL = "file_url";
    public static final String FILE_THUMB_VIDEO = "file_thumb_video";
    public static final String PATH_THUMB_VIDEO = "path_thumb_video";
    public static final String BROADCAST_ID = "broadcast_id";
    public static final String PHONE_NO = "phone_no";

    // viewtype
    public static final String VIEW_TYPE = "view_type";

    // callout
    public static final String PHONE_NUMBER = "phone_number";
    public static final String FULL_NAME = "full_name";
    public static final String CREATED = "created";

    public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" + ID
            + " INTEGER PRIMARY KEY," + MESSAGE_ID + " INTEGER, " + CONVERSATION_ID + " INTEGER," + FROM_USER_ID
            + " INTEGER," + TO_USER_ID + " INTEGER," + ATTACHMENT + " INTEGER, " + DATETIME + " TEXT, " + STATE_DATE
            + " TEXT, " + TEXT + " TEXT, " + STATE + " INTEGER," + TYPE + " INTEGER," + IS_READ + " INTEGER," + PATH
            + " TEXT, " + SHORT_DATE + " TEXT, " + LATITUDE + " TEXT," + LONGITUDE + " TEXT," + ADDRESS + " TEXT,"
            + DURATION + " INTEGER," + STICKER_CATEGORY + " INTEGER," + STICKER_NAME + " NAME," + SCHEDULE_TIME
            + " INTEGER, " + SCHEDULE_TIME_TEXT + " TEXT," + GROUP_SEND_ID + " TEXT," + GROUP_DILIVERY_USERS + " TEXT,"
            + SENDER_ID + " INTEGER," + CHAT_TYPE + " INTEGER," + GROUP_NUM_DILIVERY + " INTEGER,"
            + IS_DOWNLOAD_FILE + " INTEGER," + FILE_SIZE + " INTEGER," + TEXT_ASCII + " TEXT," + FORWARD_FROM_ID
            + " INTEGER," + FORWARD_FROM_NAME + " TEXT, " + FILE_URL + " TEXT," + VIEW_TYPE + " INTEGER, "
            + PHONE_NUMBER + " TEXT, " + FULL_NAME + " TEXT, " + CREATED + " INTEGER, " + BROADCAST_ID + " INTEGER," + PHONE_NO + " TEXT)";

    public static final String CREATE_MSG_ID_INDEX = "CREATE INDEX msg_id_index ON " + TABLE_NAME + "(" + MESSAGE_ID
            + ")";
    public static final String DELETE_TABLE = "DELETE FROM " + TABLE_NAME;
}
