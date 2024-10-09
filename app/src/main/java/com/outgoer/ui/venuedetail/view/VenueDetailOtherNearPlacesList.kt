package com.outgoer.ui.venuedetail.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.api.venue.model.OtherNearPlaceClickState
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueDetailOtherNearPlacesListBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailOtherNearPlacesList(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val otherNearPlaceClickSubject: PublishSubject<OtherNearPlaceClickState> = PublishSubject.create()
    val otherNearPlaceClick: Observable<OtherNearPlaceClickState> = otherNearPlaceClickSubject.hide()

    private lateinit var binding: ViewVenueDetailOtherNearPlacesListBinding
    private lateinit var venueDetailOtherNearPlacesAdapter: VenueDetailOtherNearPlacesAdapter

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_detail_other_near_places_list, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueDetailOtherNearPlacesListBinding.bind(view)

        venueDetailOtherNearPlacesAdapter = VenueDetailOtherNearPlacesAdapter(context)
        venueDetailOtherNearPlacesAdapter.apply {
            otherNearPlaceClick.subscribe { otherNearPlaceClickSubject.onNext(it) }
        }
        binding.otherNearPlacesRecyclerView.apply {
            adapter = venueDetailOtherNearPlacesAdapter
        }
    }

    fun bind(listOfNearPlaces: List<VenueMapInfo>) {
        venueDetailOtherNearPlacesAdapter.listOfNearPlaces = listOfNearPlaces

    }
}