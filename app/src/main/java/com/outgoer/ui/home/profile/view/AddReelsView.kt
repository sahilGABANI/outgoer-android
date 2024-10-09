package com.outgoer.ui.home.profile.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewMyReelsAddBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AddReelsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val addReelsViewClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val addReelsViewClick: Observable<Unit> = addReelsViewClickSubject.hide()

    private lateinit var binding: ViewMyReelsAddBinding

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_my_reels_add, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewMyReelsAddBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                addReelsViewClickSubject.onNext(it)
            }.autoDispose()
        }
    }

    fun bind(data: String) {
    }
}