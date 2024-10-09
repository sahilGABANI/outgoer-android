package com.outgoer.ui.home.newReels.hashtag.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.reels.model.ReelInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReelsByHashTagAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val reelsHashtagItemClicksSubject: PublishSubject<ReelInfo> = PublishSubject.create()
    val reelsHashtagItemClicks: Observable<ReelInfo> = reelsHashtagItemClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ReelInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.ReelsByHashTagViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ReelsByHashTagViewItemType.ordinal -> {
                ReelsByHashTagAdapterViewHolder(ReelsByHashTagView(context).apply {
                    reelsHashtagItemClicks.subscribe { reelsHashtagItemClicksSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.ReelsByHashTagViewItem -> {
                (holder.itemView as ReelsByHashTagView).bind(adapterItem.reelInfo)
            }
        }
    }

    private class ReelsByHashTagAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ReelsByHashTagViewItem(var reelInfo: ReelInfo) : AdapterItem(ViewType.ReelsByHashTagViewItemType.ordinal)
    }

    private enum class ViewType {
        ReelsByHashTagViewItemType
    }
}