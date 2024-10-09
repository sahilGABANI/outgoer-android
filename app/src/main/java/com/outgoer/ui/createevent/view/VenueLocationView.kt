package com.outgoer.ui.createevent.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.roundDoubleVal
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueListBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.math.RoundingMode
import javax.inject.Inject

class VenueLocationView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val venueCategoryClickSubject: PublishSubject<VenueMapInfo> = PublishSubject.create()
    val venueCategoryClick: Observable<VenueMapInfo> = venueCategoryClickSubject.hide()

    private val venueFavoriteClickSubject: PublishSubject<VenueMapInfo> = PublishSubject.create()
    val venueFavoriteClick: Observable<VenueMapInfo> = venueFavoriteClickSubject.hide()

    private lateinit var binding: ViewVenueListBinding
    private lateinit var venueCategory: VenueMapInfo
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_list, this)
        OutgoerApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueListBinding.bind(view)

        binding.apply {
            cardContainer.throttleClicks().subscribeAndObserveOnMainThread {
                venueCategoryClickSubject.onNext(venueCategory)
            }.autoDispose()

            favoriteAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
                venueFavoriteClickSubject.onNext(venueCategory)
            }
        }
    }

    fun bind(venueCategory: VenueMapInfo) {
        this.venueCategory = venueCategory
        binding.apply {

            Glide.with(context)
                .load(venueCategory.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .into(ivPlaceImage)

            tvPlaceName.text = venueCategory.name
            tvPlaceRatingCount.text = venueCategory.reviewAvg.toString()
//            distanceAppCompatTextView.text =
//                venueCategory.distance?.toBigDecimal()?.setScale(1, RoundingMode.UP)?.toDouble()
//                    .toString().plus(" miles")
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                distanceAppCompatTextView.text = if (venueCategory.distance != 0.00) {
                    venueCategory.distance?.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_miles))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_miles))
                }
            } else {
                distanceAppCompatTextView.text = if (venueCategory.distance != 0.00) {
                    venueCategory.distance?.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_kms))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_kms))
                }
            }

            Glide.with(context)
                .load(if(venueCategory.venueFavouriteStatus?.equals(1) == true) {
                    R.drawable.ic_venue_review_ratingbar_star_filled
                } else {
                    R.drawable.ic_venue_review_ratingbar_star_empty
                }).into(binding.favoriteAppCompatImageView)

        }
    }
}