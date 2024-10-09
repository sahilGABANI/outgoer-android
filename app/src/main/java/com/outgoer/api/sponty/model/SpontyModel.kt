package com.outgoer.api.sponty.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.post.model.PostUserInfo
import kotlinx.android.parcel.Parcelize

@Parcelize
data class NotificationSponty(
    @field:SerializedName("object_type")
    var objectType: String? = null,

    @field:SerializedName("sponty_id")
    val spontyId: Int
): Parcelable

data class SpontyActionRequest(
    @field:SerializedName("sponty_id")
    val spontyId: Int
)


data class ReportSpontyRequest(
    @field:SerializedName("sponty_id")
    val spontyId: Int,

    @field:SerializedName("report_id")
    val reportId: Int
)

data class SpontyCommentActionRequest(
    @field:SerializedName("comment_id")
    val commentId: Int
)

data class SpontyActionResponse(
    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("sponty_id")
    val spontyId: Int,

    @field:SerializedName("updated_at")
    val updatedAt: String?,

    @field:SerializedName("created_at")
    val createdAt: String?,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("follow_status")
    val followStatus: Boolean,

    @field:SerializedName("isfollow")
    val isfollow: String? = null
)

data class AddSpontyCommentRequest(
    @field:SerializedName("sponty_id")
    val spontyId: Int,

    @field:SerializedName("comment")
    val comment: String? = null,

    @field:SerializedName("mention_ids")
    var mentionIds: String? = null,
)

data class AddSpontyCommentReplyRequest(
    @field:SerializedName("sponty_id")
    val spontyId: Int,

    @field:SerializedName("comment_id")
    val commentId: Int,
    @field:SerializedName("comment")
    val comment: String? = null,

    @field:SerializedName("mention_ids")
    var mentionIds: String? = null,
)


data class SpontyCommentResponse(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("sponty_id")
    val spontyId: Int,

    @field:SerializedName("comment")
    var comment: String? = null,

    @field:SerializedName("total_likes")
    var totalLikes: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("human_readable_time")
    val humanReadableTime: String? = null,

    @field:SerializedName("user")
    val user: OutgoerUser? = null,

    @field:SerializedName("tags")
    val tags: ArrayList<TaggedUser>? = null,

    @field:SerializedName("comment_like")
    var commentLike: Boolean = false,

    @field:SerializedName("parent_id")
    val parentId: Int? = null,

    @field:SerializedName("replies")
    var replies: List<SpontyCommentResponse>? = null,
)

data class TaggedUser(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("comment_id")
    val commentId: Int,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user")
    val user: OutgoerUser? = null,
)


data class AllJoinSpontyRequest(
    @field:SerializedName("sponty_id")
    val spontyId: Int,

    @field:SerializedName("search")
    val search: String? = null
)

data class CreateSpontyRequest(
    @field:SerializedName("caption")
    val caption: String,

    @field:SerializedName("location")
    val location: String?,

    @field:SerializedName("date_time")
    var dateTime: String?= null,

    @field:SerializedName("place_id")
    var placeId: String,

    @field:SerializedName("latitude")
    val latitude: Double? = null,

    @field:SerializedName("longitude")
    val longitude: Double? = null,

    @field:SerializedName("tag_people")
    var tagPeople: String? = null,
    @field:SerializedName("description_tag")
    var descriptionTag: String? = null,


    @field:SerializedName("venue_id")
    var venueId: String? = null,

    @field:SerializedName("uid")
    var uid: String? = null,

    @field:SerializedName("sponty_image")
    val spontyImage: ArrayList<String> = arrayListOf(),
)

@Parcelize
data class SpontyResponse(

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("caption")
    val caption: String? = null,

    @field:SerializedName("location")
    val location: String? = null,

    @field:SerializedName("date_time")
    val dateTime: String? = null,

    @field:SerializedName("place_id")
    val placeId: String? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("total_comments")
    var totalComments: Int = 0,

    @field:SerializedName("total_likes")
    var totalLikes: Int? = null,

    @field:SerializedName("venue_id")
    var venueId: Int? = null,

    @field:SerializedName("is_notify")
    var isNotify: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("distance")
    val distance: Double = 0.0,

    @field:SerializedName("sponty_join")
    var spontyJoin: Boolean = false,

    @field:SerializedName("sponty_like")
    var spontyLike: Boolean = false,

    @field:SerializedName("human_readable_time")
    val humanReadableTime: String? = null,

    @field:SerializedName("user")
    val user: OutgoerUser? = null,

    @field:SerializedName("sponty_tags")
    val spontyTags: ArrayList<SpontyJoinResponse>? = arrayListOf(),

    @field:SerializedName("description_tags")
    val descriptionTags: ArrayList<SpontyJoinResponse>? = arrayListOf(),

    @field:SerializedName("join_users")
    val joinUsers: ArrayList<SpontyJoins>? = null,

    @field:SerializedName("sponty_joins")
    val spontyJoins: SpontyJoins? = null,

    @field:SerializedName("venue_tags")
    val venueTags: PostUserInfo? = null,

    @field:SerializedName("images")
    val images: List<ImagesItem>? = null,

    @field:SerializedName("video")
    val video: Video? = null

): Parcelable
@Parcelize
data class Video(

    @field:SerializedName("image")
    val image: String? = null,

    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("video_url")
    val videoUrl: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("sponty_id")
    val spontyId: Int? = null,

    @field:SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null
): Parcelable

@Parcelize
data class ImagesItem(

    @field:SerializedName("image")
    val image: String? = null,

    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("video_url")
    val videoUrl: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("sponty_id")
    val spontyId: Int? = null,

    @field:SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null
): Parcelable


@Parcelize
data class SpontyJoins(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("sponty_id")
    val spontyId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("user_name")
    val userName: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,
): Parcelable

@Parcelize
data class SpontyJoinResponse(

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("sponty_id")
    val spontyId: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user")
    val user: OutgoerUser? = null
):Parcelable