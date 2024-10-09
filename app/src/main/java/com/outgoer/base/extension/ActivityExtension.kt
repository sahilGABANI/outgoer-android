@file:JvmName("ActivityExtension")

package com.outgoer.base.extension

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.outgoer.R

fun Activity.startActivityWithDefaultAnimation(intent: Intent) {
    startActivity(intent)
    overridePendingTransition(R.anim.activity_move_in_from_right, R.anim.activity_move_out_to_left)
}

fun Activity.startActivityForResultWithDefaultAnimation(intent: Intent, requestCode: Int) {
    startActivityForResult(intent, requestCode)
    overridePendingTransition(R.anim.activity_move_in_from_right, R.anim.activity_move_out_to_left)
}

fun Activity.startActivityWithFadeInAnimation(intent: Intent) {
    startActivity(intent)
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
}

fun Activity.startActivityForResultWithFadeInAnimation(intent: Intent, requestCode: Int) {
    startActivityForResult(intent, requestCode)
    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
}

fun Activity.startActivityWithBottomInAnimation(intent: Intent) {
    startActivity(intent)
    overridePendingTransition(R.anim.activity_move_in_from_bottom, R.anim.default_exit)
}

fun Activity.startActivityForResultWithBottomInAnimation(intent: Intent, requestCode: Int) {
    startActivityForResult(intent, requestCode)
    overridePendingTransition(R.anim.activity_move_in_from_bottom, R.anim.default_exit)
}

fun Activity.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Activity.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Activity.hideKeyboard(): Boolean {
    val view = window.currentFocus
    return hideKeyboard(window, view)
}

fun Activity.hideKeyboard(view: View): Boolean {
    return hideKeyboard(window, view)
}

private fun hideKeyboard(window: Window, view: View?): Boolean {
    if (view == null) {
        return false
    }
    val inputMethodManager =
        window.context.getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as? InputMethodManager
    return inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0) ?: false
}

/**
 * Sets the cursor at the end of this edit text and shows a keyboard
 */
fun Activity.focusKeyboard(editText: AppCompatEditText) {
    editText.requestFocus()
    editText.setSelection(editText.text.toString().length)
    showKeyboard(editText)
}

fun Activity.showKeyboard(view: View) {
    val manager = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    manager.showSoftInput(view, InputMethodManager.SHOW_FORCED)
}

inline fun <reified T : ViewModel> AppCompatActivity.getViewModelFromFactory(vmFactory: ViewModelProvider.Factory): T {
    return ViewModelProvider(this, vmFactory)[T::class.java]
}

inline fun <T : ViewBinding> AppCompatActivity.viewBinding(
    crossinline bindingInflater: (LayoutInflater) -> T
) = lazy(LazyThreadSafetyMode.NONE) {
    bindingInflater.invoke(layoutInflater)
}

fun hideSystemUI(window: Window) {
    window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
}

fun fullSystemUI(window: Window) {
    window.decorView.systemUiVisibility =
        (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
}