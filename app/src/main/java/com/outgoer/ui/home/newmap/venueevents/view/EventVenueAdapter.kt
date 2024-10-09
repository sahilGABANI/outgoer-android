package com.outgoer.ui.home.newmap.venueevents.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.event.model.EventData
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EventVenueAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueClickSubject: PublishSubject<EventData> = PublishSubject.create()
    val venueClick: Observable<EventData> = venueClickSubject.hide()


    private var adapterItems = listOf<AdapterItem>()

    var isAllSelected = true
    var listOfDataItems: List<EventData>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.VenueItemViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueItemViewType.ordinal -> {
                VenueCategoryViewHolder(EventVenueView(context).apply {
                    venueClick.subscribe { venueClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueItemViewItem -> {
                (holder.itemView as EventVenueView).bind(adapterItem.venueCategory)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VenueCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class VenueItemViewItem(val venueCategory: EventData) : AdapterItem(ViewType.VenueItemViewType.ordinal)
    }

    private enum class ViewType {
        VenueItemViewType
    }

}