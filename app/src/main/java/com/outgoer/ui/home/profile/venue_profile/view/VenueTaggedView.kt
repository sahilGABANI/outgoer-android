package com.outgoer.ui.home.profile.venue_profile.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.NewCategoryViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueTaggedView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val eventCategoryActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val eventCategoryActionState: Observable<String> = eventCategoryActionStateSubject.hide()

    private var binding: NewCategoryViewBinding? = null

    private lateinit var venueTagName: String

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.new_category_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = NewCategoryViewBinding.bind(view)

        binding?.apply {
            cvHashtagContainer.throttleClicks().subscribeAndObserveOnMainThread {
            }.autoDispose()
        }
    }

    fun bind(tagName: String) {
        this.venueTagName = tagName
        binding?.apply {
            logoAppCompatImageView.visibility = View.GONE
            tvHashtag.text = tagName
        }
    }


    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}