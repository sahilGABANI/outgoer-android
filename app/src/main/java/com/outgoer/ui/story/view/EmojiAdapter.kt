package com.outgoer.ui.story.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EmojiAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val emojiActionStateSubject: PublishSubject<Int> = PublishSubject.create()
    val emojiActionState: Observable<Int> = emojiActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfEmoji: List<Int>? = null
        set(listOfEmoji) {
            field = listOfEmoji
            updateAdapterItem()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfEmoji?.let {
            it.forEach { item ->
                adapterItems.add(AdapterItem.EmojiViewItem(item))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.EmojiViewType.ordinal -> {
                EmojiViewHolder(EmojiView(context).apply {
                    emojiActionState.subscribe { emojiActionStateSubject.onNext(it) }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.EmojiViewItem -> {
                (holder.itemView as EmojiView).bind(adapterItem.emojiIn)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class EmojiViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class EmojiViewItem(val emojiIn: Int) : AdapterItem(ViewType.EmojiViewType.ordinal)
    }

    private enum class ViewType {
        EmojiViewType
    }
}