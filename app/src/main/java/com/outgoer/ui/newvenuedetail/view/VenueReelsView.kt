package com.outgoer.ui.newvenuedetail.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.outgoer.R
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueReelsBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueReelsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: ViewVenueReelsBinding? = null
    private val venueDetailReelsViewClickSubject: PublishSubject<ReelInfo> = PublishSubject.create()
    val venueDetailReelsViewClick: Observable<ReelInfo> = venueDetailReelsViewClickSubject.hide()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_reels, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueReelsBinding.bind(view)

        binding?.apply {

        }
    }

    fun bind(reelInfo: ReelInfo) {
        binding?.apply {

            println("reelInfo.gifthumbnailUrl: " + reelInfo.gifthumbnailUrl)
            Glide.with(context)
                .asGif()
                .load(reelInfo.gifthumbnailUrl)
                .centerCrop()
                .placeholder(R.drawable.black_background)
                .listener(object: RequestListener<GifDrawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<GifDrawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressImageLoading.visibility = View.GONE
                        return false;
                    }

                    override fun onResourceReady(
                        resource: GifDrawable?,
                        model: Any?,
                        target: Target<GifDrawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        progressImageLoading.visibility = View.GONE
                        return false;
                    }

                })
                .error(R.drawable.venue_placeholder)
                .into(reelsThumbnailImageView)

            tvViewerCount.text = reelInfo.watchCount.toString()

            flReels.throttleClicks().subscribeAndObserveOnMainThread {
                venueDetailReelsViewClickSubject.onNext(reelInfo)
            }.autoDispose()
        }
    }
}