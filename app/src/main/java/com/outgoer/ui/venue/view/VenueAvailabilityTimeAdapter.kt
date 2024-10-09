package com.outgoer.ui.venue.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import com.outgoer.api.venue.model.VenueTimeSelectionClickState
import com.outgoer.ui.sponty.location.model.ResultResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueAvailabilityTimeAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueAvailableClickSubject: PublishSubject<VenueTimeSelectionClickState> = PublishSubject.create()
    val venueAvailableClick: Observable<VenueTimeSelectionClickState> = venueAvailableClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listoflocation: ArrayList<VenueAvailabilityRequest>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listoflocation?.forEach {
            adapterItems.add(AdapterItem.VenueAvailabilityTimeViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueAvailabilityTimeViewType.ordinal -> {
                VenueAvailabilityTimeViewHolder(VenueAvailabilityTimeView(context).apply {
                    venueAvailableClick.subscribe { venueAvailableClickSubject.onNext(it) }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueAvailabilityTimeViewItem -> {
                (holder.itemView as VenueAvailabilityTimeView).bind(adapterItem.venueAvailabilityRequest)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VenueAvailabilityTimeViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class  VenueAvailabilityTimeViewItem(val venueAvailabilityRequest: VenueAvailabilityRequest) :
            AdapterItem(ViewType.VenueAvailabilityTimeViewType.ordinal)
    }

    private enum class ViewType {
        VenueAvailabilityTimeViewType,
    }
}
