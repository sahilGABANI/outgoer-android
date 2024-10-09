package com.outgoer.ui.home.newReels.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.reels.model.ReelsHashTagsItem
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewReelsHashtagAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val reelsHashtagItemClicksSubject: PublishSubject<ReelsHashTagsItem> = PublishSubject.create()
    val reelsHashtagItemClicks: Observable<ReelsHashTagsItem> = reelsHashtagItemClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var isReels: Boolean = false

    var listOfDataItems: List<ReelsHashTagsItem>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.ReelsHashtagItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ReelsHashtagItemType.ordinal -> {
                ReelsHashtagAdapterViewHolder(NewReelsHashtagView(context).apply {
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
            is AdapterItem.ReelsHashtagItem -> {
                (holder.itemView as NewReelsHashtagView).bind(adapterItem.hashTags, isReels)
            }
        }
    }

    private class ReelsHashtagAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ReelsHashtagItem(var hashTags: ReelsHashTagsItem) : AdapterItem(ViewType.ReelsHashtagItemType.ordinal)
    }

    private enum class ViewType {
        ReelsHashtagItemType
    }
}