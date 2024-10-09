package com.outgoer.ui.story.view

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs


class CubeTransformer : ViewPager2.PageTransformer {
    private val maxRotationY = 45F
    override fun transformPage(view: View, position: Float) {
        val deltaY = 0.5F
        val rotationAngle = maxRotationY * position
        view.pivotX = if (position < 0F) view.width.toFloat() else 0F
        view.pivotY = view.height * deltaY
        view.rotationY = rotationAngle
    }
}