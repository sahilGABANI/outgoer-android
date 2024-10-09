package com.outgoer.api.event.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.outgoer.api.post.model.EventMediaResponse
import com.outgoer.api.post.model.PostImage
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.api.venue.model.VenueMapInfo
import kotlinx.android.parcel.Parcelize

data class EventListData(
    @field:SerializedName("upcomming")
    val upcomming: ArrayList<EventData> = arrayListOf(),

    @field:SerializedName("ongoing")
    val ongoing: ArrayList<EventData> = arrayListOf(),
)

@Parcelize
data class EventData(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("description")
    val description: String? = null,

    @field:SerializedName("date_time")
    val dateTime: String? = null,

    @field:SerializedName("mode_of_event")
    val modeOfEvent: String? = null,

    @field:SerializedName("location")
    val location: String? = null,

    @field:SerializedName("latitude")
    val latitude: String,

    @field:SerializedName("longitude")
    val longitude: String,

    @field:SerializedName("more_info")
    val moreInfo: String? = null,

    @field:SerializedName("place_id")
    val placeId: String? = null,

    @field:SerializedName("distance")
    val distance: Double? = null,

    @field:SerializedName("review_avg")
    var reviewAvg: Double? = null,

    @field:SerializedName("total_review")
    var totalReview: Double? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("join_request_status")
    var joinRequestStatus: Boolean = false,

    @field:SerializedName("user")
    val user: User? = null,

    @field:SerializedName("first_media")
    val firstMedia: PostImage? = null,

    @field:SerializedName("media")
    val media: ArrayList<EventMediaResponse>? = null,

    @field:SerializedName("event_request")
    val eventRequest: EventRequest? = null,

    @field:SerializedName("venue_detail")
    val venueDetail: VenueMapInfo? = null,

    @field:SerializedName("category")
    val category: VenueCategory? = null,

    @field:SerializedName("mutual")
    val mutal: ArrayList<MutualFriends>? = arrayListOf(),

    @field:SerializedName("other_mutual_friend")
    val otherMutualFriend: Int? = null,

//new added
    @field:SerializedName("category_id")
    val categoryId: Int? = null,

    @field:SerializedName("venue_id")
    val venueId: Int? = null,

    @field:SerializedName("is_notify")
    val isNotify: Int? = null,

    @field:SerializedName("is_private")
    val isPrivate: Int? = null,

    @field:SerializedName("end_date_time")
    val endDateTime: String? = null,

    @field:SerializedName("video_url")
    val videoUrl: String? = null,

    @field:SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,
) : Parcelable

@Parcelize
data class User(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    ) : Parcelable

@Parcelize
data class EventRequest(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("event_id")
    val eventId: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("status")
    val status: Int
) : Parcelable

data class CreateEventResponse(
    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("description")
    val description: String? = null,

    @field:SerializedName("date_time")
    val dateTime: String? = null,

    @field:SerializedName("end_date_time")
    val endDateTime: String? = null,

    @field:SerializedName("mode_of_event")
    val modeOfEvent: Int? = null,

    @field:SerializedName("location")
    val location: String? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("place_id")
    val placeId: String? = null,

    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("media")
    val media: ArrayList<String> = arrayListOf(),

    @field:SerializedName("venue_id")
    val venueId: Int? = null,

    @field:SerializedName("category_id")
    val categoryId: Int? = null,

    @field:SerializedName("is_private")
    val isPrivate: Int? = null,
)

data class JoinRequest(
    @field:SerializedName("event_id")
    val eventId: Int
)

data class RequestResult(
    @field:SerializedName("event_id")
    val eventId: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("status")
    val status: Int
)

data class JoinRequestResponse(
    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("event_id")
    val eventId: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("created_at")
    val created_at: String? = null,

    @field:SerializedName("id")
    val id: Int
)


data class RequestList(
    @field:SerializedName("event_id")
    val eventId: Int,

    @field:SerializedName("status")
    val status: Int
)

data class RequestResponseList(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("event_id")
    var eventId: Int,

    @field:SerializedName("user_id")
    var userId: Int,

    @field:SerializedName("status")
    var status: Int,

    @field:SerializedName("created_at")
    var createdAt: String? = null,

    @field:SerializedName("updated_at")
    var updatedAt: String? = null,

    @field:SerializedName("name")
    var name: String? = null,

    @field:SerializedName("avatar")
    var avatar: String? = null
)

@Parcelize
data class MutualFriends(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("event_id")
    var eventId: Int,

    @field:SerializedName("user_id")
    var userId: Int,

    @field:SerializedName("name")
    var name: String? = null,

    @field:SerializedName("username")
    var username: String? = null,

    @field:SerializedName("email")
    var email: String? = null,

    @field:SerializedName("avatar")
    var avatar: String? = null,

    @field:SerializedName("user_type")
    var userType: String? = null
) : Parcelable

@Parcelize
data class GooglePlaces(
    @field:SerializedName("id")
    var id: String,

    @field:SerializedName("name")
    var name: String? = null,

    @field:SerializedName("venue_address")
    var venueAddress: String? = null,

    @field:SerializedName("user_type")
    var userType: String? = null,

    @field:SerializedName("review_avg")
    var reviewAvg: Double? = null,

    @field:SerializedName("avatar")
    var avatar: String? = null,

    @field:SerializedName("latitude")
    val latitude: Double? = null,

    @field:SerializedName("longitude")
    val longitude: Double? = null,
) : Parcelable


@Parcelize
data class GoogleMentionRequest(
    @field:SerializedName("rating")
    var rating: Double? = null,

    @field:SerializedName("name")
    var name: String? = null,

    @field:SerializedName("venue_address")
    var venueAddress: String? = null,

    @field:SerializedName("avatar")
    var avatar: String? = null
) : Parcelable