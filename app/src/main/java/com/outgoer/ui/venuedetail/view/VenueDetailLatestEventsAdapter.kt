package com.outgoer.ui.venuedetail.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueEventInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailLatestEventsAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val latestEventsClickSubject: PublishSubject<VenueEventInfo> = PublishSubject.create()
    val latestEventsClick: Observable<VenueEventInfo> = latestEventsClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfLatestEvent: List<VenueEventInfo>? = null
        set(listOfLatestEvent) {
            field = listOfLatestEvent
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfLatestEvent?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.LatestEventViewItemTypeItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.LatestEventViewItemType.ordinal -> {
                LatestEventViewHolder(VenueDetailLatestEventsView(context).apply {
                    latestEventsClick.subscribe { latestEventsClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.LatestEventViewItemTypeItem -> {
                (holder.itemView as VenueDetailLatestEventsView).bind(adapterItem.venueEventInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class LatestEventViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class LatestEventViewItemTypeItem(val venueEventInfo: VenueEventInfo) : AdapterItem(ViewType.LatestEventViewItemType.ordinal)
    }

    private enum class ViewType {
        LatestEventViewItemType
    }
}