package com.outgoer.ui.newvenuedetail.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.reels.model.ReelInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueReelsAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueDetailReelsViewClickSubject: PublishSubject<ReelInfo> = PublishSubject.create()
    val venueDetailReelsViewClick: Observable<ReelInfo> = venueDetailReelsViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listofAvailable: ArrayList<ReelInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listofAvailable?.forEach {
            adapterItems.add(AdapterItem.VenueReelsViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueReelsViewType.ordinal -> {
                VenueReelsViewHolder(VenueReelsView(context).apply {
                    venueDetailReelsViewClick.subscribe { venueDetailReelsViewClickSubject.onNext(it) }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueReelsViewItem -> {
                (holder.itemView as VenueReelsView).bind(adapterItem.reelInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VenueReelsViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class VenueReelsViewItem(val reelInfo: ReelInfo) : AdapterItem(ViewType.VenueReelsViewType.ordinal)
    }

    private enum class ViewType {
        VenueReelsViewType,
    }
}