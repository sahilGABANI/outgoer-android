package com.outgoer.ui.add_hashtag.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class RemoveHashtagAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val hashtagClickSubject: PublishSubject<String> = PublishSubject.create()
    val hashtagClick: Observable<String> = hashtagClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfHashtagInfo: ArrayList<String>? = null
        set(listOfHashtagInfo) {
            field = listOfHashtagInfo
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfHashtagInfo?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.RemoveHashtagViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.RemoveHashtagViewItemType.ordinal -> {
                RemoveHashtagInfoViewHolder(RemoveHashtagView(context).apply {
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
            is AdapterItem.RemoveHashtagViewItem -> {
                (holder.itemView as RemoveHashtagView).bind(adapterItem.hashtag)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class RemoveHashtagInfoViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class RemoveHashtagViewItem(val hashtag: String) : AdapterItem(ViewType.RemoveHashtagViewItemType.ordinal)
    }

    private enum class ViewType {
        RemoveHashtagViewItemType
    }
}