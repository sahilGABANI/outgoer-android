package com.outgoer.ui.suggested.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.profile.model.SuggestedUserActionState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SuggestedUserAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val suggestedUserActionStateSubject: PublishSubject<SuggestedUserActionState> = PublishSubject.create()
    val suggestedUserActionState: Observable<SuggestedUserActionState> = suggestedUserActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfSuggestedUsers: List<OutgoerUser>? = null
        set(listOfSuggestedUsers) {
            field = listOfSuggestedUsers
            updateAdapterItem()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfSuggestedUsers?.let {
            it.forEach { outgoerUser ->
                adapterItems.add(AdapterItem.SuggestedUserViewItem(outgoerUser))
            }
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SuggestedUserViewType.ordinal -> {
                SuggestedUserAdapterViewHolder(SuggestedUserView(context).apply {
                    suggestedUserActionState.subscribe { suggestedUserActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SuggestedUserViewItem -> {
                (holder.itemView as SuggestedUserView).bind(adapterItem.outgoerUser)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class SuggestedUserAdapterViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SuggestedUserViewItem(val outgoerUser: OutgoerUser) : SuggestedUserAdapter.AdapterItem(ViewType.SuggestedUserViewType.ordinal)
    }

    private enum class ViewType {
        SuggestedUserViewType
    }
}