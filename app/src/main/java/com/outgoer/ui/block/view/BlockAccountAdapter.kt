package com.outgoer.ui.block.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.post.model.BlockAccountPageState
import com.outgoer.api.profile.model.BlockUserResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class BlockAccountAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val blockAccountViewClickSubject: PublishSubject<BlockAccountPageState> = PublishSubject.create()
    val blockAccountViewClick: Observable<BlockAccountPageState> = blockAccountViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfBlockAccount: ArrayList<BlockUserResponse>? = null
        set(listOfBlockAccount) {
            field = listOfBlockAccount
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfBlockAccount?.let {
            it.forEach { data ->
                adapterItems.add(AdapterItem.BlockAccountViewItem(data))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.BlockAccountViewItemType.ordinal -> {
                BlockAccountViewHolder(BlockAccountView(context).apply {
                    blockAccountViewClick.subscribe { blockAccountViewClickSubject.onNext(it) }
                })
            }

            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.BlockAccountViewItem -> {
                (holder.itemView as BlockAccountView).bind(adapterItem.blockUserResponse)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class BlockAccountViewHolder(view: View) : RecyclerView.ViewHolder(view)
    sealed class AdapterItem(val type: Int) {
        data class BlockAccountViewItem(val blockUserResponse: BlockUserResponse) : AdapterItem(ViewType.BlockAccountViewItemType.ordinal)
    }
    private enum class ViewType {
        BlockAccountViewItemType
    }
}