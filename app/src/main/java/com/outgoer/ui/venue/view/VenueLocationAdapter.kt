package com.outgoer.ui.venue.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.event.model.GooglePlaces
import com.outgoer.ui.sponty.location.model.Predictions
import com.outgoer.ui.sponty.location.model.ResultResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueLocationAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val venueAvailableClickSubject: PublishSubject<ResultResponse> = PublishSubject.create()
    val venueAvailableClick: Observable<ResultResponse> = venueAvailableClickSubject.hide()

    private val googlePlacesClickSubject: PublishSubject<GooglePlaces> = PublishSubject.create()
    val googlePlacesClick: Observable<GooglePlaces> = googlePlacesClickSubject.hide()

    private val placeAvailableClickSubject: PublishSubject<Predictions> = PublishSubject.create()
    val placeAvailableClick: Observable<Predictions> = placeAvailableClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listofGooglePlaces: ArrayList<GooglePlaces>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    var listoflocation: ArrayList<ResultResponse>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    var listofPredictions: ArrayList<Predictions>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listofGooglePlaces?.forEach {
            adapterItems.add(AdapterItem.GooglePlacesViewItem(it))
        }

        listoflocation?.forEach {
            adapterItems.add(AdapterItem.VenueLocationViewItem(it))
        }
        listofPredictions?.forEach {
            adapterItems.add(AdapterItem.VenueLocationViewItemPredictions(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.VenueLocationViewType.ordinal -> {
                VenueLocationViewHolder(VenueLocationView(context).apply {
                    venueAvailableClick.subscribe { venueAvailableClickSubject.onNext(it) }
                    placeAvailableClick.subscribe { placeAvailableClickSubject.onNext(it) }
                })
            }

            ViewType.VenueGoogleLocationVIewType.ordinal -> {
                VenueLocationViewHolder(VenueLocationView(context).apply {
                    googlePlacesClick.subscribe { googlePlacesClickSubject.onNext(it) }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.VenueLocationViewItem -> {
                (holder.itemView as VenueLocationView).bind(adapterItem.venueReview)
            }
            is AdapterItem.VenueLocationViewItemPredictions -> {
                (holder.itemView as VenueLocationView).bind(adapterItem.venueReview)
            }
            is AdapterItem.GooglePlacesViewItem -> {
                (holder.itemView as VenueLocationView).bindGoogle(adapterItem.venueReview)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class VenueLocationViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class VenueLocationViewItem(val venueReview: ResultResponse) : AdapterItem(ViewType.VenueLocationViewType.ordinal)
        data class VenueLocationViewItemPredictions(val venueReview: Predictions) : AdapterItem(ViewType.VenueLocationViewType.ordinal)
        data class GooglePlacesViewItem(val venueReview: GooglePlaces) : AdapterItem(ViewType.VenueGoogleLocationVIewType.ordinal)

    }

    private enum class ViewType {
        VenueLocationViewType,
        VenueGoogleLocationVIewType
    }
}