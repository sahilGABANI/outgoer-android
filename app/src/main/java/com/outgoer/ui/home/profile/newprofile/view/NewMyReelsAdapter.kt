package com.outgoer.ui.home.profile.newprofile.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.reels.model.ReelInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewMyReelsAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val postViewClickSubject: PublishSubject<ReelInfo> = PublishSubject.create()
    val postViewClick: Observable<ReelInfo> = postViewClickSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<ReelInfo>? = null
        set(listOfItems) {
            field = listOfItems
            updateAdapterItems()
        }

    private fun updateAdapterItems() {
        val adapterItem = mutableListOf<AdapterItem>()
        listOfDataItems?.forEach {
            adapterItem.add(AdapterItem.PostViewItem(it))
        }
        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.PostItemViewType.ordinal -> {
                PostViewHolder(NewUserReelView(context).apply {
                    postViewClick.subscribe { postViewClickSubject.onNext(it) }
                })
            }
            else -> throw IllegalArgumentException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val adapterItem = adapterItems.getOrNull(position) ?: return

        when (adapterItem) {
            is AdapterItem.PostViewItem -> {
                (holder.itemView as NewUserReelView).bind(adapterItem.data)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class PostViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class PostViewItem(val data: ReelInfo) : AdapterItem(ViewType.PostItemViewType.ordinal)
    }

    private enum class ViewType {
        PostItemViewType
    }
}