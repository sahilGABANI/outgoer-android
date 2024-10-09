package com.outgoer.ui.home.chat.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.chat.model.ChatConversationActionState
import com.outgoer.api.chat.model.ChatConversationInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewChatUserListAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val chatConversationActionStateSubject: PublishSubject<ChatConversationActionState> = PublishSubject.create()
    val chatConversationActionState: Observable<ChatConversationActionState> = chatConversationActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()
    private val uniqueChatUserSet = mutableSetOf<ChatConversationInfo>()

    var chatUserList: ArrayList<ChatConversationInfo>? = arrayListOf()
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
                    adapterItem.add(AdapterItem.ChatUserViewItemTypeViewItem(conversationList))
                }
            }
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.ChatUserViewItemType.ordinal -> {
                ChatUserListViewHolder(NewChatUserListView(context).apply {
                    chatConversationActionState.subscribe { chatConversationActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.ChatUserViewItemTypeViewItem -> {
                (holder.itemView as NewChatUserListView).bind(adapterItem.conversationList)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class ChatUserListViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class ChatUserViewItemTypeViewItem(val conversationList: ChatConversationInfo) :
            AdapterItem(ViewType.ChatUserViewItemType.ordinal)
    }

    private enum class ViewType {
        ChatUserViewItemType
    }
}