package com.outgoer.ui.tag.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class TagPeopleAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val tagPeopleClickSubject: PublishSubject<PeopleForTag> = PublishSubject.create()
    val tagPeopleClick: Observable<PeopleForTag> = tagPeopleClickSubject.hide()

    private val tagFollowClickSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val tagFollowClick: Observable<FollowUser> = tagFollowClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<PeopleForTag>? = null
        set(listOfNearPlaces) {
            field = listOfNearPlaces
            updateAdapterItem()
        }

    var listOfFollowUser: List<FollowUser>? = null
        set(listOfFollowUser) {
            field = listOfFollowUser
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.TagPeopleViewItemTypeViewItem(data))
            }
        }


        listOfFollowUser?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.TagFollowViewItemTypeViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.TagPeopleViewItemType.ordinal -> {
                TagPeopleViewHolder(TagPeopleView(context).apply {
                    tagPeopleClick.subscribeAndObserveOnMainThread {
                        tagPeopleClickSubject.onNext(it)
                    }
                    tagFollowClick.subscribeAndObserveOnMainThread {
                        tagFollowClickSubject.onNext(it)
                    }
                })
            }

            ViewType.TagFollowViewItemType.ordinal -> {
                TagPeopleViewHolder(TagPeopleView(context).apply {
                    tagPeopleClick.subscribeAndObserveOnMainThread {
                        tagPeopleClickSubject.onNext(it)
                    }
                    tagFollowClick.subscribeAndObserveOnMainThread {
                        tagFollowClickSubject.onNext(it)
                    }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.TagPeopleViewItemTypeViewItem -> {
                (holder.itemView as TagPeopleView).bind(adapterItem.peopleForTag)
            }
            is AdapterItem.TagFollowViewItemTypeViewItem -> {
                (holder.itemView as TagPeopleView).bindFollow(adapterItem.followUser)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class TagPeopleViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class TagPeopleViewItemTypeViewItem(val peopleForTag: PeopleForTag) : AdapterItem(ViewType.TagPeopleViewItemType.ordinal)
        data class TagFollowViewItemTypeViewItem(val followUser: FollowUser) : AdapterItem(ViewType.TagFollowViewItemType.ordinal)
    }

    private enum class ViewType {
        TagPeopleViewItemType,
        TagFollowViewItemType,
    }
}