package com.outgoer.ui.venuedetail.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.outgoer.R
import com.outgoer.api.venue.model.VenueGalleryItem
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueDetailGalleryBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailGalleryView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val venueDetailGalleryViewClickSubject: PublishSubject<VenueGalleryItem> = PublishSubject.create()
    val venueDetailGalleryViewClick: Observable<VenueGalleryItem> = venueDetailGalleryViewClickSubject.hide()

    private lateinit var binding: ViewVenueDetailGalleryBinding
    private lateinit var venueGalleryItem: VenueGalleryItem

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_detail_gallery, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueDetailGalleryBinding.bind(view)

        binding.flPost.throttleClicks().subscribeAndObserveOnMainThread {
            venueDetailGalleryViewClickSubject.onNext(venueGalleryItem)
        }.autoDispose()
    }

    fun bind(venueGalleryItem: VenueGalleryItem) {
        this.venueGalleryItem = venueGalleryItem
        binding.apply {

            val mediaUrl = when (venueGalleryItem.type) {
                1 -> {
                    ivMediaTypeVideo.visibility = View.GONE
                    venueGalleryItem.media
                }
                2 -> {
                    ivMediaTypeVideo.visibility = View.VISIBLE
                    venueGalleryItem.thumbnailUrl
                }
                else -> {
                    ivMediaTypeVideo.visibility = View.GONE
                    ""
                }
            }

            Glide.with(context)
                .load(mediaUrl)
                .placeholder(R.drawable.venue_placeholder)
                .error(R.drawable.venue_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.ivMedia)
        }
    }
}