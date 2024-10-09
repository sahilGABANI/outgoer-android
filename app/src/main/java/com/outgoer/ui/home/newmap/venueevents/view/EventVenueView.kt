package com.outgoer.ui.home.newmap.venueevents.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.event.model.EventData
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.roundDoubleVal
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewNewVenueListBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.math.RoundingMode
import javax.inject.Inject

class EventVenueView(context: Context) : ConstraintLayoutWithLifecycle(context) {


    private var binding: ViewNewVenueListBinding? = null
    private lateinit var venueMapInfo: EventData
    private lateinit var mediaUrl: String
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private val venueClickSubject: PublishSubject<EventData> = PublishSubject.create()
    val venueClick: Observable<EventData> = venueClickSubject.hide()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_venue_list, this)
        OutgoerApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewVenueListBinding.bind(view)

        binding?.apply {
            cardContainer.throttleClicks().subscribeAndObserveOnMainThread {
                venueClickSubject.onNext(venueMapInfo)
            }.autoDispose()
        }
    }


    fun bind(venueMapInfo: EventData) {
        this.venueMapInfo = venueMapInfo
        binding?.apply {

            Glide.with(context)
                .load(venueMapInfo.firstMedia?.image)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(ivPlaceImage)

            //            ivVenueCategoryImage.visibility = View.GONE

            tvPlaceName.text = venueMapInfo.name
            tvPlaceDescription.text = venueMapInfo.location
            tvPlaceRatingCount.text = venueMapInfo.reviewAvg.toString()
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