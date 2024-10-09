package com.outgoer.ui.venue.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.outgoer.R
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import com.outgoer.api.venue.model.VenueTimeSelectionClickState
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewVenueAvailabilityDayBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueAvailabilityDayView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val venueCategoryClickSubject: PublishSubject<ArrayList<VenueAvailabilityRequest>> =
        PublishSubject.create()
    val venueCategoryClick: Observable<ArrayList<VenueAvailabilityRequest>> = venueCategoryClickSubject.hide()

    private lateinit var binding: ViewVenueAvailabilityDayBinding

    private lateinit var venueAvailabilityRequest: VenueAvailabilityRequest
    lateinit var adapter: VenueAvailabilityTimeAdapter
    private var itemId = 0

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_venue_availability_day, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewVenueAvailabilityDayBinding.bind(view)

        binding.apply {

        }
    }

    fun bind(venue: VenueAvailabilityRequest) {
        venueAvailabilityRequest = venue
        binding.apply {

            val listOfTime = arrayListOf<VenueAvailabilityRequest>()
            venue.openAt?.forEach {
                listOfTime.add(VenueAvailabilityRequest(venue.dayName, arrayListOf(it),
                    arrayListOf(),venue.status,itemId
                ))
                itemId+=1
            }
            for(i in 0..listOfTime.size -1) {
                listOfTime[i].closeAt = venue.closeAt?.get(i)?.let { arrayListOf(it) }
            }

            itemId = venue.id
            adapter = VenueAvailabilityTimeAdapter(context).apply {
                this.venueAvailableClick.subscribeAndObserveOnMainThread { state ->
                    when (state) {
                        is VenueTimeSelectionClickState.CloseAtClicks -> {
                            val data = listOfTime.find { it.id == state.timeInfo.id }
                            val index = listOfTime.indexOf(data)
                            listOfTime[index].closeAt = state.timeInfo.closeAt
                            adapter.listoflocation = listOfTime
                            adapter.listoflocation?.let {
                                venueCategoryClickSubject.onNext(it)
                            }
                        }
                        is VenueTimeSelectionClickState.OpensAtClick -> {
                            val data = listOfTime.find { it.id == state.timeInfo.id }
                            val index = listOfTime.indexOf(data)
                            listOfTime[index].openAt = state.timeInfo.openAt
                            adapter.listoflocation = listOfTime
                            adapter.listoflocation?.let {
                                venueCategoryClickSubject.onNext(it)
                            }

                        }
                        is VenueTimeSelectionClickState.RemoveClicks -> {
                            listOfTime.removeIf { data -> data.id == state.timeInfo.id }
                            adapter.listoflocation = listOfTime
                            adapter.listoflocation?.let {
                                venueCategoryClickSubject.onNext(it)
                            }
                        }
                    }

                }.autoDispose()
            }
            rvVenueTime.adapter = adapter
            adapter.listoflocation = listOfTime
            tvDayTitle.text = venue.dayName
            mondaySwitch.isChecked = venue.status == 1
            rvVenueTime.isVisible = venue.status == 1
            ivAdd.isVisible = venue.status == 1
            mondaySwitch.setOnCheckedChangeListener { compoundButton, b ->
                listOfTime.forEach {
                    it.status = if (b) 1 else 0
                }
                adapter.listoflocation = listOfTime

                rvVenueTime.visibility = if (b) View.VISIBLE else View.GONE
                ivAdd.visibility = if (b) View.VISIBLE else View.GONE
                adapter.listoflocation?.let {
                    venueCategoryClickSubject.onNext(it)
                }
            }

            ivAdd.throttleClicks().subscribeAndObserveOnMainThread {
                itemId += 1
                val new = VenueAvailabilityRequest(
                    dayName = venue.dayName,
                    openAt = arrayListOf("8:00AM"),
                    closeAt = arrayListOf("8:00PM"),
                    status = if(binding.mondaySwitch.isChecked) 1 else 0,
                    id = itemId
                )
                listOfTime.add(new)
                adapter.listoflocation = listOfTime
                adapter.listoflocation?.let {
                    venueCategoryClickSubject.onNext(it)
                }
            }.autoDispose()
        }
    }
}