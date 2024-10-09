package com.outgoer.ui.home.newReels.comment.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.reels.model.ReelCommentInfo
import com.outgoer.api.reels.model.ReelsCommentPageState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewReelsCommentAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val reelsCommentPageStateSubject: PublishSubject<ReelsCommentPageState> = PublishSubject.create()
    val reelsCommentPageState: Observable<ReelsCommentPageState> = reelsCommentPageStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfReelsComment: List<ReelCommentInfo>? = null
        set(listOfReelsComment) {
            field = listOfReelsComment
            updateAdapterItem()
        }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfReelsComment?.forEach {
            adapterItems.add(AdapterItem.ReelsCommentViewItemTypeViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ReelsCommentViewItemType.ordinal -> {
                ReelsCommentViewHolder(NewReelsCommentView(context).apply {
                    reelsCommentPageState.subscribe { reelsCommentPageStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.ReelsCommentViewItemTypeViewItem -> {
                (holder.itemView as NewReelsCommentView).bind(adapterItem.reelCommentInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun getItemViewType(position: Int): Int {
        return adapterItems[position].type
    }

    private class ReelsCommentViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ReelsCommentViewItemTypeViewItem(val reelCommentInfo: ReelCommentInfo) : AdapterItem(ViewType.ReelsCommentViewItemType.ordinal)
    }

    enum class ViewType {
        ReelsCommentViewItemType
    }

}