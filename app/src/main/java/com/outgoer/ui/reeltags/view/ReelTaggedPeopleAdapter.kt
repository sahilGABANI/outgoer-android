package com.outgoer.ui.reeltags.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.reels.model.ReelTaggedPeopleState
import com.outgoer.api.reels.model.ReelsTagsItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReelTaggedPeopleAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val reelTaggedPeopleClickSubject: PublishSubject<ReelTaggedPeopleState> = PublishSubject.create()
    val reelTaggedPeopleClick: Observable<ReelTaggedPeopleState> = reelTaggedPeopleClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItem: List<ReelsTagsItem>? = null
        set(listOfDataItem) {
            field = listOfDataItem
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItem?.forEach {
            adapterItems.add(AdapterItem.ReelTaggedPeopleViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ReelTaggedPeopleViewType.ordinal -> {
                ReelTaggedPeopleViewHolder(ReelTaggedPeopleView(context).apply {
                    reelTaggedPeopleClick.subscribe { reelTaggedPeopleClickSubject.onNext(it) }
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
            is AdapterItem.ReelTaggedPeopleViewItem -> {
                (holder.itemView as ReelTaggedPeopleView).bind(adapterItem.reelsTagsItem)
            }
        }
    }

    private class ReelTaggedPeopleViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ReelTaggedPeopleViewItem(val reelsTagsItem: ReelsTagsItem) : AdapterItem(ViewType.ReelTaggedPeopleViewType.ordinal)
    }

    private enum class ViewType {
        ReelTaggedPeopleViewType
    }
}