package com.outgoer.ui.chat.newview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.profile.model.NearByUserResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewMessageFollowingAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val profileItemClickStateSubject: PublishSubject<NearByUserResponse> = PublishSubject.create()
    val profileItemClickState: Observable<NearByUserResponse> = profileItemClickStateSubject.hide()

    private val followClickStateSubject: PublishSubject<NearByUserResponse> = PublishSubject.create()
    val followClickState: Observable<NearByUserResponse> = followClickStateSubject.hide()


    private var adapterItems = listOf<AdapterItem>()

    var chatUserList: List<NearByUserResponse>? = null
        set(chatUserList) {
            field = chatUserList
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItem = mutableListOf<AdapterItem>()
        chatUserList?.let {
            it.forEach { conversationList ->
                adapterItem.add(AdapterItem.FindFriendViewItemTypeViewItem(conversationList))
            }
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.FindFriendViewItemType.ordinal -> {
                FindFriendListViewHolder(NewMessageFollowingView(context).apply {
                    profileItemClickState.subscribe { profileItemClickStateSubject.onNext(it) }
                    followClickState.subscribe { followClickStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.FindFriendViewItemTypeViewItem -> {
                (holder.itemView as NewMessageFollowingView).bind(adapterItem.conversationList)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class FindFriendListViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class FindFriendViewItemTypeViewItem(val conversationList: NearByUserResponse) :
            AdapterItem(ViewType.FindFriendViewItemType.ordinal)
    }

    private enum class ViewType {
        FindFriendViewItemType
    }
}