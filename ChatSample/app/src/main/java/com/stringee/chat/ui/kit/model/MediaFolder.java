package com.stringee.chat.ui.kit.model;

import java.io.Serializable;
import java.util.ArrayList;

public class MediaFolder implements Serializable {
    private String name;
    private ArrayList<DataItem> listData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<DataItem> getListData() {
        return listData;
    }

    public void setListData(ArrayList<DataItem> listData) {
        this.listData = listData;
    }
}
