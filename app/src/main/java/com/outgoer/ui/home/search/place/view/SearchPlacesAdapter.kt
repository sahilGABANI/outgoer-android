package com.outgoer.ui.home.search.place.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.follow.model.FollowActionState
import com.outgoer.api.follow.model.PlaceFollowActionState
import com.outgoer.api.venue.model.VenueListInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SearchPlacesAdapter(
    private val context: Context,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val searchPlaceClickSubject: PublishSubject<VenueListInfo> = PublishSubject.create()
    val searchPlaceClick: Observable<VenueListInfo> = searchPlaceClickSubject.hide()

    private val followActionStateSubject: PublishSubject<PlaceFollowActionState> = PublishSubject.create()
    val followActionState: Observable<PlaceFollowActionState> = followActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItem: List<VenueListInfo>? = null
        set(listOfFollowUser) {
            field = listOfFollowUser
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItem?.forEach {
            adapterItems.add(AdapterItem.SearchPlacesViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SearchPlacesViewType.ordinal -> {
                SearchPlacesAdapterViewHolder(SearchPlacesView(context).apply {
                    searchPlaceClick.subscribe { searchPlaceClickSubject.onNext(it) }
                    followActionState.subscribe { followActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SearchPlacesViewItem -> {
                (holder.itemView as SearchPlacesView).bind(adapterItem.venueListInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class SearchPlacesAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SearchPlacesViewItem(val venueListInfo: VenueListInfo) : AdapterItem(ViewType.SearchPlacesViewType.ordinal)
    }

    private enum class ViewType {
        SearchPlacesViewType
    }
}