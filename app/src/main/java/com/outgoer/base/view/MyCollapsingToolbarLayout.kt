package com.outgoer.base.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.outgoer.R

@SuppressLint("CustomViewStyleable")
class MyCollapsingToolbarLayout(context: Context, attrs: AttributeSet?) :
    CollapsingToolbarLayout(context, attrs) {
    private val toolbarId: Int
    private var toolbar: MaterialToolbar? = null
    override fun setScrimsShown(shown: Boolean, animate: Boolean) {
        super.setScrimsShown(shown, animate)
        findToolbar()
        if (toolbar != null) {
            val tvVenueName = findViewById<AppCompatTextView>(R.id.tvVenueName)
            if (tvVenueName != null) {
                toolbar?.title = tvVenueName.text.toString()
            }

            toolbar?.setTitleTextColor(if (shown) Color.WHITE else Color.TRANSPARENT)
        }
    }

    private fun findToolbar() {
        if (toolbar == null) {
            toolbar = findViewById<View>(toolbarId) as MaterialToolbar
        }
    }

    init {
        isTitleEnabled = false
        val a = context.obtainStyledAttributes(
            attrs,
            com.google.android.material.R.styleable.CollapsingToolbarLayout, 0,
            com.google.android.material.R.style.Widget_Design_CollapsingToolbar
        )
        toolbarId = a.getResourceId(com.google.android.material.R.styleable.CollapsingToolbarLayout_toolbarId, -1)
        a.recycle()
    }
}