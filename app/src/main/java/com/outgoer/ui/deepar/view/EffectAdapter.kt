package com.outgoer.ui.deepar.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import com.outgoer.api.effects.model.EffectResponse

class EffectAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val effectItemClicksSubject: PublishSubject<EffectResponse> = PublishSubject.create()
    val effectItemClicks: Observable<EffectResponse> = effectItemClicksSubject.hide()

    private var adapterItems = listOf<EffectListItems>()

    var listOfEffects: List<EffectResponse>? = null
        set(listOfReelsInfo) {
            field = listOfReelsInfo
            updateAdapterItems()

        }

    @Synchronized
    private fun updateAdapterItems() {
        val listItem = mutableListOf<EffectListItems>()

        listOfEffects?.forEach {
            listItem.add(EffectListItems.EffectsListItem(it))
        }

        adapterItems = listItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.EffectsListItemType.ordinal -> {
                EffectsListAdapterViewHolder(EffectView(context).apply {
                    effectItemClicks.subscribe { effectItemClicksSubject.onNext(it) }
                })
            }

            else -> throw IllegalArgumentException("Unsupported ViewType")
        }

    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val listItem = adapterItems?.getOrNull(position) ?: return
        when (listItem) {
            is EffectListItems.EffectsListItem -> {
                (holder.itemView as EffectView).bind(listItem.effectRes)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class EffectsListAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class EffectListItems(val type: Int) {
        data class EffectsListItem(var effectRes: EffectResponse) :
            EffectListItems(ViewType.EffectsListItemType.ordinal)
    }

    private enum class ViewType {
        EffectsListItemType
    }

}