package com.stringee.chat.ui.kit.model;

public class Sticker implements Comparable<Sticker> {

    private String catId;
    private String name;
    private String path;

    public Sticker() {

    }

    public String getCatId() {
        return catId;
    }

    public void setCatId(String catId) {
        this.catId = catId;
    }

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

    @Override
    public int compareTo(Sticker another) {
        return this.name.compareTo(another.getName());
    }
}
