package com.outgoer.ui.chat.newview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.follow.model.FollowUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class FollowingListAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val profileItemClickStateSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val profileItemClickState: Observable<FollowUser> = profileItemClickStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var chatUserList: List<FollowUser>? = null
        set(chatUserList) {
            field = chatUserList
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItem = mutableListOf<AdapterItem>()
        chatUserList?.let {
            it.forEach { conversationList ->
                adapterItem.add(AdapterItem.FollowUserViewItemType(conversationList))
            }
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.FollowUserViewItemType.ordinal -> {
                FollowUserListViewHolder(FollowingListView(context).apply {
                    profileItemClickState.subscribe { profileItemClickStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.FollowUserViewItemType -> {
                (holder.itemView as FollowingListView).bind(adapterItem.conversationList)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class FollowUserListViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class FollowUserViewItemType(val conversationList: FollowUser) :
            AdapterItem(ViewType.FollowUserViewItemType.ordinal)
    }

    private enum class ViewType {
        FollowUserViewItemType
    }
}