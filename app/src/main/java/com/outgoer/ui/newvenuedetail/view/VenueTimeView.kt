package com.outgoer.ui.newvenuedetail.view

import android.content.Context
import android.text.Html
import android.text.format.DateFormat
import android.view.View
import com.outgoer.R
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueTimeBinding
import java.util.*

class VenueTimeView(context: Context) : ConstraintLayoutWithLifecycle(context)  {

    private var binding: ViewVenueTimeBinding? = null
    private lateinit var venueavailability: VenueAvailabilityRequest

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_time, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueTimeBinding.bind(view)
    }

    fun bind(venueAvailability: VenueAvailabilityRequest) {
        if (venueAvailability != null) {
            this.venueavailability = venueAvailability
        }

        binding?.apply {

            val date: Calendar = Calendar.getInstance()
            val dayToday: String = DateFormat.format("EEEE", date).toString()
            if(venueAvailability?.openAt?.first().isNullOrEmpty()  || venueAvailability?.closeAt?.first().isNullOrEmpty() || venueAvailability.status == 0 ) {
                val sb = StringBuilder()
                sb.append(" <font color='red'>")
                sb.append("Closed!")
                sb.append("</font>")
                timeAppCompatTextView.text = Html.fromHtml(sb.toString())
            } else {
                if(dayToday.equals(venueAvailability.dayName)) {
                    val sb = StringBuilder()
                    sb.append(" <font color='green'>")
                    sb.append("${if(venueAvailability?.openAt?.first() != null) "${venueAvailability.openAt?.first()} - ${venueAvailability.closeAt?.first()}" else ""}")
                    sb.append("</font>")
                    timeAppCompatTextView.text = Html.fromHtml(sb.toString())
                } else {
                    val sb = StringBuilder()
                    sb.append(" <font color='#AEAEB2'>")
                    sb.append("${if(venueAvailability?.openAt?.first() != null) "${venueAvailability.openAt?.first()} - ${venueAvailability.closeAt?.first()}" else ""}")
                    sb.append("</font>")
                    timeAppCompatTextView.text = Html.fromHtml(sb.toString())
                }
            }

        }
    }
}