package com.stringee.chat.ui.kit.model;

import java.io.Serializable;

/**
 * Created by luannguyen on 7/11/2017.
 */

public class Contact implements Serializable {

    private String name;
    private String phone;
    private int type;

    public Contact() {

    }

    public Contact(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
