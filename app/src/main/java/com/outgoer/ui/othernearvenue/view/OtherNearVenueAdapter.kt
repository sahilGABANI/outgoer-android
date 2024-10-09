package com.outgoer.ui.othernearvenue.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.OtherNearPlaceClickState
import com.outgoer.api.venue.model.VenueMapInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OtherNearVenueAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val otherNearPlaceClickSubject: PublishSubject<OtherNearPlaceClickState> = PublishSubject.create()
    val otherNearPlaceClick: Observable<OtherNearPlaceClickState> = otherNearPlaceClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<VenueMapInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.OtherNearPlacesViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OtherNearVenueViewItemType.ordinal -> {
                OtherNearVenueViewHolder(OtherNearVenueView(context).apply {
                    otherNearPlaceClick.subscribe { otherNearPlaceClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.OtherNearPlacesViewItem -> {
                (holder.itemView as OtherNearVenueView).bind(adapterItem.venueMapInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class OtherNearVenueViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class OtherNearPlacesViewItem(val venueMapInfo: VenueMapInfo) : AdapterItem(ViewType.OtherNearVenueViewItemType.ordinal)
    }

    private enum class ViewType {
        OtherNearVenueViewItemType
    }
}