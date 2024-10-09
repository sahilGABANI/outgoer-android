package com.outgoer.ui.sponty.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SpontyLocationAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val spontyLocationActionStateSubject: PublishSubject<String> = PublishSubject.create()
    val spontyLocationActionState: Observable<String> = spontyLocationActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfSpontyLocation: List<String>? = null
        set(listOfSpontyLocation) {
            field = listOfSpontyLocation
            updateAdapterItem()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfSpontyLocation?.let {
            it.forEach { String ->
                adapterItems.add(AdapterItem.SpontyLocationViewItem(String))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SpontyLocationViewType.ordinal -> {
                SpontyLocationViewHolder(SpontyLocationView(context).apply {
                    spontyLocationActionState.subscribe { spontyLocationActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SpontyLocationViewItem -> {
                (holder.itemView as SpontyLocationView).bind(adapterItem.String)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class SpontyLocationViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SpontyLocationViewItem(val String: String) : AdapterItem(ViewType.SpontyLocationViewType.ordinal)
    }

    private enum class ViewType {
        SpontyLocationViewType
    }
}