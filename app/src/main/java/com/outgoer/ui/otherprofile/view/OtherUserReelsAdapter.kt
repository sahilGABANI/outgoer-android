package com.outgoer.ui.otherprofile.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.ui.home.profile.view.UserReelsView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class OtherUserReelsAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val reelsViewClickSubject: PublishSubject<ReelInfo> = PublishSubject.create()
    val reelsViewClick: Observable<ReelInfo> = reelsViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ReelInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.ReelsViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ReelsItemViewType.ordinal -> {
                ReelsViewHolder(UserReelsView(context).apply {
                    reelsViewClick.subscribe { reelsViewClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.ReelsViewItem -> {
                (holder.itemView as UserReelsView).bind(adapterItem.reelInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class ReelsViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ReelsViewItem(val reelInfo: ReelInfo) : AdapterItem(ViewType.ReelsItemViewType.ordinal)
    }

    private enum class ViewType {
        ReelsItemViewType
    }
}