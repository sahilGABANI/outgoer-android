package com.outgoer.ui.createevent.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.event.model.GooglePlaces
import com.outgoer.api.venue.model.VenueMapInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NearVenueAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueClickSubject: PublishSubject<VenueMapInfo> = PublishSubject.create()
    val venueClick: Observable<VenueMapInfo> = venueClickSubject.hide()

    private val googlePlaceClickSubject: PublishSubject<GooglePlaces> = PublishSubject.create()
    val googlePlaceClick: Observable<GooglePlaces> = googlePlaceClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var isAllSelected = true
    var listOfDataItems: List<VenueMapInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    var listOfGooglePlaces: List<GooglePlaces>? = null
        set(listOfGooglePlaces) {
            field = listOfGooglePlaces
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        if(listOfGooglePlaces != null) {
            listOfGooglePlaces?.forEach {
                adapterItems.add(AdapterItem.GooglePlacesItemView(it))
            }
        } else {
            listOfDataItems?.forEach {
                adapterItems.add(AdapterItem.VenueItemViewItem(it))
            }
        }


        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueItemViewType.ordinal -> {
                VenueCategoryViewHolder(NearVenueView(context).apply {
                    venueClick.subscribe { venueClickSubject.onNext(it) }
                })
            }
            ViewType.GooglePlacesItemViewType.ordinal -> {
                VenueCategoryViewHolder(NearVenueView(context).apply {
                    googlePlaceClick.subscribe { googlePlaceClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueItemViewItem -> {
                (holder.itemView as NearVenueView).bind(adapterItem.venueCategory)
            }
            is AdapterItem.GooglePlacesItemView -> {
                (holder.itemView as NearVenueView).bindGoogle(adapterItem.venueCategory)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VenueCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class VenueItemViewItem(val venueCategory: VenueMapInfo) : AdapterItem(ViewType.VenueItemViewType.ordinal)
        data class GooglePlacesItemView(val venueCategory: GooglePlaces) : AdapterItem(ViewType.GooglePlacesItemViewType.ordinal)
    }

    private enum class ViewType {
        VenueItemViewType,
        GooglePlacesItemViewType
    }

}