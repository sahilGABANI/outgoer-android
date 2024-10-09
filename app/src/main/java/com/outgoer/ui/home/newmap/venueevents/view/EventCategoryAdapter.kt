package com.outgoer.ui.home.newmap.venueevents.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.event.model.EventData
import com.outgoer.api.venue.model.VenueCategory
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EventCategoryAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueClickSubject: PublishSubject<VenueCategory> = PublishSubject.create()
    val venueClick: Observable<VenueCategory> = venueClickSubject.hide()


    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<VenueCategory>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.EventCategoryViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.EventCategoryViewType.ordinal -> {
                EventCategoryViewHolder(EventCategoryView(context).apply {
                    venueClick.subscribe { venueClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.EventCategoryViewItem -> {
                (holder.itemView as EventCategoryView).bind(adapterItem.venueCategory)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class EventCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class EventCategoryViewItem(val venueCategory: VenueCategory) : AdapterItem(ViewType.EventCategoryViewType.ordinal)
    }

    private enum class ViewType {
        EventCategoryViewType
    }

}