package com.outgoer.ui.videorooms.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.live.model.LiveEventInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveVenueRoomAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val liveVenueRoomClickSubject: PublishSubject<LiveEventInfo> = PublishSubject.create()
    val liveVenueRoomClick: Observable<LiveEventInfo> = liveVenueRoomClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<LiveEventInfo>? = null
        set(listOfNotification) {
            field = listOfNotification
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.LiveVenueRoomViewItem(it))
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.LiveVenueRoomViewType.ordinal -> {
                LiveVenueRoomViewHolder(LiveVenueRoomView(context).apply {
                    liveVenueRoomClick.subscribe { liveVenueRoomClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.LiveVenueRoomViewItem -> {
                (holder.itemView as LiveVenueRoomView).bind(adapterItem.liveEventInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class LiveVenueRoomViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class LiveVenueRoomViewItem(val liveEventInfo: LiveEventInfo) : AdapterItem(ViewType.LiveVenueRoomViewType.ordinal)
    }

    private enum class ViewType {
        LiveVenueRoomViewType
    }
}