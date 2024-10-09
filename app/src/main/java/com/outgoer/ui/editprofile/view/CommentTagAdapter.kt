package com.outgoer.ui.editprofile.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.follow.model.FollowUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class CommentTagAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val commentTagPeopleClickSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val commentTagPeopleClick: Observable<FollowUser> = commentTagPeopleClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<FollowUser>? = null
        set(listOfDataItems) {
            field = listOfDataItems
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.CommentTagPeopleViewItem(it))
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.CommentTagPeopleViewItemType.ordinal -> {
                CommentTagPeopleViewHolder(CommentTagView(context).apply {
                    commentTagPeopleClick.subscribe { commentTagPeopleClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.CommentTagPeopleViewItem -> {
                (holder.itemView as CommentTagView).bind(adapterItem.followUser)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class CommentTagPeopleViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class CommentTagPeopleViewItem(val followUser: FollowUser) :
            AdapterItem(ViewType.CommentTagPeopleViewItemType.ordinal)
    }

    private enum class ViewType {
        CommentTagPeopleViewItemType
    }
}