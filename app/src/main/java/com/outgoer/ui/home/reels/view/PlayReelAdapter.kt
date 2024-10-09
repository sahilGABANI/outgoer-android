package com.outgoer.ui.home.reels.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.reels.model.ReelsPageState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PlayReelAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val playReelViewClicksSubject: PublishSubject<ReelsPageState> = PublishSubject.create()
    val playReelViewClicks: Observable<ReelsPageState> = playReelViewClicksSubject.hide()

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
            adapterItems.add(AdapterItem.PlayReelViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PlayReelViewItemType.ordinal -> {
                PlayReelAdapterViewHolder(PlayReelView(context).apply {
                    playReelViewClicks.subscribe { playReelViewClicksSubject.onNext(it) }
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
            is AdapterItem.PlayReelViewItem -> {
                (holder.itemView as PlayReelView).bind(adapterItem.reelInfo)
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