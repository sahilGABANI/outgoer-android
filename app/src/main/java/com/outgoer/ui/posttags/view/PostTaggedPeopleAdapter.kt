package com.outgoer.ui.posttags.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.PostTagsItem
import com.outgoer.api.post.model.PostTaggedPeopleState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class PostTaggedPeopleAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val postTaggedPeopleClickSubject: PublishSubject<PostTaggedPeopleState> = PublishSubject.create()
    val postTaggedPeopleClick: Observable<PostTaggedPeopleState> = postTaggedPeopleClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItem: List<PostTagsItem>? = null
        set(listOfDataItem) {
            field = listOfDataItem
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItem?.forEach {
            adapterItems.add(AdapterItem.PostTaggedPeopleViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PostTaggedPeopleViewType.ordinal -> {
                PostTaggedPeopleViewHolder(PostTaggedPeopleView(context).apply {
                    postTaggedPeopleClick.subscribe { postTaggedPeopleClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.PostTaggedPeopleViewItem -> {
                (holder.itemView as PostTaggedPeopleView).bind(adapterItem.postTagsItem)
            }
        }
    }

    private class PostTaggedPeopleViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class PostTaggedPeopleViewItem(val postTagsItem: PostTagsItem) : AdapterItem(ViewType.PostTaggedPeopleViewType.ordinal)
    }

    private enum class ViewType {
        PostTaggedPeopleViewType
    }
}