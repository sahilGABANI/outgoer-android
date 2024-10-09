package com.outgoer.ui.latestevents.view

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
import com.outgoer.databinding.ViewLatestEventsBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LatestEventsView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val latestEventsViewClickSubject: PublishSubject<VenueEventInfo> = PublishSubject.create()
    val latestEventsViewClick: Observable<VenueEventInfo> = latestEventsViewClickSubject.hide()

    private var binding: ViewLatestEventsBinding? = null
    private var venueEventInfo: VenueEventInfo? = null

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_latest_events, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewLatestEventsBinding.bind(view)
        binding?.apply {
            cardContainer.throttleClicks().subscribeAndObserveOnMainThread {
                venueEventInfo?.let { it1 -> latestEventsViewClickSubject.onNext(it1) }
            }.autoDispose()
        }
    }

    fun bind(venueEventInfo: VenueEventInfo) {
        this.venueEventInfo = venueEventInfo
        binding?.apply {
            Glide.with(context)
                .load(venueEventInfo.eventImage ?: "")
                .placeholder(R.drawable.venue_placeholder)
                .error(R.drawable.venue_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .into(ivEventImage)
            tvEventDate.text = getFormattedDateForEvent(venueEventInfo.eventStartDate)
            tvEventName.text = venueEventInfo.eventName ?: ""
        }
    }
}