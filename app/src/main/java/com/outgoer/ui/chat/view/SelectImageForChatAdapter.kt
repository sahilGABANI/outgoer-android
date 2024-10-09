package com.outgoer.ui.chat.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.mediapicker.models.PhotoModel
import com.outgoer.mediapicker.models.VideoModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SelectImageForChatAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val imageClickSubject: PublishSubject<PhotoModel> = PublishSubject.create()
    val imageClick: Observable<PhotoModel> = imageClickSubject.hide()

    private val videoClickSubject: PublishSubject<VideoModel> = PublishSubject.create()
    val videoClick: Observable<VideoModel> = videoClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<PhotoModel>? = null
        set(listOfNearPlaces) {
            field = listOfNearPlaces
            updateAdapterItem()
        }

    var listOfVideoItems: List<VideoModel>? = null
        set(listOfVideoItems) {
            field = listOfVideoItems
            updateAdapterItem()
        }


    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfVideoItems?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.SelectVideoForChatViewItem(data))
            }
        }

        listOfDataItems?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.SelectImageForChatViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SelectImageForChatViewItemType.ordinal -> {
                SelectImageForChatViewHolder(SelectImageForChatView(context).apply {
                    imageClick.subscribeAndObserveOnMainThread {
                        imageClickSubject.onNext(it)
                    }
                    videoClick.subscribeAndObserveOnMainThread {
                        videoClickSubject.onNext(it)
                    }
                })
            }
            ViewType.SelectVideoForChatViewItemType.ordinal -> {
                SelectImageForChatViewHolder(SelectImageForChatView(context).apply {
                    videoClick.subscribeAndObserveOnMainThread {
                        videoClickSubject.onNext(it)
                    }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SelectImageForChatViewItem -> {
                (holder.itemView as SelectImageForChatView).bind(adapterItem.photoModel)
            }
            is AdapterItem.SelectVideoForChatViewItem -> {
                (holder.itemView as SelectImageForChatView).bindVideo(adapterItem.photoModel)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class SelectImageForChatViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SelectImageForChatViewItem(val photoModel: PhotoModel) : AdapterItem(ViewType.SelectImageForChatViewItemType.ordinal)
        data class SelectVideoForChatViewItem(val photoModel: VideoModel) : AdapterItem(ViewType.SelectVideoForChatViewItemType.ordinal)
    }

    private enum class ViewType {
        SelectImageForChatViewItemType,
        SelectVideoForChatViewItemType
    }
}