package com.outgoer.ui.home.newmap.venuemap.bottomsheet.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.friend_venue.model.UserVenueResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FriendsVenueAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val profileClickSubject: PublishSubject<UserVenueResponse> = PublishSubject.create()
    val profileClick: Observable<UserVenueResponse> = profileClickSubject.hide()

    private val messageClickSubject: PublishSubject<UserVenueResponse> = PublishSubject.create()
    val messageClick: Observable<UserVenueResponse> = messageClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var isAllSelected = true
    var listOfDataItems: List<UserVenueResponse>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.FriendsVenueItemViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.FriendsVenueItemViewType.ordinal -> {
                VenueCategoryViewHolder(FriendsVenueView(context).apply {
                    profileClick.subscribe { profileClickSubject.onNext(it) }
                    messageClick.subscribe { messageClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.FriendsVenueItemViewItem -> {
                (holder.itemView as FriendsVenueView).bind(adapterItem.venueCategory)
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
        data class FriendsVenueItemViewItem(val venueCategory: UserVenueResponse) : AdapterItem(ViewType.FriendsVenueItemViewType.ordinal)
    }

    private enum class ViewType {
        FriendsVenueItemViewType
    }
}