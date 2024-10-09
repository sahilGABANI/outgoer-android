package com.outgoer.ui.home.home.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueDetail
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HomeVenueAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var adapterItems = listOf<AdapterItem>()

    private val venueDetailActionStateSubject: PublishSubject<VenueDetail> = PublishSubject.create()
    val venueDetailActionState: Observable<VenueDetail> = venueDetailActionStateSubject.hide()
    var listOfVenue: List<VenueDetail>? = null
        set(listOfVenue) {
            field = listOfVenue
            updateAdapterItem()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfVenue?.let {
            it.forEach { outgoerUser ->
                adapterItems.add(AdapterItem.VenueViewItem(outgoerUser))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueViewType.ordinal -> {
                VenueViewHolder(HomeVenueView(context).apply {
                    venueDetailActionState.subscribe { venueDetailActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueViewItem -> {
                (holder.itemView as HomeVenueView).bind(adapterItem.venue)
            }
            else -> {}
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class VenueViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class VenueViewItem(val venue: VenueDetail) : AdapterItem(ViewType.VenueViewType.ordinal)
    }

    private enum class ViewType {
        VenueViewType
    }
}