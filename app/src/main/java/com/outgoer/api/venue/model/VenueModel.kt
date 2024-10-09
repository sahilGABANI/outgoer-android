package com.outgoer.api.venue.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.outgoer.api.authentication.model.Venue
import com.outgoer.api.event.model.MutualFriends
import com.outgoer.api.reels.model.ReelInfo
import kotlinx.parcelize.Parcelize

data class GetMapNearPlacesCategoryRequest(
    @field: SerializedName("category_id")
    val categoryId: Int?,

    @field: SerializedName("value")
    val value: ArrayList<Double>,

    @field:SerializedName("search")
    val search: String? = null
)

data class GetMapVenueByCategoryRequest(
    @field: SerializedName("category_id")
    val categoryId: Int?,

    @field: SerializedName("latitude")
    val latitude: String,

    @field: SerializedName("longitude")
    val longitude: String,

    @field:SerializedName("search")
    val search: String? = null
)

data class GetVenueListRequest(
    @field: SerializedName("search")
    val search: String,

    @field: SerializedName("latitude")
    val latitude: String,

    @field: SerializedName("longitude")
    val longitude: String,
)

@Parcelize
@Keep
data class VenueCategory(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("category_name")
    val categoryName: String? = null,

    @field:SerializedName("image")
    val image: String? = null,

    @field:SerializedName("pin_image")
    val pinImage: String? = null,

    @field:SerializedName("thumbnail_image")
    val thumbnailImage: String? = null,

    @field:SerializedName("pin_image_url")
    val pinImageUrl: String? = null,

    @field:SerializedName("register_thumbnail")
    val registerThumbnail: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @Expose(serialize = false, deserialize = false)
    var isSelected: Boolean = false,
) : Parcelable

@Parcelize
@Keep
data class VenueMapInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("broadcast_message")
    val broadcastMessage: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("about")
    val about: String? = null,

    @field:SerializedName("venue_address")
    val venueAddress: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("latitude")
    var latitude: String? = null,

    @field:SerializedName("longitude")
    var longitude: String? = null,

    @field:SerializedName("category_id")
    val categoryId: String? = null,

    @field:SerializedName("category_name")
    val categoryName: String? = null,

    @field:SerializedName("distance")
    val distance: Double? = 0.0,

    @field:SerializedName("follow_status")
    var followStatus: Int? = null,

    @field:SerializedName("following_status")
    val followingStatus: Int? = null,

    @field:SerializedName("category")
    val category: ArrayList<VenueCategory>? = null,

    @field:SerializedName("venue_favourite_status")
    var venueFavouriteStatus: Int? = null,

    @field:SerializedName("review_avg")
    var reviewAvg: Double? = null,

    @field:SerializedName("total_review")
    var totalReview: Double? = null,

    @field:SerializedName("venue_checkin_status")
    var venueCheckinStatus: Int? = null,

    @field:SerializedName("venue_checkin_date")
    var venueCheckinDate: String? = null,

    @field:SerializedName("at_venue_count")
    var atVenueCount: Int? = null,

    @field:SerializedName("is_tagged")
    var isTagged: Int? = null,

    @field:SerializedName("is_post")
    var isPost: Int? = null,

    @field:SerializedName("is_reel")
    var isReel: Int? = null,

    @field:SerializedName("post_count")
    var postCount: Int? = null,

    @field:SerializedName("reel_count")
    var reelCount: Int? = null,

    @field:SerializedName("sponty_count")
    var spontyCount: Int? = null,

    @field:SerializedName("story_count")
    var storyCount: Int? = null,

    @field:SerializedName("is_live")
    var isLive: Int? = null,

    @field:SerializedName("live_id")
    var liveId: Int? = null,

    @field:SerializedName("mutual")
    val mutal: ArrayList<MutualFriends>? = arrayListOf(),

    @field:SerializedName("other_mutual_friend")
    val otherMutualFriend: Int? = null
) : Parcelable

@Parcelize
@Keep
data class VenueListInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("about")
    val about: String? = null,

    @field:SerializedName("venue_address")
    val venueAddress: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("category_id")
    val categoryId: Int? = null,

    @field:SerializedName("category_name")
    val categoryName: String? = null,

    @field:SerializedName("distance")
    val distance: Double? = null,

    @field:SerializedName("follow_status")
    var followStatus: Int? = null,

    @field:SerializedName("following_status")
    val followingStatus: Int? = null,

    @field:SerializedName("venue_favourite_status")
    var venueFavouriteStatus: Int? = null,

    @field:SerializedName("review_avg")
    var reviewAvg: Double?,

    @field:SerializedName("total_review")
    var totalReview: Int? = null,

    @field:SerializedName("at_venue_count")
    var atVenueCount: Double? = null,

    @field:SerializedName("post_count")
    val postCount: Int? = null,

    @field:SerializedName("reel_count")
    val reelCount: Int? = null,

    @field:SerializedName("sponty_count")
    val spontyCount: Int? = null,

    @field:SerializedName("story_count")
    val storyCount: Int? = null,

    @field:SerializedName("is_live")
    val isLive: Int? = null,

    @field:SerializedName("live_id")
    val liveId: Int? = null,
) : Parcelable

enum class MapVenueUserType(val type: String) {
    USER("user"),
    VENUE_OWNER("venue_owner")
}

data class GetOtherNearVenueRequest(
    @field: SerializedName("category_id")
    val categoryId: Int?,

    @field: SerializedName("venue_id")
    val venueId: Int?,

    @field:SerializedName("search")
    val search: String? = null
)

data class NewByVenueRequest(
    @field: SerializedName("category_id")
    val categoryId: Int
)

data class GetVenueDetailRequest(
    @field: SerializedName("venue_id")
    val venueId: Int,
)

data class CheckInOutRequest(
    @field: SerializedName("venue_id")
    val venueId: Int,

    @field: SerializedName("status")
    val status: Int,
)

@Parcelize
@Keep
data class VenueDetail(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("phone")
    val phone: String? = null,

    @field:SerializedName("email_verified")
    val emailVerified: Int? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("about")
    val about: String? = null,

    @field:SerializedName("category_id")
    val categoryId: String? = null,

    @field:SerializedName("venue_address")
    val venueAddress: String? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("distance")
    val distance: Double? = null,

    @field:SerializedName("platform")
    val platform: String? = null,

    @field:SerializedName("push_token")
    val pushToken: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("deactive")
    val deactive: Int? = null,

    @field:SerializedName("gallery_count")
    val galleryCount: Int? = null,

    @field:SerializedName("events_count")
    val eventsCount: Int? = null,

    @field:SerializedName("total_followers")
    val totalFollowers: Int? = null,

    @field:SerializedName("total_following")
    val totalFollowing: Int? = null,

    @field:SerializedName("category")
    val category: ArrayList<VenueCategory>? = null,

    @field:SerializedName("gallery")
    val gallery: ArrayList<VenueGalleryItem>? = null,

    @field:SerializedName("availibility")
    val availibility: ArrayList<VenueAvailabilityRequest>? = null,

    @field:SerializedName("events")
    val events: ArrayList<VenueEventInfo>? = arrayListOf(),

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("review_avg")
    var reviewAvg: Double? = null,

    @field:SerializedName("total_review")
    var totalReview: Int? = null,

    @field:SerializedName("reels")
    val reels: ArrayList<ReelInfo>? = null,

    @field: SerializedName("description")
    var description: String? = null,

    @field: SerializedName("cover_image")
    var coverImage: String? = null,

    @field:SerializedName("post_count")
    val postCount: Int? = null,

    @field:SerializedName("reel_count")
    val reelCount: Int? = null,

    @field:SerializedName("sponty_count")
    val spontyCount: Int? = null,

    @field:SerializedName("story_count")
    val storyCount: Int? = null,

    @field:SerializedName("is_live")
    val isLive: Int? = null,

    @field:SerializedName("live_id")
    val liveId: Int? = null,

    @field:SerializedName("tag_count")
    val tagCount: Int? = null,

    @field:SerializedName("check_in_count")
    val checkInCount: Int? = null,

    @field:SerializedName("venue_tags")
    val venueTags: String? = null,

    @field:SerializedName("message_count")
    val messageCount: Int? = null,

    @field:SerializedName("isCheckedIn")
    val isCheckedIn: Boolean? = false,

    @field:SerializedName("checkin_user")
    val checkInUser: Venue? = null,

    @field:SerializedName("notification_count")
    val notificationCount: Int? = null,

    @field:SerializedName("badge_request")
    val badgeRequest: Int? = null,

    @field: SerializedName("phone_code")
    var phoneCode: String? = null,

    @field:SerializedName("venue_favourite_status")
    var venueFavouriteStatus: Int = 0,

    @field:SerializedName("follow_status")
    var followStatus: Int = 0,

    @field: SerializedName("broadcast_message")
    var broadcastMessage: String? = null,

    ) : Parcelable

@Parcelize
@Keep
data class VenueEventInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("event_name")
    val eventName: String? = null,

    @field:SerializedName("event_details")
    val eventDetails: String? = null,

    @field:SerializedName("event_start_date")
    val eventStartDate: String? = null,

    @field:SerializedName("event_end_date")
    val eventEndDate: String? = null,

    @field:SerializedName("event_image")
    val eventImage: String? = null,

    @field:SerializedName("event_location")
    val eventLocation: String? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("distance")
    val distance: Double? = null,

    @field:SerializedName("event_request_status")
    val eventRequestStatus: Boolean? = null,

    val res: Int? = null
) : Parcelable

data class SectionViewInfo(
    val sectionText: String,
    val isSeeAllEnable: Boolean,
)

sealed class SectionViewSectionItem {
    data class LatestEventSection(val sectionViewInfo: SectionViewInfo) : SectionViewSectionItem()
    data class OtherNearPlacesSection(val sectionViewInfo: SectionViewInfo) :
        SectionViewSectionItem()
}

data class GetVenueGalleryRequest(
    @field: SerializedName("venue_id")
    val venueId: Int,

    @field: SerializedName("type")
    val type: Int,
)

data class AddRemoveFavouriteVenueRequest(
    @field: SerializedName("venue_id")
    val venueId: Int
)

sealed class OtherNearPlaceClickState {
    data class OtherNearPlaceClick(val venueMapInfo: VenueMapInfo) : OtherNearPlaceClickState()
    data class AddRemoveVenueFavClick(val venueMapInfo: VenueMapInfo) : OtherNearPlaceClickState()
    data class DirectionViewClick(val venueMapInfo: VenueMapInfo) : OtherNearPlaceClickState()
}

sealed class VenueViewClickState {
    data class VenueViewClick(val venueListInfo: VenueListInfo) : VenueViewClickState()
    data class AddRemoveVenueFavClick(val venueListInfo: VenueListInfo) : VenueViewClickState()
}

data class GetVenueAllGalleryRequest(
    @field: SerializedName("venue_id")
    val venueId: Int? = null,
)

data class GetVenueFollowersRequest(
    @field: SerializedName("user_id")
    val userId: Int? = null,

    @field: SerializedName("search")
    val search: String? = null,
)

@Parcelize
@Keep
data class VenueGalleryItem(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("type")
    val type: Int? = null,

    @field:SerializedName("media")
    val media: String? = null,

    @field:SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @field:SerializedName("gifthumbnail_url")
    val gifthumbnailUrl: String? = null,

    @field:SerializedName("video_url")
    val videoUrl: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,
) : Parcelable

data class AddVenueMediaListRequest(
    @field: SerializedName("gallery")
    val galleryList: List<AddVenueMediaItemRequest>
)

data class AddVenueMediaItemRequest(
    @field: SerializedName("type")
    val type: Int,

    @field: SerializedName("media")
    val media: String,
)

data class DeleteVenueGalleryRequest(
    @field: SerializedName("ids")
    val venueIdList: List<Int?>? = null,
)

data class AddUpdateEventRequest(
    @field:SerializedName("event_image")
    val eventImage: String? = null,

    @field:SerializedName("event_name")
    val eventName: String? = null,

    @field:SerializedName("event_details")
    val eventDetails: String? = null,

    @field:SerializedName("event_location")
    val eventLocation: String? = null,

    @field:SerializedName("latitude")
    val latitude: Double? = null,

    @field:SerializedName("longitude")
    val longitude: Double? = null,

    @field:SerializedName("event_start_date")
    val eventStartDate: String? = null,

    @field:SerializedName("event_end_date")
    val eventEndDate: String? = null,
)

data class RequestJoinEventRequest(
    @field: SerializedName("event_id")
    val eventId: Int
)

data class RequestSearchVenue(
    @field: SerializedName("search")
    val search: String? = null
)


@Parcelize
data class RegisterVenueRequest(
    @field: SerializedName("name")
    var name: String? = null,

    @field: SerializedName("username")
    var username: String? = null,

    @field: SerializedName("email")
    var email: String? = null,

    @field: SerializedName("phone")
    var phone: String? = null,

    @field: SerializedName("password")
    var password: String? = null,

    @field: SerializedName("description")
    var description: String? = null,

    @field: SerializedName("venue_address")
    var venueAddress: String? = null,

    @field: SerializedName("latitude")
    var latitude: String? = null,

    @field: SerializedName("longitude")
    var longitude: String? = null,

    @field: SerializedName("avatar")
    var avatar: String? = null,

    @field: SerializedName("venue_category")
    var venueCategory: String? = null,

    @field: SerializedName("vanue_availibility")
    var vanueAvailibility: ArrayList<VenueAvailabilityRequest> = arrayListOf(),

    @field: SerializedName("venue_media")
    var venueMedia: ArrayList<VenueMediaRequest> = arrayListOf(),

    @field: SerializedName("cover_image")
    var coverImage: String? = null,

    @field: SerializedName("venue_tags")
    var venueTags: String? = null,

    @field: SerializedName("phone_code")
    var phoneCode: String? = null,
) : Parcelable

@Parcelize
data class VenueAvailabilityRequest(
    @field: SerializedName("day_name")
    val dayName: String? = null,

    @field: SerializedName("open_at")
    var openAt: ArrayList<String>? = null,

    @field: SerializedName("close_at")
    var closeAt: ArrayList<String>? = null,

    @field: SerializedName("status")
    var status: Int = 0,

    @Expose(serialize = false, deserialize = false)
    var id: Int = 0,

) : Parcelable

data class VenueTime(
    @field: SerializedName("open_at")
    var openAt: ArrayList<String>? = null,

    @field: SerializedName("close_at")
    var closeAt: ArrayList<String>? = null,
)


@Parcelize
data class VenueMediaRequest(
    @field: SerializedName("media")
    val media: String? = null,

    @field: SerializedName("type")
    var type: Int? = null
) : Parcelable

@Parcelize
data class AddVenueGalleryRequest(
    @field: SerializedName("gallery")
    val gallery: ArrayList<String> = arrayListOf(),

    @field: SerializedName("uid")
    val uid: ArrayList<String> = arrayListOf()
) : Parcelable


@Parcelize
@Keep
data class GeoFenceResponse(
    val id: Int,
    val name: String? = null,
    val venueStatus: Int? = null
) : Parcelable

sealed class VenueTimeSelectionClickState {
    data class OpensAtClick(val timeInfo: VenueAvailabilityRequest) : VenueTimeSelectionClickState()
    data class CloseAtClicks(val timeInfo: VenueAvailabilityRequest) : VenueTimeSelectionClickState()
    data class RemoveClicks(val timeInfo: VenueAvailabilityRequest) : VenueTimeSelectionClickState()
}