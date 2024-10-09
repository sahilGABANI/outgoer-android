package com.outgoer.ui.report.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.ReportReason
import com.outgoer.ui.invitefriends.view.InviteFriendsLiveStreamView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReportReasonAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val reportReasonClickSubject: PublishSubject<ReportReason> = PublishSubject.create()
    val reportReasonClick: Observable<ReportReason> = reportReasonClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItem: List<ReportReason>? = null
        set(listOfFollowResponse) {
            field = listOfFollowResponse
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItem?.forEach { adapterItems.add(AdapterItem.ReportReasonViewItem(it)) }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ReportReasonViewType.ordinal -> {
                ReportReasonViewHolder(ReportReasonView(context).apply {
                   reportReasonClick.subscribe { reportReasonClickSubject.onNext(it)}

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
            is AdapterItem.ReportReasonViewItem -> {
                (holder.itemView as ReportReasonView).bind(adapterItem.reportReason)
            }
        }
    }

    private class ReportReasonViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ReportReasonViewItem(val reportReason: ReportReason) : AdapterItem(ViewType.ReportReasonViewType.ordinal)
    }

    private enum class ViewType {
        ReportReasonViewType
    }
}