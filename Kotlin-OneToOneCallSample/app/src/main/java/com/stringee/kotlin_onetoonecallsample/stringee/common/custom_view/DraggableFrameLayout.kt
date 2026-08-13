package com.stringee.kotlin_onetoonecallsample.stringee.common.custom_view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import kotlin.math.max
import kotlin.math.min

/** FrameLayout that lets the local video preview be dragged within its parent bounds. */
class DraggableFrameLayout : FrameLayout {
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var leftBound = 0
    private var rightBound = 0
    private var topBound = 0
    private var bottomBound = 0

    constructor(context: Context) : super(context) {
        initializeBounds(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initializeBounds(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initializeBounds(context)
    }

    private fun initializeBounds(context: Context) {
        post(Runnable {
            val params = getLayoutParams() as MarginLayoutParams
            val screenWidth = context.getResources().getDisplayMetrics().widthPixels
            val screenHeight = context.getResources().getDisplayMetrics().heightPixels
            leftBound = params.leftMargin
            topBound = params.topMargin
            rightBound = screenWidth - getWidth() - params.rightMargin
            bottomBound = screenHeight - getHeight() - params.bottomMargin
        })
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.getX()
                lastTouchY = event.getY()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val newX = max(
                    leftBound.toFloat(),
                    min(rightBound.toFloat(), getX() + event.getX() - lastTouchX)
                )
                val newY = max(
                    topBound.toFloat(),
                    min(bottomBound.toFloat(), getY() + event.getY() - lastTouchY)
                )
                setX(newX)
                setY(newY)
                return true
            }

            MotionEvent.ACTION_UP -> {
                performClick()
                return true
            }

            else -> return super.onTouchEvent(event)
        }
    }
}
