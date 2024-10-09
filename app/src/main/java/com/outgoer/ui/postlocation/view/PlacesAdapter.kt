package com.outgoer.ui.postlocation.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.ui.sponty.location.model.ResultResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PlacesAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val placesActionStateSubject: PublishSubject<ResultResponse> = PublishSubject.create()
    val placesActionState: Observable<ResultResponse> = placesActionStateSubject.hide()


    private var adapterItems = listOf<AdapterItem>()

    var userId: Int = -1

    var listOfPlaces: List<ResultResponse>? = null
        set(listOfPlaces) {
            field = listOfPlaces
            updateAdapterItem()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfPlaces?.let {
            it.forEach { resultRes ->
                adapterItems.add(AdapterItem.PlacesViewItem(resultRes))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PlacesViewType.ordinal -> {
                PlacesViewHolder(PlacesView(context).apply {
                    placesActionState.subscribe { placesActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.PlacesViewItem -> {
                (holder.itemView as PlacesView).bind(adapterItem.resultResponse)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class PlacesViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class PlacesViewItem(val resultResponse: ResultResponse) :
            AdapterItem(ViewType.PlacesViewType.ordinal)
    }

    private enum class ViewType {
        PlacesViewType
    }
}