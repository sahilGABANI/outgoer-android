package com.outgoer.api.follow.model

import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.outgoer.api.venue.model.VenueListInfo
import kotlinx.parcelize.Parcelize

data class SuggestedUser(
    val uId: String? = null,
    val uName: String? = null,
    val uPass: String? = null,
    val avatar: String? = null
)


@Parcelize
data class FollowUser(
    @field: SerializedName("id")
    val id: Int,

    @field: SerializedName("name")
    val name: String? = null,

    @field: SerializedName("username")
    val username: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field: SerializedName("email")
    val email: String? = null,

    @field: SerializedName("about")
    val about: String? = null,

    @field: SerializedName("avatar")
    val avatar: String? = null,

    @field: SerializedName("total_followers")
    var totalFollowers: Int? = null,

    @field: SerializedName("total_following")
    val totalFollowing: Int? = null,

    @field: SerializedName("follow_status")
    var followStatus: Int? = null,

    @field: SerializedName("following_status")
    val followingStatus: Int? = null,

    @field:SerializedName("avatarUrl")
    val avatarUrl: String? = null,

    @field:SerializedName("thumbnailUrl")
    val thumbnailUrl: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,


    @Expose(serialize = false, deserialize = false)
    var isInvited: Boolean = false,

    @Expose(serialize = false, deserialize = false)
    var isAlreadyInvited: Boolean = false,

    var isSelected: Boolean = false,

    var isAdmin: Boolean = false
) : Parcelable

data class AcceptRejectRequest(
    @field: SerializedName("user_id")
    val userId: Int
)

data class GetFollowersAndFollowingRequest(
    @field: SerializedName("user_id")
    val userId: Int,

    @field: SerializedName("search")
    val search: String? = null,
)

sealed class PlaceFollowActionState {
    data class FollowClick(val followUser: VenueListInfo) : PlaceFollowActionState()
    data class FollowingClick(val followUser: VenueListInfo) : PlaceFollowActionState()
    data class UserProfileClick(val followUser: VenueListInfo) : PlaceFollowActionState()
}


sealed class FollowActionState {
    data class FollowClick(val followUser: FollowUser) : FollowActionState()
    data class FollowingClick(val followUser: FollowUser) : FollowActionState()
    data class UserProfileClick(val followUser: FollowUser) : FollowActionState()
}
