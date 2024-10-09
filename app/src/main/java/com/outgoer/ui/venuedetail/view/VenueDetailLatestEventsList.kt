package com.outgoer.ui.venuedetail.view

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R
import com.outgoer.api.venue.model.VenueEventInfo
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueDetailLatestEventListBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailLatestEventsList(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val latestEventsClickSubject: PublishSubject<VenueEventInfo> = PublishSubject.create()
    val latestEventsClick: Observable<VenueEventInfo> = latestEventsClickSubject.hide()

    private lateinit var binding: ViewVenueDetailLatestEventListBinding
    private lateinit var venueDetailLatestEventsAdapter: VenueDetailLatestEventsAdapter

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_detail_latest_event_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueDetailLatestEventListBinding.bind(view)

        venueDetailLatestEventsAdapter = VenueDetailLatestEventsAdapter(context)
        binding.latestEventRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = venueDetailLatestEventsAdapter
        }
        venueDetailLatestEventsAdapter.apply {
            latestEventsClick.subscribe { latestEventsClickSubject.onNext(it) }
        }
    }

    fun bind(listOfLatestEvent: List<VenueEventInfo>) {
        venueDetailLatestEventsAdapter.listOfLatestEvent = listOfLatestEvent
    }
}