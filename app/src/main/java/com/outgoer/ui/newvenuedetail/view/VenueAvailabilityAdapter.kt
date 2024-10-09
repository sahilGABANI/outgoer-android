package com.outgoer.ui.newvenuedetail.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueAvailabilityAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueAvailableClickSubject: PublishSubject<VenueAvailabilityRequest> = PublishSubject.create()
    val venueAvailableClick: Observable<VenueAvailabilityRequest> = venueAvailableClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listofAvailable: ArrayList<VenueAvailabilityRequest>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listofAvailable?.forEach {
            adapterItems.add(AdapterItem.VenueAvailableViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueAvailableViewType.ordinal -> {
                VenueAvailableViewHolder(VenueAvailabilityView(context).apply {
                    venueAvailableClick.subscribe { venueAvailableClickSubject.onNext(it) }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueAvailableViewItem -> {
                (holder.itemView as VenueAvailabilityView).bind(adapterItem.venueReview)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VenueAvailableViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class VenueAvailableViewItem(val venueReview: VenueAvailabilityRequest) : AdapterItem(ViewType.VenueAvailableViewType.ordinal)
    }

    private enum class ViewType {
        VenueAvailableViewType,
    }
}