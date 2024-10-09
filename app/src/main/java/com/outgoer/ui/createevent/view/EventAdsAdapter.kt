package com.outgoer.ui.createevent.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EventAdsAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val addMediaActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val addMediaActionState: Observable<String> = addMediaActionStateSubject.hide()

    private val mediaActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val mediaActionState: Observable<String> = mediaActionStateSubject.hide()

    private val deleteActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val deleteActionState: Observable<String> = deleteActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var isAds: Boolean = false
    var isVenue: Boolean = false

    var listOfMedia: List<String>? = null
        set(listOfMedia) {
            field = listOfMedia
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        if(!isVenue) {
            if (!(isAds && listOfMedia != null && listOfMedia?.size ?: 0 > 0)) {
                adapterItems.add(AdapterItem.AddMediaViewItem(""))
            }
        }

        listOfMedia?.forEach {
            adapterItems.add(AdapterItem.MediaViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.AddMediaViewItemType.ordinal -> {
                MediaAdapterViewHolder(EventAdsView(context).apply {
                    addMediaActionState.subscribe { addMediaActionStateSubject.onNext(it) }
                })
            }

            ViewType.MediaViewItemType.ordinal -> {
                MediaAdapterViewHolder(EventAdsView(context).apply {
                    deleteActionState.subscribe { deleteActionStateSubject.onNext(it) }
                    mediaActionState.subscribe { mediaActionStateSubject.onNext(it) }
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
            is AdapterItem.AddMediaViewItem -> {
                (holder.itemView as EventAdsView).bindForAdd(adapterItem.addmedia)
            }
            is AdapterItem.MediaViewItem -> {
                (holder.itemView as EventAdsView).bind(adapterItem.media, isAds)
            }
        }
    }

    private class MediaAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class AddMediaViewItem(val addmedia: String) :
            AdapterItem(ViewType.AddMediaViewItemType.ordinal)

        data class MediaViewItem(val media: String) :
            AdapterItem(ViewType.MediaViewItemType.ordinal)
    }

    private enum class ViewType {
        AddMediaViewItemType,
        MediaViewItemType
    }
}