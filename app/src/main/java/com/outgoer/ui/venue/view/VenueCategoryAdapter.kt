package com.outgoer.ui.venue.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueCategory
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueCategoryAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueCategoryClickSubject: PublishSubject<VenueCategory> = PublishSubject.create()
    val venueCategoryClick: Observable<VenueCategory> = venueCategoryClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<VenueCategory>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.VenueCategoryViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueCategoryItemViewType.ordinal -> {
                VenueCategoryViewHolder(VenueCategoryView(context).apply {
                    venueCategoryClick.subscribe { venueCategoryClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueCategoryViewItem -> {
                (holder.itemView as VenueCategoryView).bind(adapterItem.venueCategory)
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
        data class VenueCategoryViewItem(val venueCategory: VenueCategory) : AdapterItem(ViewType.VenueCategoryItemViewType.ordinal)
    }

    private enum class ViewType {
        VenueCategoryItemViewType
    }
}