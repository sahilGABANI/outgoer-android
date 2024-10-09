package com.outgoer.ui.home.profile.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewMyReelsBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class UserReelsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val reelsViewClickSubject: PublishSubject<ReelInfo> = PublishSubject.create()
    val reelsViewClick: Observable<ReelInfo> = reelsViewClickSubject.hide()

    private lateinit var binding: ViewMyReelsBinding
    private lateinit var reelInfo: ReelInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_my_reels, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewMyReelsBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                reelsViewClickSubject.onNext(reelInfo)
            }.autoDispose()
        }
    }

    fun bind(reelInfo: ReelInfo) {
        this.reelInfo = reelInfo
        Glide.with(context)
            .load(reelInfo.thumbnailUrl)
            .placeholder(R.drawable.venue_placeholder)
            .into(binding.ivReelThumb)
        binding.tvReelName.text = reelInfo.user?.username ?: ""
    }
}