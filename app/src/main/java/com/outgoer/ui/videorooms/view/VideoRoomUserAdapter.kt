package com.outgoer.ui.videorooms.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.live.model.LiveEventInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VideoRoomUserAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val videoRoomCreateNewClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val videoRoomCreateNewClick: Observable<Unit> = videoRoomCreateNewClickSubject.hide()

    private val videoRoomUserClickSubject: PublishSubject<LiveEventInfo> = PublishSubject.create()
    val videoRoomUserClick: Observable<LiveEventInfo> = videoRoomUserClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<LiveEventInfo>? = null
        set(listOfNotification) {
            field = listOfNotification
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        adapterItems.add(AdapterItem.VideoRoomCreateNewViewItem)
        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.VideoRoomUserViewItem(it))
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VideoRoomCreateNewViewType.ordinal -> {
                VideoRoomCreateNewViewHolder(VideoRoomCreateNewView(context).apply {
                    videoRoomCreateNewClick.subscribe { videoRoomCreateNewClickSubject.onNext(it) }
                })
            }
            ViewType.VideoRoomUserViewType.ordinal -> {
                VideoRoomUserViewHolder(VideoRoomUserView(context).apply {
                    videoRoomUserClick.subscribe { videoRoomUserClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VideoRoomCreateNewViewItem -> {
                (holder.itemView as VideoRoomCreateNewView).bind()
            }
            is AdapterItem.VideoRoomUserViewItem -> {
                (holder.itemView as VideoRoomUserView).bind(adapterItem.liveEventInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VideoRoomCreateNewViewHolder(view: View) : RecyclerView.ViewHolder(view)
    private class VideoRoomUserViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        object VideoRoomCreateNewViewItem : AdapterItem(ViewType.VideoRoomCreateNewViewType.ordinal)
        data class VideoRoomUserViewItem(val liveEventInfo: LiveEventInfo) : AdapterItem(ViewType.VideoRoomUserViewType.ordinal)
    }

    private enum class ViewType {
        VideoRoomCreateNewViewType,
        VideoRoomUserViewType
    }
}