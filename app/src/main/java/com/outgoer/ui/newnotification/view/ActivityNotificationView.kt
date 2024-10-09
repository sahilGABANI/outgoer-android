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
import com.outgoer.databinding.ViewActivityNotificationBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class ActivityNotificationView(context: Context) : ConstraintLayoutWithLifecycle(context)  {
    private val notificationActionStateSubject: PublishSubject<NotificationActionState> = PublishSubject.create()
    val notificationActionState: Observable<NotificationActionState> = notificationActionStateSubject.hide()

    private lateinit var binding: ViewActivityNotificationBinding
    private lateinit var notificationInfo: NotificationInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        val view = View.inflate(context, R.layout.view_activity_notification, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewActivityNotificationBinding.bind(view)

        binding.apply {
            throttleClicks().subscribeAndObserveOnMainThread {
                notificationActionStateSubject.onNext(NotificationActionState.UpdateReadStatus(notificationInfo))
                if (loggedInUserId != notificationInfo.senderId) {
                    notificationActionStateSubject.onNext(NotificationActionState.RowViewClick(notificationInfo))
                    notificationInfo.isRead = 1
                }
            }.autoDispose()

            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                if (loggedInUserId != notificationInfo.senderId) {
                    notificationActionStateSubject.onNext(NotificationActionState.UserProfileClick(notificationInfo))
                    notificationInfo.isRead = 1
                }
            }.autoDispose()
        }
    }

    fun bind(notificationInfo: NotificationInfo) {
        this.notificationInfo = notificationInfo
        binding.apply {
            Glide.with(context)
                .load(notificationInfo.sender?.avatar ?: "")
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .centerCrop()
                .into(ivUserProfile)

            tvNotificationMessage.text = notificationInfo.message ?: ""
            tvNotificationDateTime.text = notificationInfo.humanReadableTime ?: ""

            ivOtherMedia.visibility = View.VISIBLE
            val postImageUrl = notificationInfo.postImageUrl
            val reelImageUrl = notificationInfo.reelImageUrl
            if (!postImageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(postImageUrl)
                    .placeholder(R.drawable.ic_chat_user_placeholder)
                    .centerCrop()
                    .into(ivOtherMedia)
            } else if (!reelImageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(reelImageUrl)
                    .placeholder(R.drawable.ic_chat_user_placeholder)
                    .centerCrop()
                    .into(ivOtherMedia)
            } else {
                ivOtherMedia.visibility = View.GONE
            }
        }
    }
}