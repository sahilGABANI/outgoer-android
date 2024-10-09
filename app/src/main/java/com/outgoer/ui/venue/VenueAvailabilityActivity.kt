package com.outgoer.ui.venue

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityVenueAvailabilityBinding
import com.outgoer.ui.venue.view.VenueAvailabilityDayAdapter
import java.util.*
import javax.inject.Inject

class VenueAvailabilityActivity : BaseActivity() {

    private lateinit var binding: ActivityVenueAvailabilityBinding
    private var registerVenueRequest: RegisterVenueRequest? = null

    private var listofvenue: ArrayList<VenueAvailabilityRequest> = arrayListOf()
    lateinit var venueAvailabilityDayAdapter: VenueAvailabilityDayAdapter

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    companion object {
        val INTENT_REGISTER_VENUE = "INTENT_REGISTER_VENUE"
        fun getIntent(context: Context, registerVenueRequest: RegisterVenueRequest): Intent {

            var intent = Intent(context, VenueAvailabilityActivity::class.java)
            intent.putExtra(INTENT_REGISTER_VENUE, registerVenueRequest)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityVenueAvailabilityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        getVenueAvailability()
        initUI()
    }

    private fun initUI() {
        venueAvailabilityDayAdapter = VenueAvailabilityDayAdapter(this).apply {
            this.venueCategoryClick.subscribeAndObserveOnMainThread { state ->
                val lisOfOpenAt = arrayListOf<String>()
                val lisOfCloseAt = arrayListOf<String>()
                val current = listofvenue.find { it.dayName == state.first().dayName }
                state.forEach { data ->
                    lisOfOpenAt.add(data.openAt?.first().toString())
                    lisOfCloseAt.add( data.closeAt?.first().toString())
                }
                current?.openAt = lisOfOpenAt
                current?.closeAt = lisOfCloseAt
                current?.status = state.first().status
            }
        }
        intent?.let {
            registerVenueRequest = it.getParcelableExtra(INTENT_REGISTER_VENUE)
        }

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        registerVenueRequest = loggedInUserCache.getVenueRequest()
        registerVenueRequest?.vanueAvailibility?.let {
            if(it.size > 0)
                listofvenue = it
        }


        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            registerVenueRequest = loggedInUserCache.getVenueRequest()
            registerVenueRequest?.let {
                it.vanueAvailibility = listofvenue

                loggedInUserCache.setVenueRequest(it)

                startActivity(VenueMediaActivity.getIntent(this@VenueAvailabilityActivity, it))
            }
        }

        binding.rvAvailableDay.adapter = venueAvailabilityDayAdapter
        venueAvailabilityDayAdapter.listofDataItem = listofvenue
    }

    private fun getVenueAvailability() {
        listofvenue.add(VenueAvailabilityRequest(dayName = "Monday", openAt = arrayListOf("8:00AM"), closeAt = arrayListOf("8:00PM"), status = 0))
        listofvenue.add(VenueAvailabilityRequest(dayName = "Tuesday", openAt = arrayListOf("8:00AM"), closeAt = arrayListOf("8:00PM"), status = 0))
        listofvenue.add(VenueAvailabilityRequest(dayName = "Wednesday", openAt = arrayListOf("8:00AM"), closeAt = arrayListOf("8:00PM"), status = 0))
        listofvenue.add(VenueAvailabilityRequest(dayName = "Thursday", openAt = arrayListOf("8:00AM"), closeAt = arrayListOf("8:00PM"), status = 0))
        listofvenue.add(VenueAvailabilityRequest(dayName = "Friday", openAt = arrayListOf("8:00AM"), closeAt = arrayListOf("8:00PM"), status = 0))
        listofvenue.add(VenueAvailabilityRequest(dayName = "Saturday", openAt = arrayListOf("8:00AM"), closeAt = arrayListOf("8:00PM"), status = 0))
        listofvenue.add(VenueAvailabilityRequest(dayName = "Sunday", openAt = arrayListOf("8:00AM"), closeAt = arrayListOf("8:00PM"), status = 0))
    }
}