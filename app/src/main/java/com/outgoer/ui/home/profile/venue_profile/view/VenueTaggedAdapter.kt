package com.outgoer.ui.home.profile.venue_profile.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueTaggedAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val eventCategoryActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val eventCategoryActionState: Observable<String> = eventCategoryActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()


    var listOfVenueTag: List<String>? = null
        set(listOfVenueTag) {
            field = listOfVenueTag
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfVenueTag?.forEach {
            adapterItems.add(AdapterItem.VenueTaggedViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueTaggedViewItemType.ordinal -> {
                VenueTagAdapterViewHolder(VenueTaggedView(context).apply {
                    eventCategoryActionState.subscribe { eventCategoryActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueTaggedViewItem -> {
                (holder.itemView as VenueTaggedView).bind(adapterItem.venueTag)
            }
        }
    }

    private class VenueTagAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class VenueTaggedViewItem(val venueTag: String) :
            AdapterItem(ViewType.VenueTaggedViewItemType.ordinal)
    }

    private enum class ViewType {
        VenueTaggedViewItemType
    }
}