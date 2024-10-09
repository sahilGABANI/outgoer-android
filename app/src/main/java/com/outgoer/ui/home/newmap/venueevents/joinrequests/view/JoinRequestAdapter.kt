package com.outgoer.ui.home.newmap.venueevents.joinrequests.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.event.model.RequestResponseList
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class JoinRequestAdapter(
    private val context: Context,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val approveActionStateSubject: PublishSubject<RequestResponseList> = PublishSubject.create()
    val approveActionState: Observable<RequestResponseList> = approveActionStateSubject.hide()

    private val rejectActionStateSubject: PublishSubject<RequestResponseList> = PublishSubject.create()
    val rejectActionState: Observable<RequestResponseList> = rejectActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfJoinRequest: List<RequestResponseList>? = null
        set(listOfJoinRequest) {
            field = listOfJoinRequest
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfJoinRequest?.forEach {
            adapterItems.add(AdapterItem.JoinResponseViewItemType(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.JoinResponseViewItemType.ordinal -> {
                JoinAdapterViewHolder(JoinRequestView(context).apply {
                    approveActionState.subscribe { approveActionStateSubject.onNext(it) }
                    rejectActionState.subscribe { rejectActionStateSubject.onNext(it) }
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.JoinResponseViewItemType -> {
                (holder.itemView as JoinRequestView).bind(adapterItem.eventData)
            }
        }
    }

    private class JoinAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class JoinResponseViewItemType(val eventData: RequestResponseList) :
            AdapterItem(ViewType.JoinResponseViewItemType.ordinal)
    }

    private enum class ViewType {
        JoinResponseViewItemType
    }
}