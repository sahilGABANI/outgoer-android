package com.outgoer.ui.venuedetail.view

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.OtherNearPlaceClickState
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.roundDoubleVal
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewOtherNearVenueBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class VenueDetailOtherNearPlacesView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val otherNearPlaceClickSubject: PublishSubject<OtherNearPlaceClickState> = PublishSubject.create()
    val otherNearPlaceClick: Observable<OtherNearPlaceClickState> = otherNearPlaceClickSubject.hide()

    private lateinit var binding: ViewOtherNearVenueBinding
    private lateinit var venueMapInfo: VenueMapInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserType by Delegates.notNull<String>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)
        loggedInUserType = loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType ?: ""

        val view = View.inflate(context, R.layout.view_other_near_venue, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewOtherNearVenueBinding.bind(view)

        binding.llVenueInfo.throttleClicks().subscribeAndObserveOnMainThread {
            otherNearPlaceClickSubject.onNext(OtherNearPlaceClickState.OtherNearPlaceClick(venueMapInfo))
        }.autoDispose()

        binding.ivFavourite.throttleClicks().subscribeAndObserveOnMainThread {
            var venueFavouriteStatus = venueMapInfo.venueFavouriteStatus
            venueFavouriteStatus = if (venueFavouriteStatus != null) {
                if (venueFavouriteStatus == 1) {
                    0
                } else {
                    1
                }
            } else {
                1
            }
            venueMapInfo.venueFavouriteStatus = venueFavouriteStatus
            updateFavouriteStatus()
            otherNearPlaceClickSubject.onNext(OtherNearPlaceClickState.AddRemoveVenueFavClick(venueMapInfo))
        }.autoDispose()

        binding.ivDirection.throttleClicks().subscribeAndObserveOnMainThread {
            otherNearPlaceClickSubject.onNext(OtherNearPlaceClickState.DirectionViewClick(venueMapInfo))
        }.autoDispose()
    }

    fun bind(venueMapInfo: VenueMapInfo) {
        this.venueMapInfo = venueMapInfo
        binding.apply {
            if (loggedInUserType == MapVenueUserType.USER.type) {
                binding.ivFavourite.visibility = View.VISIBLE
            } else {
                binding.ivFavourite.visibility = View.INVISIBLE
            }

            Glide.with(context)
                .load(venueMapInfo.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .error(R.drawable.venue_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.ivVenueImage)

            Glide.with(context)
                .load(venueMapInfo.category?.firstOrNull()?.thumbnailImage ?: "")
                .placeholder(R.drawable.venue_placeholder)
                .error(R.drawable.venue_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(binding.ivVenueCategoryImage)

            tvVenueName.text = venueMapInfo.username
            tvAddress.text = venueMapInfo.venueAddress ?: ""
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                tvDistance.text = if (venueMapInfo.distance != null) {
                    venueMapInfo.distance.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_miles))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_miles))
                }
            } else {
                tvDistance.text = if (venueMapInfo.distance != null) {
                    venueMapInfo.distance.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_kms))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_kms))
                }
            }
        }

        updateFavouriteStatus()
    }

    private fun updateFavouriteStatus() {
        binding.apply {
            val venueFavouriteStatus = venueMapInfo.venueFavouriteStatus
            if (venueFavouriteStatus != null) {
                if (venueFavouriteStatus == 1) {
                    binding.ivFavourite.setColorFilter(ContextCompat.getColor(getContext(), R.color.purple))
                    binding.ivFavourite.setImageResource(R.drawable.ic_favourite_venue_active)
                } else {
                    binding.ivFavourite.setColorFilter(ContextCompat.getColor(getContext(), R.color.md_white))
                    binding.ivFavourite.setImageResource(R.drawable.ic_favourite_venue_inactive)
                }
            } else {
                binding.ivFavourite.setImageResource(R.drawable.ic_favourite_venue_inactive)
            }
        }
    }
}