package com.outgoer.ui.login.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.follow.model.SuggestedUser
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SuggestedUserAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val suggestedActionStateSubject: PublishSubject<SuggestedUser> = PublishSubject.create()
    val suggestedActionState: Observable<SuggestedUser> = suggestedActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfSuggestedUser: List<SuggestedUser>? = null
        set(listOfSuggestedUser) {
            field = listOfSuggestedUser
            updateAdapterItems()
        }

    @SuppressLint("NotifyDataSetChanged")
    @Synchronized
    private fun updateAdapterItems() {
        val adapterItems = mutableListOf<AdapterItem>()

        listOfSuggestedUser?.forEach {
            adapterItems.add(AdapterItem.SuggestedUserViewItem(it))
        }

        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.SuggestedUserViewItemType.ordinal -> {
                SuggestedViewHolder(SuggestedUserView(context).apply {
                    suggestedActionState.subscribe { suggestedActionStateSubject.onNext(it) }
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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.SuggestedUserViewItem -> {
                (holder.itemView as SuggestedUserView).bind(adapterItem.suggestedUser)
            }
        }
    }

    private class SuggestedViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class SuggestedUserViewItem(val suggestedUser: SuggestedUser) :
            AdapterItem(ViewType.SuggestedUserViewItemType.ordinal)
    }

    private enum class ViewType {
        SuggestedUserViewItemType
    }
}