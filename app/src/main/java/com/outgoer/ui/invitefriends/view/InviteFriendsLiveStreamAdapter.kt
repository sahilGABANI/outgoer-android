package com.outgoer.ui.invitefriends.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.follow.model.FollowUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class InviteFriendsLiveStreamAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val inviteUpdatedSubject: PublishSubject<Map<Int, FollowUser>> = PublishSubject.create()
    val inviteUpdated: Observable<Map<Int, FollowUser>> = inviteUpdatedSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfFollowResponseWithId: MutableMap<Int, FollowUser>? = null
        set(listOfFollowResponse) {
            field = listOfFollowResponse
            updateAdapterItems()
        }

    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfFollowResponseWithId?.forEach { adapterItems.add(AdapterItem.InviteFriendsLiveStreamViewItem(it.value)) }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.InviteFriendsLiveStreamViewType.ordinal -> {
                InviteFriendsLiveStreamViewHolder(InviteFriendsLiveStreamView(context).apply {
                    inviteButtonViewClicks.subscribe {
                        updateSelection(it, true)
                    }
                    invitedButtonViewClicks.subscribe {
                        updateSelection(it, false)
                    }
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.InviteFriendsLiveStreamViewItem -> {
                (holder.itemView as InviteFriendsLiveStreamView).bind(adapterItem.followUser)
            }
        }
    }

    private class InviteFriendsLiveStreamViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class InviteFriendsLiveStreamViewItem(val followUser: FollowUser) : AdapterItem(ViewType.InviteFriendsLiveStreamViewType.ordinal)
    }

    private enum class ViewType {
        InviteFriendsLiveStreamViewType
    }

    private fun updateSelection(selectedFollowResponse: FollowUser, isInvited: Boolean) {
        listOfFollowResponseWithId?.put(
            selectedFollowResponse.id,
            selectedFollowResponse.copy(isInvited = isInvited)
        )
        inviteUpdatedSubject.onNext(listOfFollowResponseWithId?.toMap() ?: mapOf())
    }
}