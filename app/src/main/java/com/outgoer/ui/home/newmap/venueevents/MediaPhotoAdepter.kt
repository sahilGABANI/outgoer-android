package com.outgoer.ui.home.newmap.venueevents

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MediaPhotoAdepter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mediaPhotoViewClickSubject: PublishSubject<String> = PublishSubject.create()
    val mediaPhotoViewClick: Observable<String> = mediaPhotoViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: ArrayList<String>? = null
        set(listOfSpotlightVideoInfo) {
            field = listOfSpotlightVideoInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.PhotosViewItem(it))
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PhotosViewItemType.ordinal -> {
                val mediaPhotosView = MediaPhotosView(context)
                mediaPhotosView.mediaPhotoViewClick.subscribe { mediaPhotoViewClickSubject.onNext(it) }
                PhotosViewHolder(mediaPhotosView)
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
            is AdapterItem.PhotosViewItem -> {
                (holder.itemView as MediaPhotosView).bind(adapterItem.reelInfo)
            }
        }
    }

    private class PhotosViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class PhotosViewItem(var reelInfo: String) :
            AdapterItem(ViewType.PhotosViewItemType.ordinal)
    }

    private enum class ViewType {
        PhotosViewItemType
    }
}