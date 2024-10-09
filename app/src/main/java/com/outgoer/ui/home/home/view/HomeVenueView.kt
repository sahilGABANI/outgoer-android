package com.outgoer.ui.home.home.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.VenueItemViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HomeVenueView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val venueDetailActionStateSubject: PublishSubject<VenueDetail> = PublishSubject.create()
    val venueDetailActionState: Observable<VenueDetail> = venueDetailActionStateSubject.hide()

    private var binding: VenueItemViewBinding? = null
    private lateinit var venueDetail: VenueDetail


    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.venue_item_view, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = VenueItemViewBinding.bind(view)

        binding?.apply {
            venueRelativeLayout.setOnClickListener {
                venueDetailActionStateSubject.onNext(venueDetail)
            }
        }
    }

    fun bind(venue: VenueDetail) {
        this.venueDetail = venue
        binding?.apply {
            Glide.with(context)
                .load(venue.coverImage)
                .placeholder(R.drawable.venue_placeholder)
                .centerCrop()
                .into(venueMediaRoundedImageView)


            Glide.with(context)
                .load(venue.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .centerCrop()
                .into(profileRoundedImageView)

            venueNameAppCompatTextView.text = venue.name
            venueRatingCount.text = venue.reviewAvg.toString()
            venueRatingBar.rating = venue.reviewAvg?.toFloat() ?: 0f
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}