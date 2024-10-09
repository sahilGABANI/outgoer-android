package com.outgoer.ui.venuedetail.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueGalleryItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailGalleryAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueDetailGalleryViewClickSubject: PublishSubject<VenueGalleryItem> = PublishSubject.create()
    val venueDetailGalleryViewClick: Observable<VenueGalleryItem> = venueDetailGalleryViewClickSubject.hide()

    private val venueDetailGalleryCountViewClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val venueDetailGalleryCountViewClick: Observable<Unit> = venueDetailGalleryCountViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var galleryCount: Int? = 0
        set(galleryCount) {
            field = galleryCount
        }

    var listOfDataItems: List<VenueGalleryItem>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.let { mList ->
            if (mList.size > 4) {
                for (i in mList.indices) {
                    if (i < 4) {
                        adapterItems.add(AdapterItem.VenueDetailGalleryViewItem(mList[i]))
                    } else if (i == 4) {
                        adapterItems.add(AdapterItem.VenueDetailGalleryWithCountViewItem(mList[i]))
                    }
                }
            } else {
                mList.forEach {
                    adapterItems.add(AdapterItem.VenueDetailGalleryViewItem(it))
                }
            }
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueDetailGalleryViewType.ordinal -> {
                VenueDetailGalleryViewHolder(VenueDetailGalleryView(context).apply {
                    venueDetailGalleryViewClick.subscribe { venueDetailGalleryViewClickSubject.onNext(it) }
                })
            }
            ViewType.VenueDetailGalleryWithCountViewType.ordinal -> {
                VenueDetailGalleryWithCountViewHolder(VenueDetailGalleryWithCountView(context).apply {
                    venueDetailGalleryCountViewClick.subscribe { venueDetailGalleryCountViewClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueDetailGalleryViewItem -> {
                (holder.itemView as VenueDetailGalleryView).bind(adapterItem.venueGalleryItem)
            }
            is AdapterItem.VenueDetailGalleryWithCountViewItem -> {
                (holder.itemView as VenueDetailGalleryWithCountView).bind(adapterItem.venueGalleryItem, galleryCount)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VenueDetailGalleryViewHolder(view: View) : RecyclerView.ViewHolder(view)
    private class VenueDetailGalleryWithCountViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class VenueDetailGalleryViewItem(val venueGalleryItem: VenueGalleryItem) : AdapterItem(ViewType.VenueDetailGalleryViewType.ordinal)
        data class VenueDetailGalleryWithCountViewItem(var venueGalleryItem: VenueGalleryItem) : AdapterItem(ViewType.VenueDetailGalleryWithCountViewType.ordinal)
    }

    private enum class ViewType {
        VenueDetailGalleryViewType,
        VenueDetailGalleryWithCountViewType
    }
}