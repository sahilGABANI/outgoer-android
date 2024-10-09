package com.outgoer.ui.sponty.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.api.post.model.SpontyCommentActionState
import com.outgoer.api.sponty.model.SpontyCommentResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SpontyReplyListAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val spontyCommentActionStateSubject: PublishSubject<SpontyCommentActionState> = PublishSubject.create()
    val spontyCommentActionState: Observable<SpontyCommentActionState> = spontyCommentActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var userId: Int = -1

    var listOfSponty: List<SpontyCommentResponse>? = null
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
                adapterItems.add(AdapterItem.SpontyReplyViewItem(outgoerUser))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SpontyReplyViewType.ordinal -> {
                SpontyReplyViewHolder(SpontyReplyListView(context).apply {
                    spontyCommentActionState.subscribe { spontyCommentActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SpontyReplyViewItem -> {
                (holder.itemView as SpontyReplyListView).bind(adapterItem.SpontyCommentResponse, userId)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class SpontyReplyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SpontyReplyViewItem(val SpontyCommentResponse: SpontyCommentResponse) : AdapterItem(ViewType.SpontyReplyViewType.ordinal)
    }

    private enum class ViewType {
        SpontyReplyViewType
    }
}