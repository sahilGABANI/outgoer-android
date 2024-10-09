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

class FollowingAdapter(
    private val context: Context
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
            adapterItems.add(AdapterItem.FollowingDetailViewItemType(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.FollowingDetailViewItemType.ordinal -> {
                FollowingAdapterViewHolder(FollowingDetailView(context).apply {
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
            is AdapterItem.FollowingDetailViewItemType -> {
                (holder.itemView as FollowingDetailView).bind(adapterItem.followUser)
            }
        }
    }

    private class FollowingAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class FollowingDetailViewItemType(val followUser: FollowUser) : AdapterItem(ViewType.FollowingDetailViewItemType.ordinal)
    }

    private enum class ViewType {
        FollowingDetailViewItemType
    }
}