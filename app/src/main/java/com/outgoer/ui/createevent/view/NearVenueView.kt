package com.outgoer.ui.createevent.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.event.model.GooglePlaces
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.roundDoubleVal
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.NewVenueViewBinding
import com.outgoer.databinding.ViewNewVenueListBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.math.RoundingMode
import javax.inject.Inject

class NearVenueView(context: Context) : ConstraintLayoutWithLifecycle(context) {


    private var binding: NewVenueViewBinding? = null
    private lateinit var venueMapInfo: VenueMapInfo
    private lateinit var googlePlaces: GooglePlaces
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private val venueClickSubject: PublishSubject<VenueMapInfo> = PublishSubject.create()
    val venueClick: Observable<VenueMapInfo> = venueClickSubject.hide()

    private val googlePlaceClickSubject: PublishSubject<GooglePlaces> = PublishSubject.create()
    val googlePlaceClick: Observable<GooglePlaces> = googlePlaceClickSubject.hide()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.new_venue_view, this)
        OutgoerApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = NewVenueViewBinding.bind(view)
    }

    fun bindGoogle(venueMapInfo: GooglePlaces) {

        this.googlePlaces = venueMapInfo
        binding?.apply {
            cardContainer.throttleClicks().subscribeAndObserveOnMainThread {
                googlePlaceClickSubject.onNext(venueMapInfo)
            }.autoDispose()

            Glide.with(context)
                .load(venueMapInfo.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .into(ivPlaceImage)

            tvPlaceName.text = venueMapInfo.name
            tvPlaceRatingCount.text = venueMapInfo.reviewAvg.toString()
            dotAppCompatImageView.visibility = View.GONE
            distanceAppCompatTextView.visibility = View.GONE
        }
    }

    fun bind(venueMapInfo: VenueMapInfo) {
        this.venueMapInfo = venueMapInfo
        binding?.apply {
            cardContainer.throttleClicks().subscribeAndObserveOnMainThread {
                venueClickSubject.onNext(venueMapInfo)
            }.autoDispose()

            Glide.with(context)
                .load(venueMapInfo.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .into(ivPlaceImage)

            tvPlaceName.text = venueMapInfo.name
            tvPlaceRatingCount.text = venueMapInfo.reviewAvg.toString()
//            distanceAppCompatTextView.text =
//                venueMapInfo.distance?.toBigDecimal()?.setScale(1, RoundingMode.UP)?.toDouble()
//                    .toString().plus(" miles")
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                distanceAppCompatTextView.text = if (venueMapInfo.distance != 0.00) {
                    venueMapInfo.distance?.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_miles))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_miles))
                }
            } else {
                distanceAppCompatTextView.text = if (venueMapInfo.distance != 0.00) {
                    venueMapInfo.distance?.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_kms))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_kms))
                }
            }
        }
    }


    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}