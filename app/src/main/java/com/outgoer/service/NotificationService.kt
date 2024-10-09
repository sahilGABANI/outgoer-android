package com.outgoer.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.event.model.EventData
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.api.sponty.model.NotificationSponty
import com.outgoer.api.sponty.model.SpontyResponse

import com.outgoer.application.OutgoerApplication
import com.outgoer.base.ActivityManager
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.chat.NewChatActivity
import com.outgoer.ui.splash.NewSplashActivity
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

class NotificationService : FirebaseMessagingService() {

    companion object {
        private const val TITLE = "title"
        private const val BODY = "body"
        private const val CHANNEL_ID = "Default"
        private const val CHANNEL_NAME = "Default channel"

        const val NOTIFICATION_TYPE = "notification_type"
        const val PAYLOAD = "payload"

        const val N_TYPE_CHAT = "chat"
        const val N_TYPE_POST_SPONTY = "post_sponty"

        const val N_TYPE_SPONTY = "tag_sponty"

        const val N_TYPE_FOLLOW = "follow"
        const val EXTRA_PARAM_FOLLOW_BY = "follow_by"

        const val EXTRA_PARAM_POST_ID = "post_id"
        const val N_TYPE_POST_LIKED = "post_liked"
        const val N_TYPE_POST_COMMENT = "post_comment"
        const val N_TYPE_POST_COMMENT_LIKED = "post_comment_liked"
        const val N_TYPE_POST_COMMENT_REPLY = "post_comment_replay"
        const val N_TYPE_POST_TAG_POST = "tag_post"

        const val EXTRA_PARAM_REEL_ID = "reel_id"
        const val N_TYPE_REEL_LIKED = "reels_liked"
        const val N_TYPE_REEL_COMMENT = "reels_comment"
        const val N_TYPE_REEL_COMMENT_LIKED = "reels_comment_liked"
        const val N_TYPE_REEL_COMMENT_REPLY = "reels_comment_replay"
        const val N_TYPE_REEL_TAG_REEL = "tag_reels"

        const val N_TYPE_HOST_INVITE = "host_invite"
        const val N_TYPE_LIVE_EVENT = "live_event"
        const val N_TYPE_SPONTY_JOINED = "sponty_joined"
        const val N_TYPE_SPONTY_COMMENT = "sponty_comment"
        const val N_TYPE_SPONTY_LIKED = "sponty_liked"

        const val N_TYPE_EVENT_REQUEST = "event_request"
        const val N_TYPE_EVENT_REQUEST_ACCEPTED = "event_request_accepted"
        const val N_TYPE_EVENT_REQUEST_REJECTED = "event_request_rejected"
    }

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Timber.tag("FCM <><><>").i("Title %s", remoteMessage.notification?.title)
        Timber.tag("FCM <><><>").i("Body %s", remoteMessage.notification?.body)
        Timber.tag("FCM <><><>").i("RemoteMessage %s", remoteMessage.data.toProperties())
        Timber.tag("FCM <><><>").i("RemoteMess %s", remoteMessage)

        val activity = ActivityManager.getInstance().foregroundActivity
        val notifyIntent: Intent?
        if (loggedInUserCache.getLoggedInUser()?.loggedInUser != null) {
            if (remoteMessage.data[NOTIFICATION_TYPE] != N_TYPE_CHAT) {
                RxBus.publish(RxEvent.UpdateNotificationBadge(true))
            }
            RxBus.publish(RxEvent.UpdateNotificationBadge(true))

            when (val notificationType = remoteMessage.data[NOTIFICATION_TYPE]) {

                N_TYPE_EVENT_REQUEST,N_TYPE_EVENT_REQUEST_ACCEPTED, N_TYPE_EVENT_REQUEST_REJECTED -> {
                    val payload = remoteMessage.data[PAYLOAD]
                    notifyIntent = if (payload != null) {
                        val eventData = Gson().fromJson(payload, EventData::class.java)

                        if (eventData != null) {
                            HomeActivity.launchFromEventNotification(this, eventData)

                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    } else {
                        Intent(this, HomeActivity::class.java)
                    }
                }

                N_TYPE_POST_SPONTY -> {
                    val payload = remoteMessage.data[PAYLOAD]
                    notifyIntent = if (payload != null) {
                        val spontyInfo = Gson().fromJson(payload, SpontyResponse::class.java)

                        if (spontyInfo != null) {
                            HomeActivity.launchFromSpontyNotification(this, spontyInfo)

                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    } else {
                        Intent(this, HomeActivity::class.java)
                    }
                }
                N_TYPE_SPONTY, N_TYPE_SPONTY_JOINED, N_TYPE_SPONTY_COMMENT, N_TYPE_SPONTY_LIKED -> {
                    notifyIntent = if (activity == null || activity !is NewChatActivity) {
                        val payload = remoteMessage.data[PAYLOAD]
                        if (payload != null) {
                            val notificationSponty = Gson().fromJson(payload, NotificationSponty::class.java)

                            notificationSponty.objectType = notificationType
                            if (notificationSponty != null) {
                                HomeActivity.launchFromSpontyNotification(this, notificationSponty)
                            } else {
                                Intent(this, HomeActivity::class.java)
                            }
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    } else {
                        null
                    }
                }
                N_TYPE_CHAT -> {
                    notifyIntent = if (activity == null || activity !is NewChatActivity) {
                        val payload = remoteMessage.data[PAYLOAD]
                        if (payload != null) {
                            val chatConversationInfo = Gson().fromJson(payload, ChatConversationInfo::class.java)

                            if (chatConversationInfo != null) {
                                if(chatConversationInfo.chatType.equals(resources.getString(R.string.label_group)))
                                    chatConversationInfo.filePath = chatConversationInfo.profileUrl

                                HomeActivity.launchFromChatNotification(this, chatConversationInfo)
                            } else {
                                Intent(this, HomeActivity::class.java)
                            }
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    } else {
                        null
                    }
                }
                N_TYPE_FOLLOW -> {
                    val payload = remoteMessage.data[PAYLOAD]
                    notifyIntent = if (payload != null) {
                        val followObj = JSONObject(payload)
                        if (followObj.has(EXTRA_PARAM_FOLLOW_BY)) {
                            val otherUserId = followObj.optInt(EXTRA_PARAM_FOLLOW_BY, -1)
                            if (otherUserId != -1) {
                                HomeActivity.launchFromFollowNotification(this, otherUserId)
                            } else {
                                Intent(this, HomeActivity::class.java)
                            }
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    } else {
                        Intent(this, HomeActivity::class.java)
                    }
                }
                N_TYPE_POST_LIKED, N_TYPE_POST_COMMENT, N_TYPE_POST_COMMENT_LIKED, N_TYPE_POST_COMMENT_REPLY, N_TYPE_POST_TAG_POST -> {
                    val payload = remoteMessage.data[PAYLOAD]
                    notifyIntent = if (payload != null) {
                        val postObj = JSONObject(payload)
                        if (postObj.has(EXTRA_PARAM_POST_ID)) {
                            val postId = postObj.optInt(EXTRA_PARAM_POST_ID, -1)
                            if (postId != -1) {
                                if (notificationType == N_TYPE_POST_COMMENT || notificationType == N_TYPE_POST_COMMENT_LIKED || notificationType == N_TYPE_POST_COMMENT_REPLY) {
                                    HomeActivity.launchFromPostNotification(this, postId, showComments = true)
                                } else if (notificationType == N_TYPE_POST_TAG_POST) {
                                    HomeActivity.launchFromPostNotification(this, postId, showTaggedPeople = true)
                                } else {
                                    HomeActivity.launchFromPostNotification(this, postId)
                                }
                            } else {
                                Intent(this, HomeActivity::class.java)
                            }
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    } else {
                        Intent(this, HomeActivity::class.java)
                    }
                }
                N_TYPE_REEL_LIKED, N_TYPE_REEL_COMMENT, N_TYPE_REEL_COMMENT_LIKED, N_TYPE_REEL_COMMENT_REPLY, N_TYPE_REEL_TAG_REEL -> {
                    val payload = remoteMessage.data[PAYLOAD]
                    notifyIntent = if (payload != null) {
                        val reelObj = JSONObject(payload)
                        if (reelObj.has(EXTRA_PARAM_REEL_ID)) {
                            val reelId = reelObj.optInt(EXTRA_PARAM_REEL_ID, -1)
                            if (reelId != -1) {
                                if (notificationType == N_TYPE_REEL_COMMENT || notificationType == N_TYPE_REEL_COMMENT_LIKED || notificationType == N_TYPE_REEL_COMMENT_REPLY) {
                                    HomeActivity.launchFromReelNotification(this, reelId, showComments = true)
                                } else if (notificationType == N_TYPE_REEL_TAG_REEL) {
                                    HomeActivity.launchFromReelNotification(this, reelId, showTaggedPeople = true)
                                } else {
                                    HomeActivity.launchFromReelNotification(this, reelId)
                                }
                            } else {
                                Intent(this, HomeActivity::class.java)
                            }
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    } else {
                        Intent(this, HomeActivity::class.java)
                    }
                }
                N_TYPE_LIVE_EVENT, N_TYPE_HOST_INVITE -> {
                    val payload = remoteMessage.data[PAYLOAD]
                    notifyIntent = if (payload != null) {
                        val liveEventInfo = Gson().fromJson(payload, LiveEventInfo::class.java)
                        if (liveEventInfo != null) {
                            liveEventInfo.isCoHost = liveEventInfo.isCoHostNotification
                            liveEventInfo.hostStatus = liveEventInfo.hostStatusNotification
                            loggedInUserCache.invitedAsCoHost(liveEventInfo)
                            HomeActivity.launchFromLiveNotification(this, liveEventInfo)
                        } else {
                            Intent(this, HomeActivity::class.java)
                        }
                    } else {
                        Intent(this, HomeActivity::class.java)
                    }
                }

                else -> {
                    notifyIntent = Intent(this, HomeActivity::class.java)
                }
            }
        } else {
            notifyIntent = Intent(this, NewSplashActivity::class.java)
        }

        if (notifyIntent == null) {
            return
        }

        notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val notifyPendingIntent = PendingIntent.getActivity(
            this, 0, notifyIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setAutoCancel(true)
                .setContentIntent(notifyPendingIntent)
        if (!remoteMessage.notification?.title.isNullOrEmpty()) {
            builder.setContentTitle(remoteMessage.notification?.title)
        } else {
            if (!remoteMessage.data?.get("title").isNullOrEmpty()) {
                builder.setContentTitle(remoteMessage.data?.get("title"))
            }
        }
        if (!remoteMessage.notification?.body.isNullOrEmpty()) {
            builder.setContentTitle(remoteMessage.notification?.body)
        }
        if (remoteMessage.data[NOTIFICATION_TYPE] == N_TYPE_CHAT) {
            builder.setContentTitle(remoteMessage.data[TITLE])
            if (!remoteMessage.data[BODY].isNullOrEmpty()) {
                builder.setContentText(remoteMessage.data[BODY])
            } else {
                val payload = remoteMessage.data[PAYLOAD]
                if (payload != null) {
                    val chatConversationInfo = Gson().fromJson(payload, ChatConversationInfo::class.java)
                    builder.setContentText(chatConversationInfo.fileType?.name ?: "")
                }
            }
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O) {
            builder.setContentTitle(
                remoteMessage.notification?.body ?: resources.getString(R.string.app_name)
            )
        }
        if (remoteMessage.data[NOTIFICATION_TYPE] == N_TYPE_CHAT) {
            val payload = remoteMessage.data[PAYLOAD]
            if (payload != null) {
                val chatConversationInfo =
                    Gson().fromJson(payload, ChatConversationInfo::class.java)
                if (chatConversationInfo.senderId != loggedInUserCache.getUserId()) {
                    manager.notify(0, builder.build())
                }
            }
        } else {
            manager.notify(0, builder.build())
        }
    }

    override fun onCreate() {
        super.onCreate()
        OutgoerApplication.component.inject(this)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }
}
