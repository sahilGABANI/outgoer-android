package com.outgoer.ui.venuedetail.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.OtherNearPlaceClickState
import com.outgoer.api.venue.model.VenueMapInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailOtherNearPlacesAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val otherNearPlaceClickSubject: PublishSubject<OtherNearPlaceClickState> = PublishSubject.create()
    val otherNearPlaceClick: Observable<OtherNearPlaceClickState> = otherNearPlaceClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfNearPlaces: List<VenueMapInfo>? = null
        set(listOfNearPlaces) {
            field = listOfNearPlaces
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfNearPlaces?.forEach {
            adapterItems.add(AdapterItem.OtherNearPlacesViewItemTypeViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.OtherNearPlacesViewItemType.ordinal -> {
                OtherNearPlacesViewHolder(VenueDetailOtherNearPlacesView(context).apply {
                    otherNearPlaceClick.subscribe { otherNearPlaceClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.OtherNearPlacesViewItemTypeViewItem -> {
                (holder.itemView as VenueDetailOtherNearPlacesView).bind(adapterItem.venueMapInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class OtherNearPlacesViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class OtherNearPlacesViewItemTypeViewItem(val venueMapInfo: VenueMapInfo) : AdapterItem(ViewType.OtherNearPlacesViewItemType.ordinal)
    }

    private enum class ViewType {
        OtherNearPlacesViewItemType
    }
}