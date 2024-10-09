package com.outgoer.ui.story.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.story.model.MentionUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class StoryUserViewAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val storyUserActionStateSubject: PublishSubject<MentionUser> = PublishSubject.create()
    val storyUserActionState: Observable<MentionUser> = storyUserActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfViewedUser: List<MentionUser>? = null
        set(listOfViewedUser) {
            field = listOfViewedUser
            updateAdapterItem()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfViewedUser?.let {
            it.forEach { item ->
                adapterItems.add(AdapterItem.StoryUserViewItem(item))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.StoryUserViewType.ordinal -> {
                StoryLocationViewHolder(StoryUserView(context).apply {
                    storyUserActionState.subscribe { storyUserActionStateSubject.onNext(it) }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.StoryUserViewItem -> {
                (holder.itemView as StoryUserView).bind(adapterItem.viewedStoryUsers)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class StoryLocationViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class StoryUserViewItem(val viewedStoryUsers: MentionUser) :
            AdapterItem(ViewType.StoryUserViewType.ordinal)
    }

    private enum class ViewType {
        StoryUserViewType
    }
}