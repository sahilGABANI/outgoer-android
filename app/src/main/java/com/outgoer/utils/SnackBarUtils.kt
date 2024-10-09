package com.outgoer.utils

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.outgoer.R

object SnackBarUtils {
    private const val STATIC_MESSAGE = "Please check your internet connection"

    fun showTopSnackBar(view: View, message: String = STATIC_MESSAGE) {
        val snackBar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(ContextCompat.getColor(view.context, R.color.color_B421FF))
        val params = snackBarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        snackBarView.layoutParams = params
        snackBar.show()
    }
}