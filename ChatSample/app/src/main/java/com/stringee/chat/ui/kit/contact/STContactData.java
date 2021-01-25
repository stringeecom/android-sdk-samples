package com.stringee.chat.ui.kit.contact;

import android.graphics.Bitmap;

public class STContactData {

    private String name;
    private String email;
    private Bitmap avatar;
    private String phone;


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Bitmap getAvatar() {
        return avatar;
    }

    public void setAvatar(Bitmap avatar) {
        this.avatar = avatar;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isValid() {
        return (this.name != null && (this.phone != null || this.email != null));

    }

}
