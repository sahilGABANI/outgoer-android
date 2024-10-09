package com.outgoer.base.view

import android.content.Context
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller

class NonSwappableViewPager : ViewPager {

    private var isPagingEnabled = true

    constructor(context: Context) : super(context) {
        setMyScroller()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setMyScroller()

    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        // Never allow swiping to switch between pages
        return false
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Never allow swiping to switch between pages
        return false
    }

//    override fun onTouchEvent(event: MotionEvent): Boolean {
//        return this.isPagingEnabled && super.onTouchEvent(event)
//    }
//
//    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
//        return this.isPagingEnabled && super.onInterceptTouchEvent(event)
//    }

    fun setPagingEnabled(b: Boolean) {
        this.isPagingEnabled = b
    }

    private fun setMyScroller() {
        try {
            val viewpager = ViewPager::class.java
            val scroller = viewpager.getDeclaredField("mScroller")
            scroller.isAccessible = true
            scroller.set(this, MyScroller(context))
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    inner class MyScroller(context: Context) : Scroller(context, DecelerateInterpolator()) {

        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
            super.startScroll(startX, startY, dx, dy, 350 /*1 secs*/)
        }
    }
}