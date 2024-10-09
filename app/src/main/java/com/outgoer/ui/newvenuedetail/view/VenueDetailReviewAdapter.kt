package com.outgoer.ui.newvenuedetail.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueReviewModel
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailReviewAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueReviewViewClickSubject: PublishSubject<VenueReviewModel> = PublishSubject.create()
    val venueReviewViewClick: Observable<VenueReviewModel> = venueReviewViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfReviews: ArrayList<VenueReviewModel>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfReviews?.forEach {
            adapterItems.add(AdapterItem.VenueReviewViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueReviewViewType.ordinal -> {
                VenueReviewViewHolder(VenueDetailReviewView(context).apply {
                    venueReviewViewClick.subscribe { venueReviewViewClickSubject.onNext(it) }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueReviewViewItem -> {
                (holder.itemView as VenueDetailReviewView).bind(adapterItem.venueReview)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VenueReviewViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class VenueReviewViewItem(val venueReview: VenueReviewModel) : AdapterItem(ViewType.VenueReviewViewType.ordinal)
    }

    private enum class ViewType {
        VenueReviewViewType,
    }
}