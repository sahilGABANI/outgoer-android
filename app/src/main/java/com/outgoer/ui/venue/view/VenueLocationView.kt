package com.outgoer.ui.venue.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.api.event.model.GooglePlaces
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.VenueLocationItemBinding
import com.outgoer.ui.sponty.location.model.Predictions
import com.outgoer.ui.sponty.location.model.ResultResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueLocationView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val venueAvailableClickSubject: PublishSubject<ResultResponse> = PublishSubject.create()
    val venueAvailableClick: Observable<ResultResponse> = venueAvailableClickSubject.hide()

    private val googlePlacesClickSubject: PublishSubject<GooglePlaces> = PublishSubject.create()
    val googlePlacesClick: Observable<GooglePlaces> = googlePlacesClickSubject.hide()

    private val placeAvailableClickSubject: PublishSubject<Predictions> = PublishSubject.create()
    val placeAvailableClick: Observable<Predictions> = placeAvailableClickSubject.hide()

    private var binding: VenueLocationItemBinding? = null
    private lateinit var venueLocation: ResultResponse
    private lateinit var placeSearch: Predictions
    private lateinit var googlePlaces: GooglePlaces

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.venue_location_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = VenueLocationItemBinding.bind(view)

        binding?.apply {
            locationLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                if (this@VenueLocationView::venueLocation.isInitialized){
                    venueAvailableClickSubject.onNext(venueLocation)
                } else if (this@VenueLocationView::placeSearch.isInitialized) {
                    placeAvailableClickSubject.onNext(placeSearch)
                } else if (this@VenueLocationView::googlePlaces.isInitialized) {
                    googlePlacesClickSubject.onNext(googlePlaces)
                }
            }
        }
    }


    fun bindGoogle(placeSearch: GooglePlaces) {
        this.googlePlaces = placeSearch

        binding?.apply {
            locationAppCompatTextView.text = placeSearch.name
            locationAddressAppCompatTextView.text = placeSearch.venueAddress
        }
    }

    fun bind(placeSearch: ResultResponse) {
        this.venueLocation = placeSearch

        binding?.apply {
            locationAppCompatTextView.text = placeSearch.name
            locationAddressAppCompatTextView.text = placeSearch.formattedAddress
        }
    }

    fun bind(placeSearch: Predictions) {
        this.placeSearch = placeSearch

        binding?.apply {
//            locationAppCompatTextView.text = placeSearch.name
            locationAddressAppCompatTextView.text = placeSearch.description
        }
    }
}