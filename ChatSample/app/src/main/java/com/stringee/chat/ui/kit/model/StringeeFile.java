package com.stringee.chat.ui.kit.model;

public class StringeeFile {
    private String name;
    private String path;
    private int type;
    private long size;
    private boolean isChecked;
    private int level;

    public static final int TYPE_OTHER_FILE = 0;
    public static final int TYPE_DIRECTORY = 1;
    public static final int TYPE_DOCUMENT = 2;
    public static final int TYPE_MEDIA = 3;
    public static final int TYPE_IMAGE = 4;
    public static final int TYPE_VIDEO = 5;
    public static final int TYPE_ZIP = 6;
    public static final int TYPE_BACK = -1;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getSum() {
        if (size / 1024 < 1) {
            return (String.format("%.2f", (float) size) + "KB");
        } else {
            float fsize = (float) size / 1024;
            return (String.format("%.2f", fsize) + "MB");
        }
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean isChecked) {
        this.isChecked = isChecked;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
