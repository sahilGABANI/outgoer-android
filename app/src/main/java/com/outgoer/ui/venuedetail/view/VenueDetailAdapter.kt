package com.outgoer.ui.venuedetail.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.R
import com.outgoer.api.venue.model.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val sectionViewClickSubject: PublishSubject<SectionViewSectionItem> = PublishSubject.create()
    val sectionViewClick: Observable<SectionViewSectionItem> = sectionViewClickSubject.hide()

    private val otherNearPlaceClickSubject: PublishSubject<OtherNearPlaceClickState> = PublishSubject.create()
    val otherNearPlaceClick: Observable<OtherNearPlaceClickState> = otherNearPlaceClickSubject.hide()

    private val latestEventsClickSubject: PublishSubject<VenueEventInfo> = PublishSubject.create()
    val latestEventsClick: Observable<VenueEventInfo> = latestEventsClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var eventsCount: Int = 0

    var listOfLatestEvent: List<VenueEventInfo>? = null
        set(listOfLatestEvent) {
            field = listOfLatestEvent
            updateAdapterItem()
        }

    var listOfNearPlaces: List<VenueMapInfo>? = null
        set(listOfNearPlaces) {
            field = listOfNearPlaces
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfLatestEvent?.let {
            if (it.isNotEmpty()) {
                val latestEvent = SectionViewSectionItem.LatestEventSection(
                    SectionViewInfo(context.getString(R.string.label_latest_events), eventsCount > 3)
                )
                adapterItems.add(AdapterItem.SectionViewItem(latestEvent))
                adapterItems.add(AdapterItem.LatestEventViewItem(it))
            }
        }

        listOfNearPlaces?.let {
            if (it.isNotEmpty()) {
                val otherNearPlaces = SectionViewSectionItem.OtherNearPlacesSection(
                    SectionViewInfo(context.getString(R.string.other_near_places), it.size >= 6)
                )
                adapterItems.add(AdapterItem.SectionViewItem(otherNearPlaces))
                adapterItems.add(AdapterItem.OtherNearPlacesViewItem(it))
            }
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SectionViewItemType.ordinal -> {
                PlaceViewHolder(SectionView(context).apply {
                    sectionViewClick.subscribe { sectionViewClickSubject.onNext(it) }
                })
            }
            ViewType.LatestEventViewItemType.ordinal -> {
                PlaceViewHolder(VenueDetailLatestEventsList(context).apply {
                     latestEventsClick.subscribe { latestEventsClickSubject.onNext(it) }
                })
            }
            ViewType.OtherNearPlacesViewItemType.ordinal -> {
                PlaceViewHolder(VenueDetailOtherNearPlacesList(context).apply {
                    otherNearPlaceClick.subscribe { otherNearPlaceClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SectionViewItem -> {
                (holder.itemView as SectionView).bind(adapterItem.sectionViewSectionItem)
            }
            is AdapterItem.LatestEventViewItem -> {
                (holder.itemView as VenueDetailLatestEventsList).bind(adapterItem.listOfLatestEvent)
            }
            is AdapterItem.OtherNearPlacesViewItem -> {
                (holder.itemView as VenueDetailOtherNearPlacesList).bind(adapterItem.listOfOtherNearPlaces)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class PlaceViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SectionViewItem(val sectionViewSectionItem: SectionViewSectionItem) : AdapterItem(ViewType.SectionViewItemType.ordinal)

        data class LatestEventViewItem(val listOfLatestEvent: List<VenueEventInfo>) : AdapterItem(ViewType.LatestEventViewItemType.ordinal)

        data class OtherNearPlacesViewItem(val listOfOtherNearPlaces: List<VenueMapInfo>) : AdapterItem(ViewType.OtherNearPlacesViewItemType.ordinal)
    }

    private enum class ViewType {
        SectionViewItemType,
        LatestEventViewItemType,
        OtherNearPlacesViewItemType
    }
}