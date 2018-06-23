package com.stringee.softphone.common;

/**
 * Created by luannguyen on 7/27/2017.
 */

public enum Notify {
    UPDATE_RECENTS("com.stringee.softphone.recents.update"),
    END_CALL("com.stringee.softphone.call.end"),
    END_CALL_FROM_DIAL("com.stringee.softphone.call.end_dial"),
    CHECK_BALANCE("com.stringee.softphone.balance.check"),;

    private String value;

    private Notify(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
