package com.outgoer.ui.home.chat.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.profile.model.NearByUserResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NearbyPeopleAdapter(private val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val nearbyPeopleItemClickStateSubject: PublishSubject<NearByUserResponse> = PublishSubject.create()
    val nearbyPeopleItemClickState: Observable<NearByUserResponse> = nearbyPeopleItemClickStateSubject.hide()

    private val castMessageClickStateSubject: PublishSubject<NearByUserResponse> = PublishSubject.create()
    val castMessageClickState: Observable<NearByUserResponse> = castMessageClickStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var chatUserList: List<NearByUserResponse>? = arrayListOf()
        set(chatUserList) {
            field = chatUserList
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItem = mutableListOf<AdapterItem>()

        chatUserList?.forEach { conversationList ->
            adapterItem.add(AdapterItem.NearbyPeopleViewItemTypeViewItem(conversationList))
        }

        this.adapterItems = adapterItem
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.NearbyPeopleViewItemType.ordinal -> {
                NearbyPeopleListViewHolder(NearbyPeopleView(context).apply {
                    nearbyPeopleItemClickState.subscribe { nearbyPeopleItemClickStateSubject.onNext(it) }
                    castMessageClickState.subscribe { castMessageClickStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.NearbyPeopleViewItemTypeViewItem -> {
                (holder.itemView as NearbyPeopleView).bind(adapterItem.conversationList)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class NearbyPeopleListViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class NearbyPeopleViewItemTypeViewItem(val conversationList: NearByUserResponse) :
            AdapterItem(ViewType.NearbyPeopleViewItemType.ordinal)
    }

    private enum class ViewType {
        NearbyPeopleViewItemType
    }
}