package com.outgoer.ui.home.chat.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.chat.model.ChatConversationInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewFindFriendAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val findFriendItemClickStateSubject: PublishSubject<OutgoerUser> = PublishSubject.create()
    val findFriendItemClickState: Observable<OutgoerUser> = findFriendItemClickStateSubject.hide()

    private val followClickStateSubject: PublishSubject<OutgoerUser> = PublishSubject.create()
    val followClickState: Observable<OutgoerUser> = followClickStateSubject.hide()

    private val followingClickStateSubject: PublishSubject<OutgoerUser> = PublishSubject.create()
    val followingClickState: Observable<OutgoerUser> = followingClickStateSubject.hide()

    private val uniqueChatUserSet = mutableSetOf<OutgoerUser>()
    private var adapterItems = listOf<AdapterItem>()

    var chatUserList: List<OutgoerUser>? = null
        set(chatUserList) {
            field = chatUserList
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItem = mutableListOf<AdapterItem>()
        uniqueChatUserSet.clear()
        chatUserList?.let {
            it.forEach { conversationList ->
                if (uniqueChatUserSet.add(conversationList)) {
                    adapterItem.add(AdapterItem.FindFriendViewItemTypeViewItem(conversationList))
                }
            }
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.FindFriendViewItemType.ordinal -> {
                FindFriendListViewHolder(NewFindFriendView(context).apply {
                    findFriendItemClickState.subscribe { findFriendItemClickStateSubject.onNext(it) }
                    followClickState.subscribe { followClickStateSubject.onNext(it) }
                    followingClickState.subscribe { followingClickStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.FindFriendViewItemTypeViewItem -> {
                (holder.itemView as NewFindFriendView).bind(adapterItem.conversationList)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class FindFriendListViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class FindFriendViewItemTypeViewItem(val conversationList: OutgoerUser) :
            AdapterItem(ViewType.FindFriendViewItemType.ordinal)
    }

    private enum class ViewType {
        FindFriendViewItemType
    }
}