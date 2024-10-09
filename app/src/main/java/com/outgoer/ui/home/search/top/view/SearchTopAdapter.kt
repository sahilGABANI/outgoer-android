package com.outgoer.ui.home.search.top.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.MyTagBookmarkInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SearchTopAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val searchTopClickSubject: PublishSubject<MyTagBookmarkInfo> = PublishSubject.create()
    val searchTopClick: Observable<MyTagBookmarkInfo> = searchTopClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<MyTagBookmarkInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
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
                TopSearchViewHolder(SearchTopView(context).apply {
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
                if (adapterItem.myTagBookmarkInfo.isSavePost == true) {
                    (holder.itemView as SearchTopView).bindSavePost(adapterItem.myTagBookmarkInfo)
                } else {
                    (holder.itemView as SearchTopView).bind(adapterItem.myTagBookmarkInfo)
                }
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