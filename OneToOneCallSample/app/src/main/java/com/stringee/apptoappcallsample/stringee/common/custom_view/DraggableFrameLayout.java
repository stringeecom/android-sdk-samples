package com.stringee.apptoappcallsample.stringee.common.custom_view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/** FrameLayout that lets the local video preview be dragged within its parent bounds. */
public class DraggableFrameLayout extends FrameLayout {
    private float lastTouchX;
    private float lastTouchY;
    private int leftBound;
    private int rightBound;
    private int topBound;
    private int bottomBound;

    public DraggableFrameLayout(Context context) {
        super(context);
        initializeBounds(context);
    }

    public DraggableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeBounds(context);
    }

    public DraggableFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeBounds(context);
    }

    private void initializeBounds(Context context) {
        post(() -> {
            MarginLayoutParams params = (MarginLayoutParams) getLayoutParams();
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            leftBound = params.leftMargin;
            topBound = params.topMargin;
            rightBound = screenWidth - getWidth() - params.rightMargin;
            bottomBound = screenHeight - getHeight() - params.bottomMargin;
        });
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float newX = Math.max(leftBound,
                        Math.min(rightBound, getX() + event.getX() - lastTouchX));
                float newY = Math.max(topBound,
                        Math.min(bottomBound, getY() + event.getY() - lastTouchY));
                setX(newX);
                setY(newY);
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
