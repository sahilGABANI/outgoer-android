package com.outgoer.ui.home.create.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class AddedHashtagsAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private var adapterItems = listOf<AdapterItem>()
    private val removeItemClickStateSubject: PublishSubject<String> = PublishSubject.create()
    val removeItemClick: Observable<String> = removeItemClickStateSubject.hide()
    var listOfDataItems: List<String>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()
        }

    var isHashtagRemove: Boolean = false

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.AddedHashtagItem(it, isHashtagRemove))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.AddedHashtagItemType.ordinal -> {
                AddedHashtagAdapterViewHolder(AddedHashtagsView(context).apply {
                    removeItemClick.subscribe {
                        removeItemClickStateSubject.onNext(it)
                    }
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
            is AdapterItem.AddedHashtagItem -> {
                (holder.itemView as AddedHashtagsView).bind(adapterItem.hashTags, adapterItem.isHashtagRemove)
            }
        }
    }

    private class AddedHashtagAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class AddedHashtagItem(var hashTags: String, var isHashtagRemove: Boolean) : AdapterItem(ViewType.AddedHashtagItemType.ordinal)
    }

    private enum class ViewType {
        AddedHashtagItemType
    }
}