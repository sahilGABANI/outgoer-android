package com.outgoer.ui.create_story.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.ui.create_story.model.SelectedMedia
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class StoryMediaAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val storySelectionActionStateSubject: PublishSubject<SelectedMedia> = PublishSubject.create()
    val storySelectionAction: Observable<SelectedMedia> = storySelectionActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfFilePath: List<SelectedMedia>? = null
        set(listOfFilePath) {
            field = listOfFilePath
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfFilePath?.forEach {
            adapterItems.add(AdapterItem.StoryFileViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.StoryFileViewItemType.ordinal -> {
                StoryMediaAdapterViewHolder(StoryMediaView(context).apply {
                    storySelectionAction.subscribe { storySelectionActionStateSubject.onNext(it) }
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

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.StoryFileViewItem -> {
                (holder.itemView as StoryMediaView).bind(adapterItem.filePath)
            }
        }
    }

    private class StoryMediaAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class StoryFileViewItem(val filePath: SelectedMedia) : AdapterItem(ViewType.StoryFileViewItemType.ordinal)
    }

    private enum class ViewType {
        StoryFileViewItemType
    }
}