package com.outgoer.ui.group.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.group.model.GroupUserInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GroupAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val groupItemClickStateSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val groupItemClick: Observable<FollowUser> = groupItemClickStateSubject.hide()

    private val closeItemClickStateSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val closeItemClick: Observable<FollowUser> = closeItemClickStateSubject.hide()

    private val groupItemClickedStateSubject: PublishSubject<GroupUserInfo> = PublishSubject.create()
    val groupItemClicked: Observable<GroupUserInfo> = groupItemClickedStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var isFirstOne: Boolean = false

    var listOfUsers: List<FollowUser>? = arrayListOf()
        set(listOfUsers) {
            field = listOfUsers
            updateAdapterItem()
        }

    var listOfGroupUsers: ArrayList<GroupUserInfo>? = arrayListOf()
        set(listOfGroupUsers) {
            field = listOfGroupUsers
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItem = mutableListOf<AdapterItem>()

        if(listOfUsers != null) {
            listOfUsers?.forEach { user ->
                adapterItem.add(AdapterItem.GroupUserViewItem(user, isFirstOne))
            }
        }

        if(listOfGroupUsers != null) {
            listOfGroupUsers?.forEach { user ->
                adapterItem.add(AdapterItem.GroupUsersViewItem(user, isFirstOne))
            }
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.GroupUserViewItemType.ordinal -> {
                GroupUserViewHolder(GroupView(context).apply {
                    groupItemClick.subscribe { groupItemClickStateSubject.onNext(it) }
                    closeItemClick.subscribe { closeItemClickStateSubject.onNext(it) }
                    groupItemClicked.subscribe { groupItemClickedStateSubject.onNext(it) }
                })
            }

            ViewType.NewGroupViewItemType.ordinal -> {
                GroupUserViewHolder(GroupView(context).apply {
                    groupItemClick.subscribe { groupItemClickStateSubject.onNext(it) }
                    closeItemClick.subscribe { closeItemClickStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.GroupUserViewItem -> {
                (holder.itemView as GroupView).bind(adapterItem.outgoerUser, adapterItem.isFirstOne)
            }
            is AdapterItem.GroupUsersViewItem -> {
                (holder.itemView as GroupView).bindNew(adapterItem.outgoerUser, adapterItem.isFirstOne)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class GroupUserViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class GroupUserViewItem(val outgoerUser: FollowUser, val isFirstOne: Boolean) : AdapterItem(ViewType.GroupUserViewItemType.ordinal)
        data class GroupUsersViewItem(val outgoerUser: GroupUserInfo, val isFirstOne: Boolean) : AdapterItem(ViewType.NewGroupViewItemType.ordinal)
    }

    private enum class ViewType {
        GroupUserViewItemType,
        NewGroupViewItemType
    }
}