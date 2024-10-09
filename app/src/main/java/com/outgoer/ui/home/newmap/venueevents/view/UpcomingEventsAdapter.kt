package com.outgoer.ui.home.newmap.venueevents.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.event.model.EventData
import com.outgoer.api.event.model.MutualFriends
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class UpcomingEventsAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val upcomingEventsViewClickSubject: PublishSubject<EventData> = PublishSubject.create()
    val upcomingEventsViewClick: Observable<EventData> = upcomingEventsViewClickSubject.hide()

    private val profileViewClickSubject: PublishSubject<MutualFriends> = PublishSubject.create()
    val profileViewClick: Observable<MutualFriends> = profileViewClickSubject.hide()

    private val profileListViewClickSubject: PublishSubject<ArrayList<MutualFriends>> = PublishSubject.create()
    val profileListViewClick: Observable<ArrayList<MutualFriends>> = profileListViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<EventData>? = null
        set(listOfUser) {
            field = listOfUser
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.UpcomingEventsItemTypeViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.UpcomingEventsViewItemType.ordinal -> {

                UpcomingEventsViewHolder(UpcomingEventsView(context).apply {
                    upcomingEventsViewClick.subscribe { upcomingEventsViewClickSubject.onNext(it) }
                    profileViewClick.subscribeAndObserveOnMainThread { profileViewClickSubject.onNext(it) }
                    profileListViewClick.subscribeAndObserveOnMainThread { profileListViewClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.UpcomingEventsItemTypeViewItem -> {
                (holder.itemView as UpcomingEventsView).bind(adapterItem.data)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class UpcomingEventsViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class UpcomingEventsItemTypeViewItem(val data: EventData) : AdapterItem(ViewType.UpcomingEventsViewItemType.ordinal)
    }

    private enum class ViewType {
        UpcomingEventsViewItemType
    }
}