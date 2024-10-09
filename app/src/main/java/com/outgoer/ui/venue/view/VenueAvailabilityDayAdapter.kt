package com.outgoer.ui.venue.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueAvailabilityDayAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val venueAvailableClickSubject: PublishSubject<VenueAvailabilityRequest> = PublishSubject.create()
    val venueAvailableClick: Observable<VenueAvailabilityRequest> = venueAvailableClickSubject.hide()

    private val venueCategoryClickSubject: PublishSubject<ArrayList<VenueAvailabilityRequest>> =
        PublishSubject.create()
    val venueCategoryClick: Observable<ArrayList<VenueAvailabilityRequest>> = venueCategoryClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listofDataItem: ArrayList<VenueAvailabilityRequest>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listofDataItem?.forEach {
            adapterItems.add(AdapterItem.VenueAvailabilityDayViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueAvailabilityDayViewType.ordinal -> {
                VenueAvailabilityDayViewHolder(VenueAvailabilityDayView(context).apply {
                    venueAvailableClick.subscribe { venueAvailableClickSubject.onNext(it) }
                    venueCategoryClick.subscribe { venueCategoryClickSubject.onNext(it) }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueAvailabilityDayViewItem -> {
                (holder.itemView as VenueAvailabilityDayView).bind(adapterItem.venueReview)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VenueAvailabilityDayViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class  VenueAvailabilityDayViewItem(val venueReview: VenueAvailabilityRequest) :
            AdapterItem(ViewType.VenueAvailabilityDayViewType.ordinal)
    }

    private enum class ViewType {
        VenueAvailabilityDayViewType,
    }
}