package com.outgoer.ui.chat.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.group.model.GroupUserInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class GroupUserTagAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val groupUserClickSubject: PublishSubject<GroupUserInfo> = PublishSubject.create()
    val groupUserClick: Observable<GroupUserInfo> = groupUserClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var groupuserList: List<GroupUserInfo>? = null
        set(groupuserList) {
            field = groupuserList
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        groupuserList?.forEach {
            adapterItems.add(AdapterItem.GroupUserViewItem(it))
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.GroupUserViewItemType.ordinal -> {
                GroupUserViewHolder(GroupUserTagView(context).apply {
                    groupUserClick.subscribe { groupUserClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.GroupUserViewItem -> {
                (holder.itemView as GroupUserTagView).bind(adapterItem.groupUserInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class GroupUserViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class GroupUserViewItem(val groupUserInfo: GroupUserInfo) : AdapterItem(ViewType.GroupUserViewItemType.ordinal)
    }

    private enum class ViewType {
        GroupUserViewItemType
    }
}