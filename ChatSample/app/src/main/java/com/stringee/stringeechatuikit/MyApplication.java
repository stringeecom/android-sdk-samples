package com.stringee.stringeechatuikit;

import android.app.Application;
import android.graphics.Typeface;

import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.stringee.chat.ui.kit.commons.LocalImageLoader;
import com.stringee.stringeechatuikit.common.Common;
import com.stringee.stringeechatuikit.common.Constant;

/**
 * Created by luannguyen on 2/12/2018.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Common.preferences = getSharedPreferences(Constant.PREF_BASE, MODE_PRIVATE);
        Common.boldType = Typeface.createFromAsset(getAssets(), "fonts/rmedium.ttf");
        Common.normalType = Typeface.createFromAsset(getAssets(), "fonts/rmono.ttf");
        Common.iconTypeface = Typeface.createFromAsset(getAssets(), "fonts/fontawesome-webfont.ttf");

        ImageLoaderConfiguration config1 = new ImageLoaderConfiguration.Builder(this)
                .denyCacheImageMultipleSizesInMemory().diskCacheFileNameGenerator(new Md5FileNameGenerator()).build();
        ImageLoader.getInstance().init(config1);

        ImageLoaderConfiguration config2 = new ImageLoaderConfiguration.Builder(this)
                .denyCacheImageMultipleSizesInMemory().diskCacheFileNameGenerator(new Md5FileNameGenerator()).build();
        LocalImageLoader.getInstance().init(config2);
    }
}
