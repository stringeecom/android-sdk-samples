package com.stringee.video_conference_sample.stringee_wrapper.ui.custom_view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class DraggableFrameLayout extends FrameLayout {

    private float lastTouchX;
    private float lastTouchY;

    private int leftBound;
    private int rightBound;
    private int topBound;
    private int bottomBound;

    public DraggableFrameLayout(Context context) {
        super(context);
        initBounds(context);
    }

    public DraggableFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initBounds(context);
    }

    public DraggableFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initBounds(context);
    }

    private void initBounds(Context context) {
        post(() -> {
            MarginLayoutParams layoutParams = (MarginLayoutParams) this.getLayoutParams();

            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;

            leftBound = layoutParams.leftMargin;
            topBound = layoutParams.topMargin;
            rightBound = screenWidth - getWidth() - layoutParams.rightMargin;
            bottomBound = screenHeight - getHeight() - layoutParams.bottomMargin;
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
                float dx = event.getX() - lastTouchX;
                float dy = event.getY() - lastTouchY;

                float newX = getX() + dx;
                float newY = getY() + dy;

                if (newX < leftBound) newX = leftBound;
                if (newX > rightBound) newX = rightBound;
                if (newY < topBound) newY = topBound;
                if (newY > bottomBound) newY = bottomBound;

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