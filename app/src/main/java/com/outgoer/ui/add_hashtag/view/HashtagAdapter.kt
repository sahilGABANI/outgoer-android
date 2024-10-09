package com.outgoer.ui.add_hashtag.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.hashtag.model.HashtagResponse
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.ui.music.view.AddMusicView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HashtagAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val hashtagClickSubject: PublishSubject<HashtagResponse> = PublishSubject.create()
    val hashtagClick: Observable<HashtagResponse> = hashtagClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfHashtagInfo: ArrayList<HashtagResponse>? = null
        set(listOfHashtagInfo) {
            field = listOfHashtagInfo
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfHashtagInfo?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.HashtagTypeViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.HashtagViewItemType.ordinal -> {
                HashtagInfoViewHolder(HashtagView(context).apply {
                    hashtagClick.subscribeAndObserveOnMainThread {
                        hashtagClickSubject.onNext(it)
                    }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.HashtagTypeViewItem -> {
                (holder.itemView as HashtagView).bind(adapterItem.musicResponse)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class HashtagInfoViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class HashtagTypeViewItem(val musicResponse: HashtagResponse) :
            AdapterItem(ViewType.HashtagViewItemType.ordinal)
    }

    private enum class ViewType {
        HashtagViewItemType
    }
}