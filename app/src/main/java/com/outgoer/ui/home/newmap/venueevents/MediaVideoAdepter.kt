package com.outgoer.ui.home.newmap.venueevents

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.event.model.EventData
import com.outgoer.api.post.model.VideoViewClick
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MediaVideoAdepter (private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){

    private val mediaVideoViewClickSubject: PublishSubject<VideoViewClick> = PublishSubject.create()
    val mediaVideoViewClick: Observable<VideoViewClick> = mediaVideoViewClickSubject.hide()

     private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: ArrayList<EventData>? = null
        set(listOfSpotlightVideoInfo) {
            field = listOfSpotlightVideoInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.VideosViewItem(it))
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VideosViewItemType.ordinal -> {
                VideosViewHolder(MediaVideoView(context).apply {
                    mediaVideoViewClick.subscribe { mediaVideoViewClickSubject.onNext(it)}
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")

        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VideosViewItem -> {
                (holder.itemView as MediaVideoView).bind(adapterItem.eventData)
            }
        }
    }
    private class VideosViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class VideosViewItem(var eventData: EventData) :
            AdapterItem(ViewType.VideosViewItemType.ordinal)
    }

    private enum class ViewType {
        VideosViewItemType
    }
}