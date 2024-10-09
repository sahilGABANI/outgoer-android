package com.outgoer.ui.otherprofile.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.PostInfo
import com.outgoer.ui.home.profile.newprofile.view.NewOtherUserPostView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewOtherUserPostAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val postViewClickSubject: PublishSubject<PostInfo> = PublishSubject.create()
    val postViewClick: Observable<PostInfo> = postViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<PostInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.PostViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PostItemViewType.ordinal -> {
                PostViewHolder(NewOtherUserPostView(context).apply {
                    postViewClick.subscribe { postViewClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.PostViewItem -> {
                (holder.itemView as NewOtherUserPostView).bind(adapterItem.data)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class PostViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class PostViewItem(val data: PostInfo) : AdapterItem(ViewType.PostItemViewType.ordinal)
    }

    private enum class ViewType {
        PostItemViewType
    }
}