package com.stringee.chat.ui.kit.commons;

import com.nostra13.universalimageloader.core.ImageLoader;

public class LocalImageLoader extends ImageLoader {

    private static LocalImageLoader instance;

    public static LocalImageLoader getInstance() {
        if (instance == null) {
            synchronized (ImageLoader.class) {
                if (instance == null) {
                    instance = new LocalImageLoader();
                }
            }
        }
        return instance;
    }
}