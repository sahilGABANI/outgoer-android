package com.outgoer.ui.home.profile.newprofile.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SwitchAccountAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val tagPeopleClickSubject: PublishSubject<PeopleForTag> = PublishSubject.create()
    val tagPeopleClick: Observable<PeopleForTag> = tagPeopleClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<PeopleForTag>? = null
        set(listOfNearPlaces) {
            field = listOfNearPlaces
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.SwitchViewItemTypeViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SwitchAccountViewItemType.ordinal -> {
                SwitchAccountViewHolder(SwitchAccountView(context).apply {
                    tagPeopleClick.subscribeAndObserveOnMainThread {
                        tagPeopleClickSubject.onNext(it)
                    }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SwitchViewItemTypeViewItem -> {
                (holder.itemView as SwitchAccountView).bind(adapterItem.peopleForTag)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class SwitchAccountViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SwitchViewItemTypeViewItem(val peopleForTag: PeopleForTag) :
            AdapterItem(ViewType.SwitchAccountViewItemType.ordinal)
    }

    private enum class ViewType {
        SwitchAccountViewItemType,
    }
}