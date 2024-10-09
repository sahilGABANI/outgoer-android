package com.outgoer.ui.latestevents.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueEventInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LatestEventsAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val latestEventsViewClickSubject: PublishSubject<VenueEventInfo> = PublishSubject.create()
    val latestEventsViewClick: Observable<VenueEventInfo> = latestEventsViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<VenueEventInfo>? = null
        set(listOfUser) {
            field = listOfUser
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.LatestEventsViewItemTypeViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.LatestEventsViewItemType.ordinal -> {
                LatestEventsViewHolder(LatestEventsView(context).apply {
                    latestEventsViewClick.subscribe { latestEventsViewClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.LatestEventsViewItemTypeViewItem -> {
                (holder.itemView as LatestEventsView).bind(adapterItem.data)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class LatestEventsViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class LatestEventsViewItemTypeViewItem(val data: VenueEventInfo) : AdapterItem(ViewType.LatestEventsViewItemType.ordinal)
    }

    private enum class ViewType {
        LatestEventsViewItemType
    }
}