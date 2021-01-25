package com.stringee.chat.ui.kit.model;

import java.io.Serializable;

public abstract class DataItem implements Serializable {
    private String dataPath;
    private long dateAdd;

    DataItem(String dataPath, long dateAdd) {
        this.dataPath = dataPath;
        this.dateAdd = dateAdd;
    }

    public String getDataPath() {
        return this.dataPath;
    }

    public long getDateAdd() {
        return this.dateAdd;
    }

    public String getDuration() {
        return "";
    }
}
