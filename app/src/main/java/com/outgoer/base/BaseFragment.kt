package com.outgoer.base

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import com.outgoer.utils.UiUtils
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber

abstract class BaseFragment : Fragment() {

    private val compositeDisposable = CompositeDisposable()

    lateinit var baseActivity: Activity

    var PLACE_API_KEY = "AIzaSyCC_Nu3RvrGB8WId3Wazw_VWoD17u2eGI4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        context.let {
            baseActivity = (context as Activity)
        }
    }

    fun Disposable.autoDispose() {
        compositeDisposable.add(this)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }

    @CallSuper
    open fun onBackPressed(): Boolean {
        Timber.i("[%s] onBackPressed", javaClass.simpleName)
        if (isAdded) {
            val view = baseActivity.currentFocus
            if (view != null && view is EditText) {
                UiUtils.hideKeyboard(baseActivity.window)
                return false
            }
        }
        return false
    }
}