package com.outgoer.ui.newvenuedetail.view

import android.content.Context
import android.text.Html
import android.view.View
import androidx.core.view.isVisible
import com.outgoer.R
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.VenueAvailabilityItemBinding

class VenueAvailabilityView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private var binding: VenueAvailabilityItemBinding? = null
    private lateinit var venueavailability: VenueAvailabilityRequest
    private lateinit var venueTimeAdapter: VenueTimeAdapter


    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.venue_availability_item, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = VenueAvailabilityItemBinding.bind(view)
    }

    fun bind(venueAvailability: VenueAvailabilityRequest) {
        this.venueavailability = venueAvailability

        binding?.apply {
            venueTimeAdapter = VenueTimeAdapter(context)

            val listOfTime = arrayListOf<VenueAvailabilityRequest>()

            venueAvailability.openAt?.forEach {
                listOfTime.add(VenueAvailabilityRequest(venueAvailability.dayName, arrayListOf(it),
                    arrayListOf(),venueAvailability.status,venueAvailability.id
                ))
            }
            for(i in 0..listOfTime.size -1) {
                listOfTime[i].closeAt = venueAvailability.closeAt?.get(i)?.let { arrayListOf(it) }
            }

            rvVenueTime.adapter = venueTimeAdapter
            venueTimeAdapter.listofAvailable = listOfTime

            if(venueAvailability.openAt.isNullOrEmpty() || venueAvailability.closeAt.isNullOrEmpty()) {
                closeAppCompatTextView.isVisible = true
                viewClose.visibility = View.VISIBLE
                rvVenueTime.isVisible = false
            } else {
                viewClose.visibility = View.GONE
                closeAppCompatTextView.isVisible = false
                rvVenueTime.isVisible = true
            }

            timeAppCompatTextView.text = venueAvailability.dayName
        }
    }
}