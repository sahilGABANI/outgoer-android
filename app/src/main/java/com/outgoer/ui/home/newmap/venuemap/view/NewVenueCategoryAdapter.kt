package com.outgoer.ui.home.newmap.venuemap.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueCategory
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewVenueCategoryAdapter(
    private val context: Context,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueCategoryAllClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val venueCategoryAllClick: Observable<Unit> = venueCategoryAllClickSubject.hide()

    private val venueCategoryClickSubject: PublishSubject<VenueCategory> = PublishSubject.create()
    val venueCategoryClick: Observable<VenueCategory> = venueCategoryClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var isAllSelected = true
    var listOfDataItems: List<VenueCategory>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        adapterItems.add(AdapterItem.VenueCategoryAllViewItem)

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.VenueCategoryViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueCategoryAllItemViewType.ordinal -> {
                VenueCategoryAllViewHolder(NewVenueCategoryAllView(context).apply {
                    venueCategoryAllClick.subscribe { venueCategoryAllClickSubject.onNext(it) }
                })
            }
            ViewType.VenueCategoryItemViewType.ordinal -> {
                VenueCategoryViewHolder(NewVenueCategoryView(context).apply {
                    venueCategoryClick.subscribe { venueCategoryClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueCategoryAllViewItem -> {
                (holder.itemView as NewVenueCategoryAllView).bind(isAllSelected)
            }
            is AdapterItem.VenueCategoryViewItem -> {
                (holder.itemView as NewVenueCategoryView).bind(adapterItem.venueCategory)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VenueCategoryAllViewHolder(view: View) : RecyclerView.ViewHolder(view)
    private class VenueCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        object VenueCategoryAllViewItem : AdapterItem(ViewType.VenueCategoryAllItemViewType.ordinal)
        data class VenueCategoryViewItem(val venueCategory: VenueCategory) : AdapterItem(ViewType.VenueCategoryItemViewType.ordinal)
    }

    private enum class ViewType {
        VenueCategoryAllItemViewType,
        VenueCategoryItemViewType
    }
}