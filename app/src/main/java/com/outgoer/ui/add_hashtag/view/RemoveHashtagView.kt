package com.outgoer.ui.add_hashtag.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.outgoer.R
import com.outgoer.api.hashtag.model.HashtagResponse
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.NewReelsHashtagViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class RemoveHashtagView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val hashtagClickSubject: PublishSubject<String> = PublishSubject.create()
    val hashtagClick: Observable<String> = hashtagClickSubject.hide()

    private lateinit var binding: NewReelsHashtagViewBinding
    private lateinit var hashtagResponse: String

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.new_reels_hashtag_view, this)
//        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = NewReelsHashtagViewBinding.bind(view)

        binding.apply {
            removesAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                hashtagClickSubject.onNext(hashtagResponse)
            }.autoDispose()
        }
    }

    fun bind(hashtagRes: String) {
        this.hashtagResponse = hashtagRes
        binding.apply {
            tvHashtag.isVisible = false
            removeHashtagConstraintLayout.isVisible = false
            removeHashtagsConstraintLayout.isVisible = true
            removesAppCompatImageView.isVisible = true

            hashTagsAppCompatTextView.text = hashtagRes
        }
    }
}