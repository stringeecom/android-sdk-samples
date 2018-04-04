package com.stringee.softphone.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.stringee.softphone.common.Common;
import com.stringee.softphone.common.Constant;
import com.stringee.softphone.common.Utils;

public class Message implements Parcelable {
    private int id;
    private int conversationId;
    private int fromUserId;
    private int toUserId;
    private int type;
    private int msgId;
    private int isRead;
    private int attachment;
    private String datetime;
    private String stateDate;
    private String text;
    private int state; // 1:sending 2:sent 3:delivered 4:not show yet
    private String path;
    private String shortDate;
    private String username;
    private String latitude;
    private String longitude;
    private String address;
    private double duration; // call duration in seconds
    private boolean isPlayed;
    private String fullname;
    private int stickerCategory;
    private String stickerName;
    private String avatarSmall;
    private String strDate;
    private String strHour;
    private boolean isSent;
    private long scheduleTime;
    private String strScheduleTime;
    private String groupSendId;
    private String groupDiliveryId;
    private int senderId;
    private int chatType = Constant.CHAT_TYPE_PRIVATE;
    private int groupNumDilivery;
    private int isDownloadFile = 0;
    private int fileSize;
    private String textAscii;
    private int forwardFromId;
    private String forwardFromName;
    private String fileUrl;
    private String phoneNumber;
    private long created;//time
    private int fileType = 0;
    private String thumbnail;
    private String thumbnailUrl;
    private int broadcastId = 0;
    private String phoneNo;

    private boolean isUploadFile = false;
    private boolean isForwardMessage = false;
    private boolean isLast;

    public static final int FILE_TYPE_FILE = 1;
    public static final int FILE_TYPE_IMAGE = 2;
    public static final int FILE_TYPE_AUDIO = 3;
    public static final int FILE_TYPE_DOCUMENT = 4;

    public Message() {
    }

    public Message(int fromUser, int toUser, String text, String datetime, int type, int chatType) {
        this.conversationId = fromUser == Common.userId ? toUser : fromUser;
        this.fromUserId = fromUser;
        this.toUserId = toUser;
        this.text = text;
        this.datetime = datetime;
        this.type = type;
        this.chatType = chatType;
        if (text.length() > 24) {
            if (type == Constant.TYPE_MESSAGE_SENT)
                this.type = Constant.TYPE_MESSAGE_SENT_L;
            else if (type == Constant.TYPE_MESSAGE_INCOME_L)
                this.type = Constant.TYPE_MESSAGE_INCOME_L;
        }
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getConversationId() {
        return conversationId;
    }

    public void setConversationId(int conversationId) {
        this.conversationId = conversationId;
    }

    public int getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(int fromUserId) {
        this.fromUserId = fromUserId;
    }

    public int getToUserId() {
        return toUserId;
    }

    public void setToUserId(int toUserId) {
        this.toUserId = toUserId;
    }

    public int getMsgId() {
        return msgId;
    }

    public void setMsgId(int msgId) {
        this.msgId = msgId;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getIsRead() {
        return isRead;
    }

    public void setIsRead(int isRead) {
        if (isRead > 0)
            isRead = 1;
        this.isRead = isRead;
    }

    public int getAttachment() {
        return attachment;
    }

    public void setAttachment(int attachment) {
        this.attachment = attachment;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getStateDate() {
        return stateDate;
    }

    public void setStateDate(String stateDate) {
        this.stateDate = stateDate;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getShortDate() {
        return shortDate;
    }

    public void setShortDate(String shortDate) {
        this.shortDate = shortDate;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public boolean isPlayed() {
        return isPlayed;
    }

    public void setPlayed(boolean isPlayed) {
        this.isPlayed = isPlayed;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public int getStickerCategory() {
        return stickerCategory;
    }

    public void setStickerCategory(int stickerCategory) {
        this.stickerCategory = stickerCategory;
    }

    public String getStickerName() {
        return stickerName;
    }

    public void setStickerName(String stickerName) {
        this.stickerName = stickerName;
    }

    public String getAvatarSmall() {
        return avatarSmall;
    }

    public void setAvatarSmall(String avatarSmall) {
        this.avatarSmall = avatarSmall;
    }

    public String getStrDate() {
        return strDate;
    }

    public void setStrDate(String strDate) {
        this.strDate = strDate;
    }

    public String getStrHour() {
        return strHour;
    }

    public void setStrHour(String strHour) {
        this.strHour = strHour;
    }

    public boolean isSent() {
        return isSent;
    }

    public void setSent(boolean isSent) {
        this.isSent = isSent;
    }

    public long getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(long scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public String getStrScheduleTime() {
        return strScheduleTime;
    }

    public void setStrScheduleTime(String strScheduleTime) {
        this.strScheduleTime = strScheduleTime;
    }

    public String getGroupSendId() {
        return groupSendId;
    }

    public void setGroupSendId(String groupSendId) {
        this.groupSendId = groupSendId;
    }

    public String getGroupDiliveryId() {
        return groupDiliveryId;
    }

    public void setGroupDiliveryId(String groupDiliveryId) {
        this.groupDiliveryId = groupDiliveryId;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getChatType() {
        return chatType;
    }

    public void setChatType(int chatType) {
        this.chatType = chatType;
    }

    public int getGroupNumDilivery() {
        return groupNumDilivery;
    }

    public void setGroupNumDilivery(int groupNumDilivery) {
        this.groupNumDilivery = groupNumDilivery;
    }

    public boolean isUploadFile() {
        return isUploadFile;
    }

    public void setUploadFile(boolean isUploadFile) {
        this.isUploadFile = isUploadFile;
    }

    public int getIsDownloadFile() {
        return isDownloadFile;
    }

    public void setIsDownloadFile(int isDownloadFile) {
        this.isDownloadFile = isDownloadFile;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public void setTextAscii(String textAscii) {
        this.textAscii = textAscii;
    }

    public int getForwardFromId() {
        return forwardFromId;
    }

    public void setForwardFromId(int forwardFromId) {
        this.forwardFromId = forwardFromId;
    }

    public String getForwardFromName() {
        return forwardFromName;
    }

    public void setForwardFromName(String forwardFromName) {
        this.forwardFromName = forwardFromName;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = Utils.formatPhone(phoneNumber);
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    public int getBroadcastId() {
        return broadcastId;
    }

    public void setBroadcastId(int broadcastId) {
        this.broadcastId = broadcastId;
    }

    public boolean isForwardMessage() {
        return isForwardMessage;
    }

    public void setForwardMessage(boolean forwardMessage) {
        isForwardMessage = forwardMessage;
    }

    public boolean isLast() {
        return isLast;
    }

    public void setLast(boolean last) {
        isLast = last;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public int getFileType() {
        if (fileType == 0) {
            if (getType() == Constant.TYPE_PICTURE_SENT || getType() == Constant.TYPE_PICTURE_INCOME || getType() == Constant.TYPE_PICTURE_URL)
                setFileType(Message.FILE_TYPE_IMAGE);
            else if (getType() == Constant.TYPE_AUDIO_SENT || getType() == Constant.TYPE_AUDIO_INCOME || getType() == Constant.TYPE_AUDIO_URL)
                setFileType(Message.FILE_TYPE_AUDIO);
            else if (getType() == Constant.TYPE_FILE_INCOME || getType() == Constant.TYPE_FILE_SENT)
                setFileType(Message.FILE_TYPE_FILE);
        }
        return fileType;
    }

    public void setFileType(int fileType) {
        this.fileType = fileType;
    }

    public Message(Parcel in) {
        id = in.readInt();
        conversationId = in.readInt();
        fromUserId = in.readInt();
        toUserId = in.readInt();
        type = in.readInt();
        msgId = in.readInt();
        isRead = in.readInt();
        attachment = in.readInt();
        datetime = in.readString();
        stateDate = in.readString();
        text = in.readString();
        state = in.readInt();
        path = in.readString();
        shortDate = in.readString();
        username = in.readString();
        latitude = in.readString();
        longitude = in.readString();
        address = in.readString();
        duration = in.readDouble();
        fullname = in.readString();
        stickerCategory = in.readInt();
        stickerName = in.readString();
        avatarSmall = in.readString();
        strDate = in.readString();
        strHour = in.readString();
        int iSent = in.readInt();
        if (iSent == 1) {
            isSent = true;
        } else {
            isSent = false;
        }
        scheduleTime = in.readLong();
        strScheduleTime = in.readString();
        groupSendId = in.readString();
        groupDiliveryId = in.readString();
        senderId = in.readInt();
        chatType = in.readInt();
        groupNumDilivery = in.readInt();
        isDownloadFile = in.readInt();
        fileSize = in.readInt();
        textAscii = in.readString();
        forwardFromId = in.readInt();
        forwardFromName = in.readString();
        fileUrl = in.readString();
        phoneNumber = in.readString();
        created = in.readLong();
        fileType = in.readInt();
        thumbnail = in.readString();
        thumbnailUrl = in.readString();
        broadcastId = in.readInt();
        phoneNo = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flag) {
        dest.writeInt(id);
        dest.writeInt(conversationId);
        dest.writeInt(fromUserId);
        dest.writeInt(toUserId);
        dest.writeInt(type);
        dest.writeInt(msgId);
        dest.writeInt(isRead);
        dest.writeInt(attachment);
        dest.writeString(datetime);
        dest.writeString(stateDate);
        dest.writeString(text);
        dest.writeInt(state);
        dest.writeString(path);
        dest.writeString(shortDate);
        dest.writeString(username);
        dest.writeString(latitude);
        dest.writeString(longitude);
        dest.writeString(address);
        dest.writeDouble(duration);
        dest.writeString(fullname);
        dest.writeInt(stickerCategory);
        dest.writeString(stickerName);
        dest.writeString(avatarSmall);
        dest.writeString(strDate);
        dest.writeString(strHour);
        if (isSent) {
            dest.writeInt(1);
        } else {
            dest.writeInt(0);
        }
        dest.writeLong(scheduleTime);
        dest.writeString(strScheduleTime);
        dest.writeString(groupSendId);
        dest.writeString(groupDiliveryId);
        dest.writeInt(senderId);
        dest.writeInt(chatType);
        dest.writeInt(groupNumDilivery);
        dest.writeInt(isDownloadFile);
        dest.writeInt(fileSize);
        dest.writeString(textAscii);
        dest.writeInt(forwardFromId);
        dest.writeString(forwardFromName);
        dest.writeString(fileUrl);
        dest.writeString(phoneNumber);
        dest.writeLong(created);
        dest.writeInt(fileType);
        dest.writeString(thumbnail);
        dest.writeString(thumbnailUrl);
        dest.writeInt(broadcastId);
        dest.writeString(phoneNo);
    }

    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

}
