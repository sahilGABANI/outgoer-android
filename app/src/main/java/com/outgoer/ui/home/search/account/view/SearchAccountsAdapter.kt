package com.outgoer.ui.home.search.account.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.follow.model.FollowActionState
import com.outgoer.api.follow.model.FollowUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SearchAccountsAdapter(
    private val context: Context,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val searchAccountsStateSubject: PublishSubject<FollowActionState> = PublishSubject.create()
    val searchAccountsState: Observable<FollowActionState> = searchAccountsStateSubject.hide()

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
            adapterItems.add(AdapterItem.SearchAccountsViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SearchAccountsViewType.ordinal -> {
                SearchAccountsAdapterViewHolder(SearchAccountsView(context).apply {
                    searchAccountsState.subscribe { searchAccountsStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SearchAccountsViewItem -> {
                (holder.itemView as SearchAccountsView).bind(adapterItem.followUser)
                holder.setIsRecyclable(false)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class SearchAccountsAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SearchAccountsViewItem(val followUser: FollowUser) : AdapterItem(ViewType.SearchAccountsViewType.ordinal)
    }

    private enum class ViewType {
        SearchAccountsViewType
    }
}