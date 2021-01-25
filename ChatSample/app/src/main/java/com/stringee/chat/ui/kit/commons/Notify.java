package com.stringee.chat.ui.kit.commons;

public enum Notify {
    CONNECTION_CONNECTED("com.stringee.connection.connected"),
    CONVERSATION_ADDED("com.stringee.conversation.added"),
    CONVERSATION_UPDATED("com.stringee.conversation.updated"),
    CONVERSATION_DELETED("com.stringee.conversation.deleted"),
    MESSAGE_ADDED("com.stringee.message.added"),
    MESSAGE_UPDATED("com.stringee.message.updated"),
    MESSAGE_DELETED("com.stringee.message.deleted");

    private String value;

    private Notify(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
