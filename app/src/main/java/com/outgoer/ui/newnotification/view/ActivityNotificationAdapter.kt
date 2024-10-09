package com.outgoer.ui.newnotification.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.outgoer.api.notification.model.NotificationActionState
import com.outgoer.api.notification.model.NotificationInfo
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ActivityNotificationAdapter(
    private val context: Context
) : RecyclerView.Adapter<RecyclerView.ViewHolder>()  {

    private val notificationActionStateSubject: PublishSubject<NotificationActionState> = PublishSubject.create()
    val notificationActionState: Observable<NotificationActionState> = notificationActionStateSubject.hide()

    private var adapterItems = listOf<AdapterItem>()

    var listOfDataItems: List<NotificationInfo>? = null
        set(listOfNotification) {
            field = listOfNotification
            updateAdapterItem()
        }

    private fun updateAdapterItem() {
        val adapterItems = mutableListOf<AdapterItem>()
        listOfDataItems?.forEach {
            adapterItems.add(AdapterItem.NotificationViewItemTypeViewItem(it))
        }
        this.adapterItems = adapterItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ViewType.NotificationViewItemType.ordinal -> {
                NotificationViewHolder(ActivityNotificationView(context).apply {
                    notificationActionState.subscribe { notificationActionStateSubject.onNext(it) }
                })
            }
            else -> throw IllegalAccessException("Unsupported ViewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val adapterItem = adapterItems.getOrNull(position) ?: return
        when (adapterItem) {
            is AdapterItem.NotificationViewItemTypeViewItem -> {
                (holder.itemView as ActivityNotificationView).bind(adapterItem.notificationInfo)
            }
        }
    }

    override fun getItemCount(): Int {
        return adapterItems.size
    }

    private class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view)

    sealed class AdapterItem(val type: Int) {
        data class NotificationViewItemTypeViewItem(val notificationInfo: NotificationInfo) : AdapterItem(ViewType.NotificationViewItemType.ordinal)
    }

    private enum class ViewType {
        NotificationViewItemType
    }
}