package com.stringee.video_conference_sample.stringee_wrapper.ui.base;

import android.graphics.drawable.Drawable;
import android.widget.ImageButton;

import androidx.databinding.BindingAdapter;
import androidx.lifecycle.ViewModel;

public abstract class MyViewModel extends ViewModel {
    @BindingAdapter("android:src")
    public static void setImageButtonSrc(ImageButton imageButton, Drawable src) {
        imageButton.setImageDrawable(src);
    }

    @BindingAdapter("android:background")
    public static void setImageButtonBackground(ImageButton imageButton, Drawable background) {
        imageButton.setBackground(background);
    }
}
