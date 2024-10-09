package com.outgoer.api.profile.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.outgoer.api.authentication.model.OutgoerUser
import kotlinx.parcelize.Parcelize

//-----------------------Request-----------------------

data class LocationUpdateRequest(
    @field: SerializedName("latitude")
    val latitude: String? = null,

    @field: SerializedName("longitude")
    val longitude: String? = null,
)

data class SearchUserListRequest(
    @field:SerializedName("search")
    val search: String,
)

data class SetVisibilityRequest(
    @field:SerializedName("is_visible")
    val isVisible: Int,
)

data class DeviceAccountRequest(
    @field:SerializedName("device_id")
    val deviceId: String,
)

data class SwitchDeviceAccountRequest(
    @field:SerializedName("device_id")
    val deviceId: String,

    @field:SerializedName("user_id")
    val userId: Int,
)

data class UpdateProfileRequest(
    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("image")
    val image: String? = null,

    @field:SerializedName("about")
    val about: String? = null,

    @field:SerializedName("web_link")
    val webLink: String? = null,

    @field:SerializedName("web_title")
    val webTitle: String? = null,

    @field:SerializedName("fb_link")
    val fbLink: String? = null,

    @field:SerializedName("tag_ids")
    val tagIds: String? = null,

    @field:SerializedName("mention_ids")
    val mentionIds: String? = null
)

data class GetUserProfileRequest(
    @field:SerializedName("user_id")
    val userId: Int?,

)

data class BlockUserListRequest(
    @field:SerializedName("page")
    val page: Int,

    @field:SerializedName("search")
    val search: String? = null
)

data class BlockUserRequest(
    @field:SerializedName("block_for")
    val blockFor: Int
)

data class ReportUserRequest(
    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("report_id")
    val reportId: Int
)

data class ReportEventRequest(
    @field:SerializedName("event_id")
    val eventId: Int,

    @field:SerializedName("report_id")
    val reportId: Int
)

sealed class SuggestedUserActionState {
    data class FollowButtonClick(val outgoerUser: OutgoerUser) : SuggestedUserActionState()
    data class UserProfileClick(val outgoerUser: OutgoerUser) : SuggestedUserActionState()
}

sealed class ProfileMenuBottomSheetState {
    object EditProfileClick : ProfileMenuBottomSheetState()
    object EditVenueClick : ProfileMenuBottomSheetState()
    object AboutClick : ProfileMenuBottomSheetState()
    object HelpClick : ProfileMenuBottomSheetState()
    object LogoutClick : ProfileMenuBottomSheetState()
    object DeactivateProfileClick : ProfileMenuBottomSheetState()
}


data class UpdateVenueRequest(
    @field:SerializedName("category_id")
    val categoryId: Int? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("venue_address")
    val venueAddress: String? = null,

    @field:SerializedName("latitude")
    val latitude: Double? = null,

    @field:SerializedName("longitude")
    val longitude: Double? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,
)

@Parcelize
data class NearByUserResponse(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("broadcast_message")
    var broadcastMessage: String? = null,

    @field:SerializedName("about")
    val about: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("distance")
    val distance: Double? = null,

    @field: SerializedName("follow_status")
    var followStatus: Int? = null,

    @field: SerializedName("following_status")
    var followingStatus: Int? = null,


    @field:SerializedName("post_count")
    val postCount: Int? = null,

    @field:SerializedName("reel_count")
    val reelCount: Int? = null,

    @field:SerializedName("sponty_count")
    val spontyCount: Int? = null,

    @field:SerializedName("story_count")
    var storyCount: Int? = null,

    @field:SerializedName("is_live")
    val isLive: Int? = null,

    @field:SerializedName("live_id")
    val liveId: Int? = null,
): Parcelable

data class BlockUserResponse(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("block_for")
    val blockFor: Int,

    @field:SerializedName("block_by")
    val blockBy: Int,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("badge_request")
    val badgeRequest: Int? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,
)