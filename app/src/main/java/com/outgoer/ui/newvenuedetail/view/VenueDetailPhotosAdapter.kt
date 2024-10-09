package com.outgoer.ui.newvenuedetail.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueGalleryItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailPhotosAdapter(
    private val context: Context,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueDetailGalleryViewClickSubject: PublishSubject<VenueGalleryItem> = PublishSubject.create()
    val venueDetailGalleryViewClick: Observable<VenueGalleryItem> = venueDetailGalleryViewClickSubject.hide()

    private val deleteViewClickSubject: PublishSubject<VenueGalleryItem> = PublishSubject.create()
    val deleteViewClick: Observable<VenueGalleryItem> = deleteViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var isMyVenue: Boolean = false
    var listOfDataItems: ArrayList<VenueGalleryItem>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.VenueDetailGalleryViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueDetailGalleryViewType.ordinal -> {
                VenueDetailGalleryViewHolder(VenueDetailPhotosView(context).apply {
                    venueDetailGalleryViewClick.subscribe { venueDetailGalleryViewClickSubject.onNext(it) }
                    deleteViewClick.subscribe { deleteViewClickSubject.onNext(it) }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueDetailGalleryViewItem -> {
                (holder.itemView as VenueDetailPhotosView).bind(adapterItem.venueGalleryItem, isMyVenue)
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

    sealed class AdapterItem(val type: Int) {
        data class VenueDetailGalleryViewItem(val venueGalleryItem: VenueGalleryItem) :
            AdapterItem(ViewType.VenueDetailGalleryViewType.ordinal)
    }

    private enum class ViewType {
        VenueDetailGalleryViewType,
    }
}