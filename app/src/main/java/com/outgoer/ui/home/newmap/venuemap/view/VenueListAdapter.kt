package com.outgoer.ui.home.newmap.venuemap.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueMapInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueListAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueCategoryAllClickSubject: PublishSubject<VenueMapInfo> = PublishSubject.create()
    val venueCategoryAllClick: Observable<VenueMapInfo> = venueCategoryAllClickSubject.hide()

    private val venueFavoriteClickSubject: PublishSubject<VenueMapInfo> = PublishSubject.create()
    val venueFavoriteClick: Observable<VenueMapInfo> = venueFavoriteClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var isAllSelected = true
    var listOfDataItems: List<VenueMapInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.VenueItemViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueItemViewType.ordinal -> {
                VenueCategoryViewHolder(VenueListView(context).apply {
                    venueCategoryClick.subscribe { venueCategoryAllClickSubject.onNext(it) }
                    venueFavoriteClick.subscribe { venueFavoriteClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueItemViewItem -> {
                (holder.itemView as VenueListView).bind(adapterItem.venueCategory)
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
    }

    private enum class ViewType {
        VenueItemViewType
    }
}