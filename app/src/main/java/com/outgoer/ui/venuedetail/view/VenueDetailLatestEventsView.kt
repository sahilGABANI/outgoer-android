package com.outgoer.ui.venuedetail.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.outgoer.R
import com.outgoer.api.venue.model.VenueEventInfo
import com.outgoer.base.extension.getFormattedDateForEvent
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueDetailLatestEventsBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailLatestEventsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val latestEventsClickSubject: PublishSubject<VenueEventInfo> = PublishSubject.create()
    val latestEventsClick: Observable<VenueEventInfo> = latestEventsClickSubject.hide()

    private lateinit var binding: ViewVenueDetailLatestEventsBinding
    private lateinit var venueEventInfo: VenueEventInfo

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_detail_latest_events, this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueDetailLatestEventsBinding.bind(view)

        binding.ivEventPlace.throttleClicks().subscribeAndObserveOnMainThread {
            latestEventsClickSubject.onNext(venueEventInfo)
        }
    }

    fun bind(venueEventInfo: VenueEventInfo) {
        this.venueEventInfo = venueEventInfo
        binding.apply {
            Glide.with(context)
                .load(venueEventInfo.eventImage ?: "")
                .placeholder(R.drawable.venue_placeholder)
                .error(R.drawable.venue_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(ivEventPlace)
            tvEventDate.text = getFormattedDateForEvent(venueEventInfo.eventStartDate)
            tvEventName.text = venueEventInfo.eventName ?: ""
        }
    }
}