package com.stringee.softphone.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.stringee.softphone.common.Constant;
import com.stringee.softphone.model.Message;
import com.stringee.softphone.model.Recent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageHandler extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "com.stringee.softphone.db.message";

    private static MessageHandler databaseHandler;
    private SQLiteDatabase writeDatabase;
    private SQLiteDatabase readDatabase;

    /**
     * @return singleton class instance
     */
    public static MessageHandler getInstance(Context context) {
        if (databaseHandler == null) {
            synchronized (MessageHandler.class) {
                if (databaseHandler == null) {
                    databaseHandler = new MessageHandler(context);
                }
            }
        }
        return databaseHandler;
    }

    public MessageHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        writeDatabase = getWritableDatabase();
        readDatabase = getReadableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            createAllTable(db);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void createAllTable(SQLiteDatabase db) {
        db.execSQL(MessageConstant.CREATE_TABLE);

        db.execSQL(MessageConstant.CREATE_MSG_ID_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 1 && newVersion == 2) {
            db.execSQL("ALTER TABLE " + MessageConstant.TABLE_NAME + " ADD COLUMN " + MessageConstant.PHONE_NO + " TEXT");
        }
    }

    public void clearData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL(MessageConstant.DELETE_TABLE);
    }

    // getRecents
    public List<Recent> getRecents() {
        List<Message> messages = new ArrayList<Message>();

        String query = "SELECT * FROM " + MessageConstant.TABLE_NAME + " WHERE " + MessageConstant.TYPE + " IN (?,?,?,?,?) ORDER BY " + MessageConstant.DATETIME
                + " DESC LIMIT 100";
        String[] selectionArgs = {String.valueOf(Constant.TYPE_CALL_OUT), String.valueOf(Constant.TYPE_CALL_PHONE_TO_APP), String.valueOf(Constant.TYPE_OUTGOING_CALL), String.valueOf(Constant.TYPE_INCOMING_CALL), String.valueOf(Constant.TYPE_MISSED_CALL)};
        Cursor cursor = writeDatabase.rawQuery(query, selectionArgs);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Message message = genMessageFromCursor(cursor);
                    messages.add(message);
                } while (cursor.moveToNext());
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        Map<String, Recent> mapRecents = new HashMap<String, Recent>();

        for (Message message : messages) {
            Recent recent = new Recent();
            recent.setRecentFromMessage(message, 1);
            String key;
            if (recent.getAgentId() != null) {
                key = "agent:" + recent.getAgentId();
            } else {
                key = "pn:" + recent.getPhoneNumber();
            }
            Recent recent2 = mapRecents.get(key);
            if (recent2 == null) {
                mapRecents.put(key, recent);
            } else {
                int numCall = recent2.getNumCall();
                recent.setNumCall(++numCall);
                recent.setText(recent2.getText());
                recent.setShortDate(recent2.getShortDate());
                recent.setDatetime(recent2.getDatetime());
                recent.setType(recent2.getType());
                mapRecents.put(key, recent);
            }
        }
        ArrayList<Recent> recents = new ArrayList<Recent>(mapRecents.values());
        Collections.sort(recents, new Comparator<Recent>() {

            @Override
            public int compare(Recent lhs, Recent rhs) {
                String date1 = lhs.getDatetime();
                String date2 = rhs.getDatetime();
                return date1.compareTo(date2);
            }
        });
        Collections.reverse(recents);
        return recents;
    }

    private Message genMessageFromCursor(Cursor cursor) {
        Message message = new Message();
        message.setId(cursor.getInt(0));
        message.setMsgId(cursor.getInt(1));
        message.setConversationId(cursor.getInt(2));
        message.setFromUserId(cursor.getInt(3));
        message.setToUserId(cursor.getInt(4));
        message.setAttachment(cursor.getInt(5));
        message.setDatetime(cursor.getString(6));
        message.setStateDate(cursor.getString(7));
        message.setText(cursor.getString(8));
        message.setState(cursor.getInt(9));
        message.setType(cursor.getInt(10));
        message.setIsRead(cursor.getInt(11));
        message.setPath(cursor.getString(12));
        message.setShortDate(cursor.getString(13));
        message.setLatitude(cursor.getString(14));
        message.setLongitude(cursor.getString(15));
        message.setAddress(cursor.getString(16));
        message.setDuration(cursor.getInt(17));
        message.setStickerCategory(cursor.getInt(18));
        message.setStickerName(cursor.getString(19));
        message.setScheduleTime(cursor.getLong(20));
        message.setStrScheduleTime(cursor.getString(21));
        message.setGroupSendId(cursor.getString(22));
        message.setGroupDiliveryId(cursor.getString(23));
        message.setSenderId(cursor.getInt(24));
        message.setChatType(cursor.getInt(25));
        message.setGroupNumDilivery(cursor.getInt(26));
        message.setIsDownloadFile(cursor.getInt(27));
        message.setFileSize(cursor.getInt(28));
        message.setTextAscii(cursor.getString(29));
        message.setForwardFromId(cursor.getInt(30));
        message.setForwardFromName(cursor.getString(31));
        message.setFileUrl(cursor.getString(32));
        message.setPhoneNumber(cursor.getString(34));
        message.setFullname(cursor.getString(35));
        message.setCreated(cursor.getLong(36));
        message.setBroadcastId(cursor.getInt(37));
        message.setPhoneNo(cursor.getString(38));
        if (message.getText().length() > 24) {
            if (message.getType() == Constant.TYPE_MESSAGE_SENT)
                message.setType(Constant.TYPE_MESSAGE_SENT_L);
            else if (message.getType() == Constant.TYPE_MESSAGE_INCOME)
                message.setType(Constant.TYPE_MESSAGE_INCOME_L);
        }
        return message;
    }

    public int insertMessage(Message message) {
        ContentValues values = new ContentValues();
        values.put(MessageConstant.MESSAGE_ID, message.getMsgId());
        values.put(MessageConstant.CONVERSATION_ID, message.getConversationId());
        values.put(MessageConstant.FROM_USER_ID, message.getFromUserId());
        values.put(MessageConstant.TO_USER_ID, message.getToUserId());
        values.put(MessageConstant.ATTACHMENT, message.getAttachment());
        values.put(MessageConstant.DATETIME, message.getDatetime());
        values.put(MessageConstant.TEXT, message.getText());
        values.put(MessageConstant.STATE, message.getState());
        values.put(MessageConstant.TYPE, message.getType());
        values.put(MessageConstant.IS_READ, message.getIsRead());
        values.put(MessageConstant.PATH, message.getPath());
        values.put(MessageConstant.SHORT_DATE, message.getShortDate());
        values.put(MessageConstant.LATITUDE, message.getLatitude());
        values.put(MessageConstant.LONGITUDE, message.getLongitude());
        values.put(MessageConstant.ADDRESS, message.getAddress());
        values.put(MessageConstant.DURATION, message.getDuration());
        values.put(MessageConstant.STICKER_CATEGORY, message.getStickerCategory());
        values.put(MessageConstant.STICKER_NAME, message.getStickerName());
        values.put(MessageConstant.SCHEDULE_TIME, message.getScheduleTime());
        values.put(MessageConstant.SCHEDULE_TIME_TEXT, message.getStrScheduleTime());
        values.put(MessageConstant.GROUP_SEND_ID, message.getGroupSendId());
        values.put(MessageConstant.GROUP_DILIVERY_USERS, message.getGroupDiliveryId());
        values.put(MessageConstant.SENDER_ID, message.getSenderId());
        values.put(MessageConstant.CHAT_TYPE, message.getChatType());
        values.put(MessageConstant.GROUP_NUM_DILIVERY, message.getGroupNumDilivery());
        values.put(MessageConstant.IS_DOWNLOAD_FILE, message.getIsDownloadFile());
        values.put(MessageConstant.FILE_SIZE, message.getFileSize());
        values.put(MessageConstant.TEXT_ASCII, message.getText());
        values.put(MessageConstant.FORWARD_FROM_ID, message.getForwardFromId());
        values.put(MessageConstant.FORWARD_FROM_NAME, message.getForwardFromName());
        values.put(MessageConstant.FILE_URL, message.getFileUrl());
        values.put(MessageConstant.PHONE_NUMBER, message.getPhoneNumber());
        values.put(MessageConstant.FULL_NAME, message.getFullname());
        values.put(MessageConstant.CREATED, message.getCreated());
        values.put(MessageConstant.BROADCAST_ID, message.getBroadcastId());
        values.put(MessageConstant.PHONE_NO, message.getPhoneNo());
        return (int) writeDatabase.insert(MessageConstant.TABLE_NAME, null, values);
    }

    public int updateCall(Message Message) {

        ContentValues values = new ContentValues();
        values.put(MessageConstant.TEXT, Message.getText());
        values.put(MessageConstant.STATE, Message.getState());
        values.put(MessageConstant.IS_READ, Message.getIsRead());

        // updating row
        return writeDatabase.update(MessageConstant.TABLE_NAME, values, MessageConstant.ID + " = ?",
                new String[]{String.valueOf(Message.getId())});
    }

    public List<Message> getMessageCallByPhone(String phoneNumber) {
        List<Message> messages = new ArrayList<Message>();
        if (phoneNumber.length() == 0) {
            return messages;
        }
        String query = "SELECT * FROM " + MessageConstant.TABLE_NAME + " WHERE " + MessageConstant.PHONE_NUMBER
                + " = ? AND " + MessageConstant.TYPE + " IN (?,?,?,?,?) " + " ORDER BY " + MessageConstant.ID + " DESC LIMIT 100";
        String[] selectionArgs = new String[]{phoneNumber, String.valueOf(Constant.TYPE_CALL_OUT), String.valueOf(Constant.TYPE_CALL_PHONE_TO_APP), String.valueOf(Constant.TYPE_OUTGOING_CALL), String.valueOf(Constant.TYPE_INCOMING_CALL), String.valueOf(Constant.TYPE_MISSED_CALL)};
        Cursor cursor = writeDatabase.rawQuery(query, selectionArgs);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    Message message = genMessageFromCursor(cursor);
                    messages.add(message);
                } while (cursor.moveToNext());
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return messages;
    }

    public Message getLastOutgoingCall() {
        String query = "SELECT * FROM " + MessageConstant.TABLE_NAME + " WHERE " + MessageConstant.TYPE + " = ? " + " ORDER BY " + MessageConstant.DATETIME + " DESC LIMIT 1";
        String[] selectionArgs = {String.valueOf(Constant.TYPE_CALL_OUT)};
        Cursor cursor = writeDatabase.rawQuery(query, selectionArgs);
        Message message = null;
        if (cursor != null && cursor.moveToFirst()) {
            message = genMessageFromCursor(cursor);
            cursor.close();
        }
        return message;
    }
}
