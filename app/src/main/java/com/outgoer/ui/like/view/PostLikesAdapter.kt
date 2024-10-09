package com.outgoer.ui.like.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.PostLikesUser
import com.outgoer.api.post.model.PostLikesUserPageState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PostLikesAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val postLikesViewClickSubject: PublishSubject<PostLikesUserPageState> = PublishSubject.create()
    val postLikesViewClick: Observable<PostLikesUserPageState> = postLikesViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<PostLikesUser>? = null
        set(listOfUser) {
            field = listOfUser
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.PostLikesViewItemTypeViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PostLikesViewItemType.ordinal -> {
                PostLikesViewHolder(PostLikesView(context).apply {
                    postLikesViewClick.subscribe { postLikesViewClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.PostLikesViewItemTypeViewItem -> {
                (holder.itemView as PostLikesView).bind(adapterItem.postLikesUser)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class PostLikesViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class PostLikesViewItemTypeViewItem(val postLikesUser: PostLikesUser) : AdapterItem(ViewType.PostLikesViewItemType.ordinal)
    }

    private enum class ViewType {
        PostLikesViewItemType
    }
}