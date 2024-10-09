package com.outgoer.ui.followdetail.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.follow.model.FollowActionState
import com.outgoer.api.follow.model.FollowUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FollowersAdapter(
    private val context: Context,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val followActionStateSubject: PublishSubject<FollowActionState> = PublishSubject.create()
    val followActionState: Observable<FollowActionState> = followActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItem: List<FollowUser>? = null
        set(listOfFollowUser) {
            field = listOfFollowUser
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItem?.forEach {
            adapterItems.add(AdapterItem.FollowersDetailViewItemType(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.FollowersDetailViewItemType.ordinal -> {
                FollowAdapterViewHolder(FollowersDetailView(context).apply {
                    followActionState.subscribe { followActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.FollowersDetailViewItemType -> {
                (holder.itemView as FollowersDetailView).bind(adapterItem.followUser)
            }
        }
    }

    private class FollowAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class FollowersDetailViewItemType(val followUser: FollowUser) : AdapterItem(ViewType.FollowersDetailViewItemType.ordinal)
    }

    private enum class ViewType {
        FollowersDetailViewItemType
    }
}