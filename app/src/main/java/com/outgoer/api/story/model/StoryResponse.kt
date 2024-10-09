package com.outgoer.api.story.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.outgoer.api.authentication.model.MentionResponse
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.event.model.GooglePlaces
import com.outgoer.application.Outgoer
import kotlinx.android.parcel.Parcelize
import com.outgoer.api.event.model.GoogleMentionRequest

data class ViewStoryRequest(
    @field:SerializedName("story_id") var storyId: Int? = null,
)

data class StoryRequest(
    @field:SerializedName("image") var image: String? = null,

    @field:SerializedName("uid") var uid: String? = null,

    @field:SerializedName("venue_mention") var venue_mention: String? = null,

    @field:SerializedName("type") var type: Int,

    @field:SerializedName("music_id") var musicId: Int? = null,

    @field:SerializedName("google_mention") var googleMention: ArrayList<GoogleMentionRequest>? = null
)


//@Parcelize
//data class GoogleMentionRequest(
//    @field:SerializedName("rating")
//    var rating: Double?= null,
//
//    @field:SerializedName("name")
//    var name: String? = null,
//
//    @field:SerializedName("venue_address")
//    var venueAddress: String? = null,
//
//    @field:SerializedName("avatar")
//    var avatar: String? = null
//) : Parcelable


@Parcelize
data class StoryListResponse(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("badge_request")
    val badgeRequest: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("last_thumbnail")
    val lastThumbnail: String? = null,

    @field:SerializedName("at_venue_count")
    val atVenueCount: Int,

    @field:SerializedName("stories")
    val stories: ArrayList<StoriesResponse> = arrayListOf(),

    var isSelected: Boolean = false
) : Parcelable


@Parcelize
data class StoriesResponse(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("user_id")
    var userId: Int,

    @field:SerializedName("caption")
    val caption: String? = null,

    @field:SerializedName("image")
    val image: String? = null,

    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("type")
    val type: Int? = null,

    @field:SerializedName("music_id")
    val musicId: Int? = null,

    @field:SerializedName("duration")
    val duration: String? = null,

    @field:SerializedName("total_views")
    val totalViews: Int? = null,

    @field:SerializedName("google_mention")
    val googleMention: ArrayList<GooglePlaces>? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("conversation_id")
    var conversationId: Int? = null,

    @field:SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @field:SerializedName("video_url")
    val videoUrl: String? = null,

    @field:SerializedName("human_readable_time")
    val humanReadableTime: String? = null,

    @field:SerializedName("mentions")
    val mentions: ArrayList<StoryMentionUser>? = arrayListOf(),

    @field:SerializedName("music")
    val music: MusicResponse? = null,
) : Parcelable

@Parcelize
data class StoryMentionUser(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("story_id")
    var storyId: Int,

    @field:SerializedName("user_id")
    var userId: Int,

    @field:SerializedName("venue_id")
    var venueId: Int,

    @field:SerializedName("user")
    var user: MentionUser,
) : Parcelable

@Parcelize
data class MentionUser(
    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("address")
    val address: String? = null,

    @field:SerializedName("venue_address")
    val venueAddress: String? = null,

    @field:SerializedName("rating")
    val rating: Double? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("badge_request")
    val badgeRequest: Int? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("story_id")
    val storyId: Int? = null

) : Parcelable

@Parcelize
data class MusicResponse(
    @field:SerializedName("id")
    var id: Int,

    @field:SerializedName("song_title")
    val songTitle: String? = null,

    @field:SerializedName("song_subtitle")
    val songSubtitle: String? = null,

    @field:SerializedName("song_file")
    val songFile: String? = null,

    @field:SerializedName("song_image")
    val songImage: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("category_id")
    val categoryId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("artists")
    val artists: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    ) : Parcelable