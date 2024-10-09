package com.outgoer.ui.home.profile.newprofile.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueListInfo
import com.outgoer.api.venue.model.VenueViewClickState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewMyFavouriteVenueAdapter(
    private val context: Context,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueViewClickSubject: PublishSubject<VenueViewClickState> = PublishSubject.create()
    val venueViewClick: Observable<VenueViewClickState> = venueViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<VenueListInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.MyFavouriteVenueViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.MyFavouriteVenueViewType.ordinal -> {
                MyFavouriteVenueViewHolder(NewMyFavouriteVenueView(context).apply {
                    venueViewClick.subscribe { venueViewClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.MyFavouriteVenueViewItem -> {
                (holder.itemView as NewMyFavouriteVenueView).bind(adapterItem.venueListInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class MyFavouriteVenueViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class MyFavouriteVenueViewItem(val venueListInfo: VenueListInfo) : AdapterItem(ViewType.MyFavouriteVenueViewType.ordinal)
    }

    private enum class ViewType {
        MyFavouriteVenueViewType
    }
}