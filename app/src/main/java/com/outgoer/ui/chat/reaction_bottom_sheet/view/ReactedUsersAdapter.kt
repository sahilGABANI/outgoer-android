package com.outgoer.ui.chat.reaction_bottom_sheet.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.chat.model.Reaction
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReactedUsersAdapter(
    private val context: Context,
    private val loggedInUserId: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val reactionClickSubject: PublishSubject<Reaction> = PublishSubject.create()
    val reactionClick: Observable<Reaction> = reactionClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<Reaction>? = null
        set(listOfData) {
            field = listOfData
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.ReactedUserViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ReactedUserViewItemType.ordinal -> {
                ReactedUserViewHolder(ReactedUserView(context).apply {
                    reactionClick.subscribeAndObserveOnMainThread {
                        reactionClickSubject.onNext(it)
                    }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.ReactedUserViewItem -> {
                (holder.itemView as ReactedUserView).bind(adapterItem.reaction, loggedInUserId)
            }
        }
    }

    private class ReactedUserViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ReactedUserViewItem(val reaction: Reaction) :
            AdapterItem(ViewType.ReactedUserViewItemType.ordinal)
    }

    private enum class ViewType {
        ReactedUserViewItemType,
    }
}