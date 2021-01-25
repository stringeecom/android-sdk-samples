package com.stringee.chat.ui.kit.model;

public class Video extends DataItem {
    private String duration;

    @Override
    public String getDuration() {
        return duration;
    }

    public Video(String dataPath, long dateAdd, String duration) {
        super(dataPath, dateAdd);
        this.duration = duration;
    }
}
