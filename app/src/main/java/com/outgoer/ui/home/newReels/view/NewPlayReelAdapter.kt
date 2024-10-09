package com.outgoer.ui.home.newReels.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.reels.model.ReelsHashTagsItem
import com.outgoer.api.reels.model.ReelsPageState
import com.outgoer.ui.home.newReels.DiscoverReelsFragment
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewPlayReelAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val playReelViewClicksSubject: PublishSubject<ReelsPageState> = PublishSubject.create()
    val playReelViewClicks: Observable<ReelsPageState> = playReelViewClicksSubject.hide()

    private val reelsHashtagItemClicksSubject: PublishSubject<ReelsHashTagsItem> = PublishSubject.create()
    val reelsHashtagItemClicks: Observable<ReelsHashTagsItem> = reelsHashtagItemClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ReelInfo>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.PlayReelViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PlayReelViewItemType.ordinal -> {
                val playReelView = NewPlayReelView(context)
                playReelView.playReelViewClicks.subscribe { playReelViewClicksSubject.onNext(it) }
                playReelView.reelsHashtagItemClicks.subscribe { reelsHashtagItemClicksSubject.onNext(it) }
                PlayReelAdapterViewHolder(playReelView)
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
            is AdapterItem.PlayReelViewItem -> {
                (holder.itemView as NewPlayReelView).bind(adapterItem.reelInfo)
            }
        }
    }

    private class PlayReelAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class PlayReelViewItem(var reelInfo: ReelInfo) : AdapterItem(ViewType.PlayReelViewItemType.ordinal)
    }

    private enum class ViewType {
        PlayReelViewItemType
    }
}