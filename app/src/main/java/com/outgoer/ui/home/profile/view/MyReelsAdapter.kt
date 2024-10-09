package com.outgoer.ui.home.profile.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.reels.model.ReelInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MyReelsAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val addReelsViewClickSubject: PublishSubject<Unit> = PublishSubject.create()
    val addReelsViewClick: Observable<Unit> = addReelsViewClickSubject.hide()

    private val myReelsViewClickSubject: PublishSubject<ReelInfo> = PublishSubject.create()
    val myReelsViewClick: Observable<ReelInfo> = myReelsViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ReelInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        adapterItems.add(AdapterItem.AddReelsViewItem(""))

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.ReelsViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.AddReelsItemViewType.ordinal -> {
                AddReelsViewHolder(AddReelsView(context).apply {
                    addReelsViewClick.subscribe { addReelsViewClickSubject.onNext(it) }
                })
            }
            ViewType.ReelsItemViewType.ordinal -> {
                ReelsViewHolder(UserReelsView(context).apply {
                    reelsViewClick.subscribe { myReelsViewClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.AddReelsViewItem -> {
                (holder.itemView as AddReelsView).bind(adapterItem.data)
            }
            is AdapterItem.ReelsViewItem -> {
                (holder.itemView as UserReelsView).bind(adapterItem.reelInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class AddReelsViewHolder(view: View) : RecyclerView.ViewHolder(view)
    private class ReelsViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class AddReelsViewItem(val data: String) : AdapterItem(ViewType.AddReelsItemViewType.ordinal)
        data class ReelsViewItem(val reelInfo: ReelInfo) : AdapterItem(ViewType.ReelsItemViewType.ordinal)
    }

    private enum class ViewType {
        AddReelsItemViewType,
        ReelsItemViewType
    }
}