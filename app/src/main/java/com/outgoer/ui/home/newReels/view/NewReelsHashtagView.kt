package com.outgoer.ui.home.newReels.view

import android.content.Context
import android.graphics.Color
import android.view.View
import androidx.core.view.isVisible
import com.outgoer.R
import com.outgoer.api.reels.model.ReelsHashTagsItem
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.NewReelsHashtagViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewReelsHashtagView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val reelsHashtagItemClicksSubject: PublishSubject<ReelsHashTagsItem> = PublishSubject.create()
    val reelsHashtagItemClicks: Observable<ReelsHashTagsItem> = reelsHashtagItemClicksSubject.hide()

    private lateinit var binding: NewReelsHashtagViewBinding
    private lateinit var hashtagsItem: ReelsHashTagsItem

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.new_reels_hashtag_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = NewReelsHashtagViewBinding.bind(view)

        binding.apply {
            tvHashtag.throttleClicks().subscribeAndObserveOnMainThread {
                reelsHashtagItemClicksSubject.onNext(hashtagsItem)
            }
        }
    }

    fun bind(hashtagsItem: ReelsHashTagsItem, isReels: Boolean) {
        this.hashtagsItem = hashtagsItem

        if(isReels) {
            binding.tvHashtag.isVisible = true
            binding.tvHashtag.text = hashtagsItem.title
            binding.tvHashtag.background = null
            binding.tvHashtag.setPadding(4,0,4,0)
            binding.tvHashtag.setTextColor(resources.getColor(R.color.blue))
            binding.tvHashtag.setTextSize(9f)
        } else {
            binding.tvHashtag.isVisible = true
            binding.tvHashtag.text = hashtagsItem.title
        }
    }
}