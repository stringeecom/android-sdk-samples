package com.stringee.softphone.model;

import com.stringee.softphone.common.Constant;

public class Recent {
    private int id;
    private int userId;
    private String appUserId;
    private String avatar;
    private String shortDate;
    private String name;
    private String nameAscii;
    private String phoneNumberRaw;
    private int type;
    private int state;
    private int numCall;
    private String phoneNumber;
    private String text;
    private String datetime;
    private String agentId;
    private String phoneNo;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getAppUserId() {
        return appUserId;
    }

    public void setAppUserId(String appUserId) {
        this.appUserId = appUserId;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getShortDate() {
        return shortDate;
    }

    public void setShortDate(String shortDate) {
        this.shortDate = shortDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameAscii() {
        return nameAscii;
    }

    public void setNameAscii(String nameAscii) {
        this.nameAscii = nameAscii;
    }

    public String getPhoneNumberRaw() {
        return phoneNumberRaw;
    }

    public void setPhoneNumberRaw(String phoneNumberRaw) {
        this.phoneNumberRaw = phoneNumberRaw;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getNumCall() {
        return numCall;
    }

    public void setNumCall(int numCall) {
        this.numCall = numCall;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public void setRecentFromMessage(Message message, int numCall) {
        this.id = message.getId();
        if (message.getType() == Constant.TYPE_OUTGOING_CALL) {
            this.userId = message.getToUserId();
        } else {
            this.userId = message.getFromUserId();
        }
        this.avatar = message.getAvatarSmall();
        this.shortDate = message.getShortDate();
        this.type = message.getType();
        this.state = message.getState();
        this.numCall = numCall;
        this.name = message.getFullname();
        this.phoneNumber = message.getPhoneNumber();
        this.phoneNumberRaw = message.getPhoneNumber();
        this.text = message.getText();
        this.datetime = message.getDatetime();
        this.phoneNo = message.getPhoneNo();
    }
}
