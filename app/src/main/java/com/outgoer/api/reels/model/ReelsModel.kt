package com.outgoer.api.reels.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.outgoer.api.post.model.PostUserInfo
import com.outgoer.api.story.model.MusicResponse
import kotlinx.parcelize.Parcelize
@Parcelize
data class CreateReelRequest(
    @field:SerializedName("uid")
    var uid: String? = null,

    @field:SerializedName("tag_people")
    var tagPeople: String? = null,

    @field:SerializedName("description_tag")
    var descriptionTag: String? = null,

    @field:SerializedName("latitude")
    var latitude: Double? = null,

    @field:SerializedName("caption")
    var caption: String? = null,

    @field:SerializedName("longitude")
    var longitude: Double? = null,

    @field:SerializedName("reel_location")
    var reelLocation: String? = null,

    @field:SerializedName("hash_tags")
    var hashTags: String? = null,

    @field:SerializedName("place_id")
    var placeId: String? = null,

    @field:SerializedName("duration")
    var duration: Int? = null,

    @field:SerializedName("tag_venue")
    var tagVenue: String? = null,

    @field:SerializedName("music_id")
    var musicId: Int? = null,
) :Parcelable


@Keep
@Parcelize
data class UidRequest(

    @field:SerializedName("music_id")
    val musicId: Int? = null,

    @field:SerializedName("video_id")
    val videoId: String? = null,


    @field:SerializedName("seq_no")
    val seq_no: Int? = null,
) :Parcelable

@Keep
@Parcelize
data class ReelInfo(
    @field:SerializedName("share_count")
    var shareCount: Int? = null,

    @field:SerializedName("save_count")
    var saveCount: Int? = null,

    @field:SerializedName("total_comments")
    var totalComments: Int? = null,

    @field:SerializedName("reels_tags")
    val reelsTags: List<ReelsTagsItem>? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("follow_status")
    var followStatus: Boolean,

    @field:SerializedName("caption")
    val caption: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("reels_like")
    var reelsLike: Boolean,

    @field:SerializedName("width")
    var width: Int? = 0,

    @field:SerializedName("height")
    var height: Int? = 0,

    @field:SerializedName("total_likes")
    var totalLikes: Int? = null,

    @field:SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @field:SerializedName("gifthumbnail_url")
    val gifthumbnailUrl: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("human_readable_time")
    val humanReadableTime: String? = null,

    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("video_url")
    val videoUrl: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("bookmark_status")
    var bookmarkStatus: Boolean,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user")
    val user: ReelUserInfo? = null,

    @field:SerializedName("venue_tags")
    val venueTags: PostUserInfo? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("reel_location")
    val reelLocation: String? = null,

    @field:SerializedName("reels_hash_tags")
    val reelHashTags: List<ReelsHashTagsItem>? = null,

    @field:SerializedName("watch_count")
    val watchCount: Int? = null,

    @Expose(serialize = false, deserialize = false)
    var isMute: Boolean = false,

    @field:SerializedName("music")
    val music: MusicResponse? = null,
) : Parcelable

data class ReelAllLikeRequest(
    @field:SerializedName("reels_id")
    val reelId: Int,
)

@Keep
data class ReelAllLike(
    @field:SerializedName("reel_id")
    val reelId: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("user")
    val user: ReelUserInfo? = null
)

@Keep
@Parcelize
data class ReelUserInfo(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("about")
    val about: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("total_followers")
    val totalFollowers: Int? = null,

    @field:SerializedName("at_venue_count")
    val atVenueCount: Int? = null,

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

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

) : Parcelable

@Keep
@Parcelize
data class ReelsTagsItem(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("reel_id")
    val reelId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("follow_status")
    var followStatus: Boolean,

    @field:SerializedName("user")
    val user: ReelUserInfo? = null
) : Parcelable

data class AddReelLikeRequest(
    @field:SerializedName("reels_id")
    val reelId: Int,
)

data class RemoveReelLikeRequest(
    @field:SerializedName("reels_id")
    val reelId: Int,
)

@Keep
data class AddReelLikeResponse(
    @field:SerializedName("reel_id")
    val reelId: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null
)

data class GetAllReelCommentsRequest(
    @field:SerializedName("reels_id")
    val reelId: Int,
)

@Keep
data class ReelCommentInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("reel_id")
    val reelId: Int,

    @field:SerializedName("parent_id")
    val parentId: Int? = null,

    @field:SerializedName("comment")
    var comment: String? = null,

    @field:SerializedName("total_likes")
    var totalLikes: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("reels_comment_like")
    var reelsCommentLike: Boolean,

    @field:SerializedName("human_readable_time")
    val humanReadableTime: String? = null,

    @field:SerializedName("user")
    val commentUserInfo: ReelUserInfo? = null,

    @field:SerializedName("replies")
    var replies: List<ReelCommentInfo>? = null,

    @field:SerializedName("tags")
    var tags: List<ReelCommentInfo>? = null,
)

data class AddReelCommentRequest(
    @field:SerializedName("comment")
    val comment: String,

    @field:SerializedName("reels_id")
    val reelId: Int,

    @field:SerializedName("mention_ids")
    val mentionIds: String? = null,
)

data class AddReelCommentReplyRequest(
    @field:SerializedName("comment")
    val replyMessage: String,

    @field:SerializedName("reels_id")
    val reelsId: Int,

    @field:SerializedName("comment_id")
    val commentId: Int,

    @field:SerializedName("mention_ids")
    val mentionIds: String? = null,
)

data class UpdateReelCommentRequest(
    @field:SerializedName("comment")
    val comment: String,

    @field:SerializedName("mention_ids")
    val mentionIds: String? = null,
)

data class AddLikeToReelCommentRequest(
    @field:SerializedName("comment_id")
    val commentId: Int,
)

data class RemoveLikeFromReelCommentRequest(
    @field:SerializedName("comment_id")
    val commentId: Int,
)

data class AddBookmarkToReelRequest(
    @field:SerializedName("reels_id")
    val reelsId: Int,
)

data class RemoveBookmarkFromReelRequest(
    @field:SerializedName("reels_id")
    val reelsId: Int,
)

@Keep
data class AddBookmarkToReelUserResponse(
    @field:SerializedName("reels_id")
    val reelsId: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null
)

data class MyReelRequest(
    @field:SerializedName("user_id")
    val userId: Int?,
)

data class MyBookmarkReelRequest(
    @field:SerializedName("user_id")
    val userId: Int?,
)

@Keep
@Parcelize
data class ReelsHashTagsItem(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("reel_id")
    val reelId: Int? = null,

    @field:SerializedName("tag_id")
    val tagId: Int? = null,

    @field:SerializedName("title")
    val title: String? = null
) : Parcelable

data class GetReelsByHashTagRequest(
    @field:SerializedName("tag_id")
    val tagId: Int?,
)


sealed class ReelsPageState {
    data class UserProfileClick(val reelInfo: ReelInfo) : ReelsPageState()
    data class TaggedPeopleClick(val reelInfo: ReelInfo) : ReelsPageState()
    data class FollowClick(val reelInfo: ReelInfo) : ReelsPageState()
    data class UnfollowClick(val reelInfo: ReelInfo) : ReelsPageState()
    data class AddReelLikeClick(val reelInfo: ReelInfo) : ReelsPageState()
    data class RemoveReelLikeClick(val reelInfo: ReelInfo) : ReelsPageState()
    data class CommentClick(val reelInfo: ReelInfo) : ReelsPageState()
    data class AddBookmarkClick(val reelInfo: ReelInfo) : ReelsPageState()
    data class RemoveBookmarkClick(val reelInfo: ReelInfo) : ReelsPageState()
    data class ShareClick(val reelInfo: ReelInfo) : ReelsPageState()
    data class MoreClick(val reelInfo: ReelInfo,val showReport:Boolean) : ReelsPageState()
    data class MuteUnmuteClick(val isMute: Boolean) : ReelsPageState()
    data class VenueTaggedProfileClick(val reelInfo: ReelInfo) : ReelsPageState()
}

sealed class ReelsCommentPageState {
    data class Like(val reelCommentInfo: ReelCommentInfo) : ReelsCommentPageState()
    data class DisLike(val reelCommentInfo: ReelCommentInfo) : ReelsCommentPageState()
    data class ReplyComment(val reelCommentInfo: ReelCommentInfo) : ReelsCommentPageState()
    data class ClickComment(val reelCommentInfo: ReelCommentInfo) : ReelsCommentPageState()
    data class UserImageClick(val reelCommentInfo: ReelCommentInfo) : ReelsCommentPageState()
    data class TaggedUser(val clickedText: String, val reelCommentInfo: ReelCommentInfo) : ReelsCommentPageState()
}

data class ReelTaggedPeopleRequest(
    @field:SerializedName("reel_id")
    var reelId: Int
)

sealed class ReelTaggedPeopleState {
    data class UserProfileClick(val reelsTagsItem: ReelsTagsItem) : ReelTaggedPeopleState()
    data class Follow(val reelsTagsItem: ReelsTagsItem) : ReelTaggedPeopleState()
    data class Unfollow(val reelsTagsItem: ReelsTagsItem) : ReelTaggedPeopleState()
}

data class ReportReelRequest(
    @field:SerializedName("reel_id")
    var postId: Int,

    @field:SerializedName("report_id")
    var reportId: Int
)