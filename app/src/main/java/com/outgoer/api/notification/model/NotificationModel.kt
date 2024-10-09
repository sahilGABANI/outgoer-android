package com.outgoer.api.notification.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class NotificationInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("sender_id")
    val senderId: Int? = null,

    @field:SerializedName("receiver_id")
    val receiverId: Int? = null,

    @field:SerializedName("object_id")
    val objectId: Int? = null,

    @field:SerializedName("object_type")
    val objectType: String? = null,

    @field:SerializedName("post_reel_id")
    val postReelId: Int? = null,

    @field:SerializedName("notification_type")
    val notificationType: String? = null,
    @field:SerializedName("title")
    val title: String? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("is_read")
    var isRead: Int,

    @field:SerializedName("human_readable_time")
    val humanReadableTime: String? = null,

    @field:SerializedName("post_image_url")
    val postImageUrl: String? = null,

    @field:SerializedName("reel_image_url")
    val reelImageUrl: String? = null,

    @field:SerializedName("sender")
    val sender: SenderInfo? = null,
)

@Keep
data class SenderInfo(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,
)

sealed class NotificationActionState {
    data class UpdateReadStatus(val notificationInfo: NotificationInfo) : NotificationActionState()
    data class RowViewClick(val notificationInfo: NotificationInfo) : NotificationActionState()
    data class UserProfileClick(val notificationInfo: NotificationInfo) : NotificationActionState()
}

data class UpdateNotificationReadStatusRequest(
    @field:SerializedName("notification_ids")
    val notificationIds: List<Int>,
)

@Keep
data class UpdateNotificationReadStatus(
    @field:SerializedName("notificationStatus")
    val notificationStatus: Boolean,
)