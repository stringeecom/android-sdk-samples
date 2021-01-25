package com.stringee.chat.ui.kit.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import java.util.ArrayList;

/**
 * Created by huybq7 on 9/18/2014.
 */
public class CusRelativeLayout extends RelativeLayout {

    private final int KEYBOARD_NOT_SET = -1;
    private final int KEYBOARD_HIDDEN = 0;
    private final int KEYBOARD_SHOW = 1;
    private int isHiddenKeyboard = KEYBOARD_NOT_SET;

    public interface IKeyboardChanged {
        void onKeyboardShown();

        void onKeyboardHidden();
    }

    private ArrayList<IKeyboardChanged> mKeyboardListener = new ArrayList<IKeyboardChanged>();

    public CusRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CusRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CusRelativeLayout(Context context) {
        super(context);
    }

    public void addKeyboardStateChangedListener(IKeyboardChanged listener) {
        mKeyboardListener.add(listener);
    }

    public void removeKeyboardStateChangedListener(IKeyboardChanged listener) {
        mKeyboardListener.remove(listener);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int proposedheight = MeasureSpec.getSize(heightMeasureSpec);
        final int actualHeight = getHeight();

        if (proposedheight > 0 && actualHeight > 0 && Math.abs(actualHeight - proposedheight) > 100) {
            if (actualHeight > proposedheight) {
                notifyKeyboardShown();
            } else if (actualHeight < proposedheight) {
                notifyKeyboardHidden();
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void notifyKeyboardHidden() {
        if (isHiddenKeyboard != KEYBOARD_HIDDEN) {
            isHiddenKeyboard = KEYBOARD_HIDDEN;
            for (IKeyboardChanged listener : mKeyboardListener) {
                listener.onKeyboardHidden();
            }
        }
    }

    private void notifyKeyboardShown() {
        if (isHiddenKeyboard != KEYBOARD_SHOW) {
            isHiddenKeyboard = KEYBOARD_SHOW;
            for (IKeyboardChanged listener : mKeyboardListener) {
                listener.onKeyboardShown();
            }
        }
    }
}