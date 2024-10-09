package com.outgoer.ui.createevent.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.venue.model.VenueCategory
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EventCategoryAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val eventCategoryActionStateSubject: PublishSubject<VenueCategory> = PublishSubject.create()
    val eventCategoryActionState: Observable<VenueCategory> = eventCategoryActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()


    var listOfCategory: List<VenueCategory>? = null
        set(listOfCategory) {
            field = listOfCategory
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        
        listOfCategory?.forEach {
            adapterItems.add(AdapterItem.CategoryViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CategoryViewItemType.ordinal -> {
                CategoryAdapterViewHolder(EventCategoryView(context).apply {
                    eventCategoryActionState.subscribe { eventCategoryActionStateSubject.onNext(it) }
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
            is AdapterItem.CategoryViewItem -> {
                (holder.itemView as EventCategoryView).bind(adapterItem.category)
            }
        }
    }

    private class CategoryAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class CategoryViewItem(val category: VenueCategory) : AdapterItem(ViewType.CategoryViewItemType.ordinal)
    }

    private enum class ViewType {
        CategoryViewItemType
    }
}