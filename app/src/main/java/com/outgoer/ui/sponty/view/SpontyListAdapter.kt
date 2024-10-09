package com.outgoer.ui.sponty.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.api.post.model.VideoViewClick
import com.outgoer.api.sponty.model.SpontyResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SpontyListAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val spontyActionStateSubject: PublishSubject<SpontyActionState> = PublishSubject.create()
    val spontyActionState: Observable<SpontyActionState> = spontyActionStateSubject.hide()



    private var adapterItems = listOf<AdapterItem>()

    var userId: Int = -1

    var listOfSponty: List<SpontyResponse>? = null
        set(listOfSponty) {
            field = listOfSponty
            updateAdapterItem()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfSponty?.let {
            it.forEach { outgoerUser ->
                adapterItems.add(AdapterItem.SpontyViewItem(outgoerUser))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SpontyViewType.ordinal -> {
                SpontyViewHolder(SpontyListView(context).apply {
                    spontyActionState.subscribe { spontyActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SpontyViewItem -> {
                (holder.itemView as SpontyListView).bind(adapterItem.spontyResponse, userId)
            }
            else -> {}
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class SpontyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SpontyViewItem(val spontyResponse: SpontyResponse) : AdapterItem(ViewType.SpontyViewType.ordinal)
    }

    private enum class ViewType {
        SpontyViewType
    }
}