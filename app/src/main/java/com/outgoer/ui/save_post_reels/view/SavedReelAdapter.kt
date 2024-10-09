package com.outgoer.ui.save_post_reels.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.MyTagBookmarkInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SavedReelAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val searchTopClickSubject: PublishSubject<MyTagBookmarkInfo> = PublishSubject.create()
    val searchTopClick: Observable<MyTagBookmarkInfo> = searchTopClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<MyTagBookmarkInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.TopSearchViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.TopSearchItemViewType.ordinal -> {
                TopSearchViewHolder(SavedReelView(context).apply {
                    searchTopClick.subscribe { searchTopClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.TopSearchViewItem -> {
                (holder.itemView as SavedReelView).bindSavePost(adapterItem.myTagBookmarkInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class TopSearchViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class TopSearchViewItem(val myTagBookmarkInfo: MyTagBookmarkInfo) : AdapterItem(ViewType.TopSearchItemViewType.ordinal)
    }

    private enum class ViewType {
        TopSearchItemViewType
    }
}