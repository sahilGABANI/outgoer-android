package com.outgoer.ui.newvenuedetail.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.ReviewImage
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReviewImageAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val clickMediaActionStateSubject: PublishSubject<ReviewImage> = PublishSubject.create()
    val clickMediaActionState: Observable<ReviewImage> = clickMediaActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfMedia: List<ReviewImage>? = null
        set(listOfMedia) {
            field = listOfMedia
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfMedia?.forEach {
            adapterItems.add(AdapterItem.ReviewMediaViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ReviewMediaViewItemType.ordinal -> {
                ReviewMediaAdapterViewHolder(ReviewImageView(context).apply {
                    clickMediaActionState.subscribe { clickMediaActionStateSubject.onNext(it) }
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
            is AdapterItem.ReviewMediaViewItem -> {
                (holder.itemView as ReviewImageView).bind(adapterItem.media)
            }
        }
    }

    private class ReviewMediaAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ReviewMediaViewItem(val media: ReviewImage) : AdapterItem(ViewType.ReviewMediaViewItemType.ordinal)
    }

    private enum class ViewType {
        ReviewMediaViewItemType
    }
}