package com.outgoer.ui.newnotification.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.notification.model.NotificationActionState
import com.outgoer.api.notification.model.NotificationInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewGeneralNotificationBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class GeneralNotificationView(context: Context) : ConstraintLayoutWithLifecycle(context)   {

    private val notificationActionStateSubject: PublishSubject<NotificationActionState> = PublishSubject.create()
    val notificationActionState: Observable<NotificationActionState> = notificationActionStateSubject.hide()

    private lateinit var binding: ViewGeneralNotificationBinding
    private lateinit var notificationInfo:NotificationInfo

    private val bottomMargin = context.resources.getDimension(com.intuit.sdp.R.dimen._12sdp).toInt()

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        val view = View.inflate(context, R.layout.view_general_notification, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        (layoutParams as LayoutParams).setMargins(0, 0, 0, bottomMargin)
        binding = ViewGeneralNotificationBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                notificationActionStateSubject.onNext(NotificationActionState.UpdateReadStatus(notificationInfo))
                if (loggedInUserId != notificationInfo.senderId) {
                    notificationActionStateSubject.onNext(NotificationActionState.RowViewClick(notificationInfo))

                    notificationInfo.isRead = 1
                    // updateNotificationReadStatus()
                }
            }.autoDispose()

            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                if (loggedInUserId != notificationInfo.senderId) {
                    notificationActionStateSubject.onNext(NotificationActionState.UserProfileClick(notificationInfo))

                    notificationInfo.isRead = 1
                    //updateNotificationReadStatus()
                }
            }.autoDispose()
        }
    }

    fun bind(notificationInfo: NotificationInfo) {
        this.notificationInfo = notificationInfo
        binding.apply {
            Glide.with(context)
                .load(notificationInfo.sender?.avatar ?: "")
                .placeholder(R.drawable.ic_logo_placeholder)
                .centerCrop()
                .into(ivUserProfile)

            tvNotificationMessage.text = notificationInfo.title ?: ""
            tvNotificationDateTime.text = notificationInfo.message ?: ""

        }
    }


}