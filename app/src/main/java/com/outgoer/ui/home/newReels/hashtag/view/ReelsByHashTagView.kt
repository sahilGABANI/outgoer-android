package com.outgoer.ui.home.newReels.hashtag.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewReelsByHashtagBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReelsByHashTagView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val reelsHashtagItemClicksSubject: PublishSubject<ReelInfo> = PublishSubject.create()
    val reelsHashtagItemClicks: Observable<ReelInfo> = reelsHashtagItemClicksSubject.hide()

    private lateinit var binding: ViewReelsByHashtagBinding
    private lateinit var reelInfo: ReelInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_reels_by_hashtag, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewReelsByHashtagBinding.bind(view)

        binding.apply {
            ivMedia.throttleClicks().subscribeAndObserveOnMainThread {
                reelsHashtagItemClicksSubject.onNext(reelInfo)
            }
        }
    }

    fun bind(reelInfo: ReelInfo) {
        this.reelInfo = reelInfo

       Glide.with(this)
           .asGif()
           .load(reelInfo.gifthumbnailUrl)
           .placeholder(R.drawable.ic_logo_placeholder)
           .into(binding.ivMedia)

    }
}