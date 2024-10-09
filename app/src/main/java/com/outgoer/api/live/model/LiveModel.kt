package com.outgoer.api.live.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

enum class LiveUserType(val type: Int) {
    NormalUser(0),
    HostUser(1)
}

enum class LiveStreamNoOfCoHost(val type: String) {
    FirstCoHost("firstCoHost"),
    SecondCoHost("secondCoHost"),
    ThirdCoHost("thirdCoHost"),
    FourthCoHost("fourthCoHost")
}

data class Time(
    val hours: Int,
    val min: Int,
    val second: Int
)

fun Int.secondToTime(): Time {
    val h = (this / 3600)
    val m = (this / 60 % 60)
    val s = (this % 60)
    return Time(h, m, s)
}

//Request
data class CreateLiveEventRequest(
    @field:SerializedName("event_name")
    val eventName: String? = null,

    @field:SerializedName("is_lock")
    val isLock: Int? = null,

    @field:SerializedName("password")
    val password: String? = null,

    @field:SerializedName("invite_ids")
    val inviteIds: String? = null
)

data class JoinLiveEventRequest(
    @field:SerializedName("channel_id")
    val channelId: String? = null,

    @field:SerializedName("role_type")
    val roleType: String = "RoleAttendee",
)

data class EndLiveEventRequest(
    @field:SerializedName("channel_id")
    val channelId: String? = null,
)

data class LiveEventWatchingUserRequest(
    @field:SerializedName("live_id")
    val liveId: Int
)

//Response
data class AllActiveEventInfo(
    @field:SerializedName("user_event")
    val userEventList: List<LiveEventInfo>? = null,

    @field:SerializedName("venue_event")
    val venueEventList: List<LiveEventInfo>? = null,
)

@Parcelize
data class LiveEventInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("event_name")
    val eventName: String? = null,

    @field:SerializedName("channel_id")
    val channelId: String,

    @field:SerializedName("is_lock")
    val isLock: Int? = null,

    @field:SerializedName("start_date")
    val startDate: String? = null,

    @field:SerializedName("expire_date")
    val expireDate: String? = null,

    @field:SerializedName("token")
    val token: String,

    @field:SerializedName("event_image")
    val eventImage: String? = null,

    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val userName: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("profileUrl")
    val profileUrl: String? = null,

    @field:SerializedName("userjoin")
    val userJoin: Int? = null,

    @field:SerializedName("thumbnailUrl")
    val thumbnailUrl: String? = null,

    @field:SerializedName("is_co_host")
    var isCoHost: Int? = null,

    @field:SerializedName("host_status")
    var hostStatus: Int? = null,

    @field:SerializedName("followers")
    val followers: Int? = null,

    //[{"key":"role_type","value":"RolePublisher","description":"RoleAttendee, RolePublisher","type":"text"}]
    @field:SerializedName("role_type")
    val roleType: String? = null,

    @field:SerializedName("venue_thumbnail_url")
    val venueThumbnailUrl: String? = null,

    @field:SerializedName("venue_video_url")
    val venueVideoUrl: String? = null,

    @field:SerializedName("venue_address")
    val venueAddress: String? = null,

    //==========================Use this only when from notification==========================
    @field:SerializedName("is_co_host_notification")
    val isCoHostNotification: Int? = null,

    @field:SerializedName("host_status_notification")
    val hostStatusNotification: Int? = null,
) : Parcelable {
    fun isPublisherRole(): Boolean {
        return roleType == ROLE_PUBLISHER
    }
}

@Keep
data class LiveEventWatchingCount(
    @field:SerializedName("live_watching_count")
    val liveWatchingCount: Int? = null
)

data class LiveRoomRequest(
    @field:SerializedName("channel_id")
    val channelId: String?,

    @field:SerializedName("live_id")
    val liveId: Int?,

    @field:SerializedName("user_id")
    val userId: Int?,
)

data class LiveRoomDisconnectRequest(
    @field:SerializedName("channel_id")
    val channelId: String?,

    @field:SerializedName("live_id")
    val liveId: Int?,

    @field:SerializedName("user_id")
    val userId: Int?,

    @field:SerializedName("is_kicked")
    val isKicked: Int? = null,
)

data class LiveEventVerifyRequest(
    @field:SerializedName("channel_id")
    val channelId: String? = null,

    @field:SerializedName("password")
    val password: String? = null,
)

@Parcelize
data class LiveEventSendOrReadComment(
    @field:SerializedName("channel_id")
    val channelId: String?,

    @field:SerializedName("live_id")
    val liveId: Int?,

    @field:SerializedName("user_id")
    val userId: Int?,

    @field:SerializedName("id")
    val id: String,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("profile_url")
    val profileUrl: String? = null,

    @field:SerializedName("comment")
    val comment: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    ) : Parcelable

data class CoHostRequest(
    @field:SerializedName("channel_id")
    val channelId: String? = null,
    //i.e 50,55
    @field:SerializedName("invite_ids")
    val inviteIds: String? = null,
)

data class RemoveHostRequest(
    @field:SerializedName("channel_id")
    val channelId: String,

    @field:SerializedName("live_id")
    val liveId: Int,

    @field:SerializedName("user_id")
    val userId: Int
)

data class LiveJoinResponse(
    @field:SerializedName("request_status")
    val requestStatus: Int? = null,

    @field:SerializedName("profileUrl")
    val profileUrl: String? = null,

    @field:SerializedName("live_id")
    val liveId: Int? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("role")
    val role: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("username")
    val username: String? = null
)

data class LiveEventKickUser(
    @field:SerializedName("channel_id")
    val channelId: String?,

    @field:SerializedName("live_id")
    val liveId: Int?,

    @field:SerializedName("user_id")
    val userId: Int?,
)

data class LiveEventEndSocketEvent(
    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("channel_id")
    val channelId: String? = null,

    @field:SerializedName("live_id")
    val liveId: Int? = null,
)

data class SendHeartSocketEvent(
    @field:SerializedName("id")
    val id: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("channel_id")
    val channelId: String? = null,

    @field:SerializedName("live_id")
    val liveId: Int? = null,
)

const val ROLE_PUBLISHER = "RolePublisher"
const val ROLE_ATTENDEE = "RoleAttendee"
const val ROLE_ADMIN = "RoleAdmin"