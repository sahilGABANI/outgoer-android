package com.outgoer.ui.sponty.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.sponty.model.SpontyJoins
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SpontyUserAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val spontyUserActionStateSubject: PublishSubject<SpontyJoins> = PublishSubject.create()
    val spontyUserActionState: Observable<SpontyJoins> = spontyUserActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfSpontyUser: List<SpontyJoins>? = null
        set(listOfSpontyUser) {
            field = listOfSpontyUser
            updateAdapterItem()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfSpontyUser?.let {
            it.forEach { item ->
                adapterItems.add(AdapterItem.SpontyUserViewItem(item))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SpontyUserViewType.ordinal -> {
                SpontyLocationViewHolder(SpontyUserView(context).apply {
                    spontyUserActionState.subscribe { spontyUserActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SpontyUserViewItem -> {
                (holder.itemView as SpontyUserView).bind(adapterItem.spontyJoins)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class SpontyLocationViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SpontyUserViewItem(val spontyJoins: SpontyJoins) :
            AdapterItem(ViewType.SpontyUserViewType.ordinal)
    }

    private enum class ViewType {
        SpontyUserViewType
    }
}