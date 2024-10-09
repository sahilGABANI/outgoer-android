package com.outgoer.base.view

import android.app.Dialog
import android.app.ProgressDialog
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.outgoer.R

object CustomProgressDialog {

    fun showProgressSpinner(context: Context, message: String): ProgressDialog {
        val dialog = ProgressDialog(context)
        try {
            dialog.setMessage(message)
            dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
            dialog.setCancelable(false)
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dialog
    }

    fun showProgressHorizontal(context: Context, message: String): ProgressDialog {
        val dialog = ProgressDialog(context)
        try {
            dialog.setMessage(message)
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            dialog.setCancelable(false)
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return dialog
    }

    fun hideProgress(dialog: ProgressDialog?) {
        try {
            dialog?.let {
                if (it.isShowing) {
                    it.dismiss()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showAVLProgressDialog(context: Context): Dialog {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val window = dialog.window
        window?.let {
            it.setBackgroundDrawableResource(android.R.color.transparent)
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                it.statusBarColor = ContextCompat.getColor(context, android.R.color.transparent)
                it.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
        val view = View.inflate(context, R.layout.dialog_loader, null)
        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()
        return dialog
    }

    fun hideAVLProgressDialog(dialog: Dialog?) {
        dialog?.let {
            if (it.isShowing) {
                it.dismiss()
            }
        }
    }
}