package com.outgoer.base.extension

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager

@SuppressLint("LongLogTag")
fun Context?.hideKeyboard() {
    if (this == null) {
        return
    }
    val inputMethodManager = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    var activity: Activity? = null
    if (this is Activity) {
        activity = this
    } else if (this is ContextWrapper) {
        val parentContext = this.baseContext
        if (parentContext is Activity) {
            activity = parentContext
        }
    }

    if (activity == null) {
        Log.w("Try to hide keyboard but context type is incorrect %s", this.javaClass.simpleName)
        return
    }

    if (activity.currentFocus != null) {
        inputMethodManager.hideSoftInputFromWindow(activity.currentFocus!!.windowToken, 0)
    } else {
        Log.w("Try to hide keyboard but there is no current focus view","")
    }
}