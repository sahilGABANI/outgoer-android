package com.outgoer.ui.home.newmap.venueevents.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.event.model.MutualFriends
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MutualFriendsAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val profileViewClickSubject: PublishSubject<MutualFriends> = PublishSubject.create()
    val profileViewClick: Observable<MutualFriends> = profileViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfMutualFriends: List<MutualFriends>? = null
        set(listOfMutualFriend) {
            field = listOfMutualFriend
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfMutualFriends?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.MutualFItemTypeViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.MutualFriendsViewItemType.ordinal -> {

                MutualFriendsViewHolder(MutualFriendsView(context).apply {
                    profileViewClick.subscribeAndObserveOnMainThread {
                        profileViewClickSubject.onNext(
                            it
                        )
                    }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.MutualFItemTypeViewItem -> {
                (holder.itemView as MutualFriendsView).bind(adapterItem.data)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class MutualFriendsViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class MutualFItemTypeViewItem(val data: MutualFriends) :
            AdapterItem(ViewType.MutualFriendsViewItemType.ordinal)
    }

    private enum class ViewType {
        MutualFriendsViewItemType
    }
}