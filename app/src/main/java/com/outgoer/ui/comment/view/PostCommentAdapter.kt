package com.outgoer.ui.comment.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.CommentInfo
import com.outgoer.api.post.model.PostCommentActionState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PostCommentAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val postCommentActionStateSubject: PublishSubject<PostCommentActionState> = PublishSubject.create()
    val postCommentActionState: Observable<PostCommentActionState> = postCommentActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfCommentInfo: List<CommentInfo>? = null
        set(listOfCommentInfo) {
            field = listOfCommentInfo
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfCommentInfo?.forEach {
            adapterItems.add(AdapterItem.CommentViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CommentViewItemType.ordinal -> {
                CommentAdapterViewHolder(PostCommentView(context).apply {
                    postCommentActionState.subscribe { postCommentActionStateSubject.onNext(it) }
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
            is AdapterItem.CommentViewItem -> {
                (holder.itemView as PostCommentView).bind(adapterItem.commentInfo)
            }
        }
    }

    private class CommentAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class CommentViewItem(val commentInfo: CommentInfo) : AdapterItem(ViewType.CommentViewItemType.ordinal)
    }

    private enum class ViewType {
        CommentViewItemType
    }
}