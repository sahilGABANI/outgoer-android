package com.outgoer.ui.newvenuedetail.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.venue.model.ReviewImage
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.EventAdsMediaViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReviewImageView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val clickMediaActionStateSubject: PublishSubject<ReviewImage> = PublishSubject.create()
    val clickMediaActionState: Observable<ReviewImage> = clickMediaActionStateSubject.hide()

    private var binding: EventAdsMediaViewBinding? = null

    private lateinit var reviewImage: ReviewImage

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.event_ads_media_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = EventAdsMediaViewBinding.bind(view)

        binding?.apply {
            addLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                clickMediaActionStateSubject.onNext(reviewImage)
            }.autoDispose()
        }
    }



    fun bind(media: ReviewImage) {
        this.reviewImage = media
        binding?.apply {
            mediaLinearLayout.visibility = View.VISIBLE
            addLinearLayout.visibility = View.GONE
            videoAppCompatImageView.visibility = View.GONE
            deleteAppCompatImageView.visibility = View.GONE

            Glide.with(context)
                .load(reviewImage.media)
                .placeholder(R.drawable.venue_placeholder)
                .error(R.drawable.venue_placeholder)
                .into(mediaRoundedImageView)
        }
    }


    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}