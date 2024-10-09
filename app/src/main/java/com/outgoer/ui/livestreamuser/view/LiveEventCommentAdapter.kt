package com.outgoer.ui.livestreamuser.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.live.model.LiveEventSendOrReadComment
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveEventCommentAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val liveStreamCommentViewClicksSubject: PublishSubject<LiveEventSendOrReadComment> = PublishSubject.create()
    val liveStreamCommentViewClicks: Observable<LiveEventSendOrReadComment> = liveStreamCommentViewClicksSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfComments: List<LiveEventSendOrReadComment>? = null
        set(listOfComments) {
            field = listOfComments
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfComments?.forEach {
            adapterItems.add(AdapterItem.LiveStreamCommentViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.LiveStreamCommentViewType.ordinal -> {
                LiveStreamCommentViewHolder(LiveEventCommentView(context).apply {
                    liveStreamCommentViewClicks.subscribe { liveStreamCommentViewClicksSubject.onNext(it) }
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
            is AdapterItem.LiveStreamCommentViewItem -> {
                (holder.itemView as LiveEventCommentView).bind(adapterItem.liveEventSendOrReadComment)
            }
        }
    }

    private class LiveStreamCommentViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class LiveStreamCommentViewItem(val liveEventSendOrReadComment: LiveEventSendOrReadComment) :
            AdapterItem(ViewType.LiveStreamCommentViewType.ordinal)
    }

    private enum class ViewType {
        LiveStreamCommentViewType
    }
}