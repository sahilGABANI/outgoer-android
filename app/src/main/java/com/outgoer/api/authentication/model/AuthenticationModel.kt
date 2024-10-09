package com.outgoer.api.authentication.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.venue.model.VenueAvailabilityRequest
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.api.venue.model.VenueMediaRequest
import kotlinx.parcelize.Parcelize

@Parcelize
data class OutgoerUser(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_type")
    val userType: String? = "",

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("phone")
    val phone: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("thumbnail")
    val thumbnail: String? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("avatarUrl")
    val avatarUrl: String? = null,

    @field:SerializedName("about")
    val about: String? = null,

    @field:SerializedName("venue_address")
    val venueAddress: String? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("thumbnailUrl")
    val thumbnailUrl: String? = null,

    @field:SerializedName("total_post")
    val totalPost: Int? = null,

    @field:SerializedName("total_reels")
    val totalReels: Int? = null,

    @field:SerializedName("total_followers")
    var totalFollowers: Int? = null,

    @field:SerializedName("total_following")
    var totalFollowing: Int? = null,

    @field:SerializedName("platform")
    val platform: String? = null,

    @field:SerializedName("push_token")
    val pushToken: String? = null,

    @field:SerializedName("deactive")
    val deactive: Int? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("email_verified")
    val emailVerified: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field: SerializedName("follow_status")
    var followStatus: Int? = null,

    @field: SerializedName("following_status")
    var followingStatus: Int? = null,

    @field:SerializedName("venue_category")
    val venueCategories: String? = null,

    @field:SerializedName("category")
    val venueCategory: VenueCategory? = null,

    @field:SerializedName("web_link")
    val webLink: String? = null,

    @field:SerializedName("web_title")
    val webTitle: String? = null,

    @field:SerializedName("fb_link")
    val fbLink: String? = null,

    @field:SerializedName("tag_ids")
    val tagIds: String? = null,

    @field:SerializedName("mention_ids")
    val mentionIds: String? = null,

    @field:SerializedName("mentions")
    val mentions: ArrayList<MentionResponse>? = null,

    @field: SerializedName("description")
    var description: String? = null,

    @field: SerializedName("availibility")
    var availibility: ArrayList<VenueAvailabilityRequest>? = arrayListOf(),

    @field: SerializedName("gallery")
    var gallery: ArrayList<VenueMediaRequest>? = arrayListOf(),

    @field:SerializedName("reels")
    val reels: ArrayList<ReelInfo>? = null,

    @field:SerializedName("total_review")
    val totalReview: Int? = null,

    @field:SerializedName("review_avg")
    val reviewAvg: Double? = null,

    @field:SerializedName("is_visible")
    val isVisible: Int? = null,

    var isSelected: Boolean = false,

    var isAdmin: Boolean = false,

    @field: SerializedName("mutual_friend")
     var mutualFriend: List<MutualFriend>?,

    @field: SerializedName("other_mutual_friend")
    var otherMutualFriend :Int? = null,

    @field: SerializedName("cover_image")
    var coverImage: String? = null,

    @field: SerializedName("broadcast_message")
    var broadcastMessage: String? = null,

    @field:SerializedName("at_venue_count")
    val atVenueCount: Int? = null,

    @field:SerializedName("venue")
    val venue: Venue? = null,

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

    @field: SerializedName("venue_tags")
    var venueTags: String? = null,

    @field:SerializedName("tag_count")
    val tagCount: Int? = null,

    @field:SerializedName("check_in_count")
    val checkInCount: Int? = null,

    @field:SerializedName("message_count")
    val messageCount: Int? = null,

    @field:SerializedName("notification_count")
    val notificationCount: Int? = null,

    @field:SerializedName("badge_request")
    val badgeRequest: Int? = null,

    @field:SerializedName("is_miles")
    val isMiles: Int? = null,

    @field: SerializedName("phone_code")
    var phoneCode: String? = null,

    @field: SerializedName("country")
    var country: String? = null,

) : Parcelable {}

@Parcelize
data class Venue(
    @field:SerializedName("id")
    var id:Int? = null ,
    @field:SerializedName("user_id")
    var userId:Int? = null ,
    @field:SerializedName("name")
    var name: String? = null,
    @field:SerializedName("username")
    var username: String? = null,
    @field:SerializedName("user_type")
    var userType: String? = null,
    @field:SerializedName("avatar")
    var avatar: String? = null,
    @field:SerializedName("profile_verified")
    var profileVerified: String? = null,
    @field:SerializedName("at_venue_count")
    var atVenueCount: String? = null
): Parcelable

@Parcelize
data class MutualFriend(
    @field:SerializedName("id")
    var id:Int? = null ,
    @field:SerializedName("name")
    var name: String? = null,
    @field:SerializedName("username")
    var username: String? = null,
    @field:SerializedName("avatar")
    var avatar: String? = null
) : Parcelable{}


@Parcelize
data class MentionResponse(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("mention_id")
    val mentionId: Int? = null,

    @field:SerializedName("mention_type")
    val mentionType: String? = null,

    @field:SerializedName("username")
    val username: String? = null,
) : Parcelable

data class LoggedInUser(
    val loggedInUser: OutgoerUser,
    val loggedInUserToken: String?,
)

//-----------------------Request-----------------------
data class RegisterRequest(
    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("password")
    val password: String? = null,

    @field: SerializedName("latitude")
    var latitude: Double? = null,

    @field: SerializedName("longitude")
    var longitude: Double? = null,
)

data class LoginRequest(
    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("password")
    val password: String? = null,
)

data class CheckSocialIdExistRequest(
    @field:SerializedName("social_id")
    val socialId: String? = null,
)

data class SocialMediaLoginRequest(
    @field:SerializedName("social_id")
    val socialId: String? = null,

    @field:SerializedName("social_platform")
    val socialPlatform: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field: SerializedName("latitude")
    var latitude: Double? = null,

    @field: SerializedName("longitude")
    var longitude: Double? = null,
)

data class VerifyUserRequest(
    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("verification_code")
    val verificationCode: String? = null,
)

data class ResendCodeRequest(
    @field:SerializedName("email")
    val email: String? = null,
)

data class ForgotPasswordRequest(
    @field:SerializedName("email")
    val email: String? = null,
)

data class ResetPasswordRequest(
    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("password")
    val password: String? = null,
)

data class UpdateNotificationTokenRequest(
    @field:SerializedName("push_token")
    val firebaseToken: String? = null,

    @field:SerializedName("platform")
    val platform: String? = null,

    @field:SerializedName("device_id")
    val deviceId: String? = null,
)

//-----------------------Response-----------------------
data class FBGraphResponse(
    @field:SerializedName("id")
    val facebookId: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("email")
    val emailId: String? = null,
)

@Keep
data class CheckSocialIdExistResponse(
    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("username")
    val username: String? = null,
)

@Keep
data class UpdateNotificationToken(
    @field:SerializedName("notificationStatus")
    val notificationStatus: Boolean,
)

data class ChekUsernameRequest(
    @field:SerializedName("username")
    val username: String
)

@Keep
data class ChekUsernameResponse(
    @field:SerializedName("username_exist")
    val usernameExist: Int,
)

data class AddUsernameEmail(
    val username: String,
    val email: String,
)

data class AccountActivationRequest(
    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("reason")
    val reason: String? = null,
)

@Keep
data class AwsInformation(
    @field:SerializedName("AWS_ACCESS_KEY_ID")
    val awsAccessKeyId: String? = null,

    @field:SerializedName("AWS_SECRET_ACCESS_KEY")
    val awsSecretAccessKey: String? = null,

    @field:SerializedName("AWS_DEFAULT_REGION")
    val awsDefaultRegion: String? = null,

    @field:SerializedName("AWS_BUCKET")
    val awsBucket: String? = null,

    @field:SerializedName("AWS_URL")
    val awsBaseUrl: String? = null
)
