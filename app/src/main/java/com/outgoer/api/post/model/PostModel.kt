package com.outgoer.api.post.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.profile.model.BlockUserResponse
import com.outgoer.api.reels.model.ReelUserInfo
import com.outgoer.api.reels.model.ReelsHashTagsItem
import com.outgoer.api.reels.model.ReelsTagsItem
import com.outgoer.api.reels.model.UidRequest
import com.outgoer.api.sponty.model.SpontyCommentResponse
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.api.story.model.MusicResponse
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.api.venue.model.VenueDetail
import kotlinx.parcelize.Parcelize

//-----------------------Request-----------------------
@Parcelize
data class CreatePostRequest(
    @field:SerializedName("uid")
    var uid: ArrayList<UidRequest>? = null,

    @field:SerializedName("post_image")
    var postImage: ArrayList<ImageList>? = arrayListOf(),

    @field:SerializedName("tag_people")
    var tagPeople: String? = null,

    @field:SerializedName("description_tag")
    var descriptionTag: String? = null,

    @field:SerializedName("tag_venue")
    var tagVenue: String? = null,

    @field:SerializedName("latitude")
    var latitude: Double? = null,

    @field:SerializedName("caption")
    var caption: String? = null,

    @field:SerializedName("type")
    var type: Int? = null,

    @field:SerializedName("longitude")
    var longitude: Double? = null,

    @field:SerializedName("post_location")
    var postLocation: String? = null,

    @field:SerializedName("place_id")
    var placeId: String? = null,

    @field:SerializedName("hash_tags")
    var hashTags: String? = null
): Parcelable
@Parcelize
data class ImageList(
    @field:SerializedName("image")
    val image: String? = null,

    @field:SerializedName("seq_no")
    val seq_no: Int? = null,
): Parcelable


data class PostUserAllLikesRequest(
    @field:SerializedName("search")
    val search: String? = null,

    @field:SerializedName("post_id")
    val postId: Int,
)

data class AddLikesRequest(
    @field:SerializedName("post_id")
    val postId: Int,
)

data class RemoveLikesRequest(
    @field:SerializedName("post_id")
    val postId: Int,
)


data class AddCommentRequest(
    @field:SerializedName("comment")
    val comment: String,

    @field:SerializedName("post_id")
    val postId: Int,

    @field:SerializedName("mention_ids")
    val mentionIds: String? = null,
)

data class PostUserAllCommentRequest(
    @field:SerializedName("post_id")
    val postId: Int,
)

data class AddCommentReplyRequest(
    @field:SerializedName("comment")
    val replyMessage: String,

    @field:SerializedName("post_id")
    val postId: Int,

    @field:SerializedName("comment_id")
    val commentId: Int,

    @field:SerializedName("mention_ids")
    val mentionIds: String? = null,
)

data class UpdateCommentRequest(
    @field:SerializedName("comment")
    val comment: String,

    @field:SerializedName("mention_ids")
    val mentionIds: String? = null,
)

data class AddLikeToCommentRequest(
    @field:SerializedName("comment_id")
    val commentId: Int,
)

data class RemoveLikeFromCommentRequest(
    @field:SerializedName("comment_id")
    val commentId: Int,
)

//-----------------------Response-----------------------
@Keep
@Parcelize
data class PostInfo(
    @field:SerializedName("object_type")
    var objectType: String? = null,

    @field:SerializedName("sponties")
    var sponties: ArrayList<SpontyResponse>? = null,

    @field:SerializedName("venues")
    var venues: ArrayList<VenueDetail>? = null,

    @field:SerializedName("total_comments")
    var totalComments: Int? = null,

    @field:SerializedName("images")
    val images: List<PostImage>? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("follow_status")
    val followStatus: Boolean? = null,

    @field:SerializedName("caption")
    val caption: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("post_like")
    var postLike: Boolean,

    @field:SerializedName("type")
    val type: Int? = null,

    @field:SerializedName("total_likes")
    var totalLikes: Int? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("human_readable_time")
    val humanReadableTime: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("bookmark_status")
    var bookmarkStatus: Boolean,

    @field:SerializedName("post_tags")
    val postTags: List<PostTagsItem>? = null,

    @field:SerializedName("description_tags")
    val descriptionTags: List<PostTagsItem>? = null,

    @field:SerializedName("posts_hash_tags")
    val postsHashTags: ArrayList<ReelsHashTagsItem>? = arrayListOf(),

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user")
    val user: PostUserInfo? = null,

    @field:SerializedName("venue_tags")
    val venueTags: PostUserInfo? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("is_publish")
    val isPublish: Int? = null,

    @field:SerializedName("share_count")
    var shareCount: Int? = null,

    @field:SerializedName("save_count")
    var saveCount: Int? = null,

    @field:SerializedName("post_location")
    val postLocation: String? = null,

    @field:SerializedName("place_id")
    val place_id: String? = null,
) : Parcelable

@Keep
@Parcelize
data class PostUserInfo(
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

    @field:SerializedName("about")
    val about: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("at_venue_count")
    val atVenueCount: String? = null,

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
data class EventMediaResponse(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("event_id")
    val eventId: String? = null,

    @field:SerializedName("image")
    val image: String? = null,

    @field:SerializedName("uid")
    val uid: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,
): Parcelable

@Keep
@Parcelize
data class PostImage(
    @field:SerializedName("image")
    val image: String? = null,

    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("video_url")
    val videoUrl: String? = null,

    @field:SerializedName("gifthumbnail_url")
    val gifthumbnailUrl: String? = null,

    @field:SerializedName("post_id")
    val postId: Int? = null,

    @field:SerializedName("music_id")
    val musicId: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @Expose(serialize = false, deserialize = false)
    val isMute: Boolean = false,
    @Expose(serialize = false, deserialize = false)
    var isResumed :Boolean? = false,

    @field:SerializedName("music")
    val music: MusicResponse? = null,

    var width: Int = 0,
    var height: Int = 0
) : Parcelable

@Keep
@Parcelize
data class PostLikesUser(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("post_id")
    val postId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("follow_status")
    var followStatus: Boolean,

    @field:SerializedName("user")
    val user: PostUserInfo,

    @field:SerializedName("isfollow")
    val isFollow: PostIsFollow,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,
) : Parcelable

data class CommentInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("post_id")
    val postId: Int,

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

    @field:SerializedName("comment_like")
    var commentLike: Boolean = false,

    @field:SerializedName("human_readable_time")
    val humanReadableTime: String? = null,

    @field:SerializedName("user")
    val commentUserInfo: CommentUserInfo? = null,

    @field:SerializedName("replies")
    var replies: List<CommentInfo>? = null,

    @field:SerializedName("tags")
    var tags: List<CommentInfo>? = null,
)

data class CommentUserInfo(
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

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,
)

data class DeleteComment(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("post_id")
    val postId: Int?= null,
)

sealed class HomePagePostInfoState {
    data class UserProfileClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class VenueTaggedProfileClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class TaggedPeopleClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class OpenTaggedPeopleClick(val postInfo: PostUserInfo) : HomePagePostInfoState()
    data class MoreClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class AddPostLikeClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class RemovePostLikeClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class PostLikeCountClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class CommentClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class ShareClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class AddBookmarkClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class RemoveBookmarkClick(val postInfo: PostInfo) : HomePagePostInfoState()
    data class HashtagItemClicks(val postInfo: ReelsHashTagsItem) : HomePagePostInfoState()
    data class PhotoViewClick(val postImageUrl: String) : HomePagePostInfoState()
    data class VideoViewClick(val postVideoUrl: String, val postVideoThumbnailUrl: String?) : HomePagePostInfoState()
    data class ChangesVideoPosition(val postVideoUrl: Int) : HomePagePostInfoState()
}



sealed class HomePageStoryInfoState {
    data class StoryResponseData(val storyListResponse: StoryListResponse) : HomePageStoryInfoState()
    data class AddStoryResponseInfo(val storyInfo: String) : HomePageStoryInfoState()
    data class UserProfileClick(val storyListResponse: StoryListResponse) : HomePageStoryInfoState()

}



data class VideoViewClick(val postVideoUrl: String, val postVideoThumbnailUrl: String?)
data class PeopleForTagRequest(
    @field:SerializedName("search")
    var search: String? = null
)

data class PeopleForTag(

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("user_type")
    val userType: String? = null,

    @field:SerializedName("is_live")
    val isLive: Int? = null,

    @field:SerializedName("live_id")
    val liveId: String? = null,

    @field:SerializedName("about")
    val about: String? = null,

    @field:SerializedName("venue_address")
    val venueAddress: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,

    @field:SerializedName("total_followers")
    val totalFollowers: Int? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("total_following")
    val totalFollowing: Int? = null,

    @Expose(serialize = false, deserialize = false)
    var isSelected: Boolean = false,
)

@Keep
@Parcelize
data class PostIsFollow(
    @field:SerializedName("follow_by")
    val followBy: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("follow_for")
    val followFor: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("status")
    val status: Int? = null
) : Parcelable

@Keep
@Parcelize
data class PostTagsItem(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("post_id")
    val postId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("follow_status")
    var followStatus: Boolean,

    @field:SerializedName("user")
    val user: PostUserInfo? = null
) : Parcelable

data class AddPostLikeUserResponse(
    @field:SerializedName("post_id")
    val postId: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("follow_status")
    val followStatus: Boolean? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("isfollow")
    val isFollow: PostIsFollow? = null
)

data class AddBookmarkRequest(
    @field:SerializedName("post_id")
    val postId: Int,
)

data class RemoveBookmarkRequest(
    @field:SerializedName("post_id")
    val postId: Int,
)

data class AddPostBookmarkUserResponse(
    @field:SerializedName("post_id")
    val postId: Int? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("id")
    val id: Int? = null
)

sealed class BlockAccountPageState {
    data class UserProfileClick(val blockUserResponse: BlockUserResponse) : BlockAccountPageState()
    data class UnblockAccountClick(val blockUserResponse: BlockUserResponse) : BlockAccountPageState()
}


sealed class PostLikesUserPageState {
    data class UserProfileClick(val postLikesUser: PostLikesUser) : PostLikesUserPageState()
    data class FollowUserClick(val postLikesUser: PostLikesUser) : PostLikesUserPageState()
}

sealed class PostCommentActionState {
    data class Like(val commentInfo: CommentInfo) : PostCommentActionState()
    data class DisLike(val commentInfo: CommentInfo) : PostCommentActionState()
    data class ReplyComment(val commentInfo: CommentInfo) : PostCommentActionState()
    data class ClickComment(val commentInfo: CommentInfo, val replyComment: Boolean) : PostCommentActionState()
    data class UserImageClick(val commentInfo: CommentInfo) : PostCommentActionState()
    data class TaggedUser(val clickedText: String, val commentInfo: CommentInfo) :
        PostCommentActionState()
}


sealed class MoreActionsForTextActionState {
    data class UserProfileOpen(val chatMessageInfo: ChatMessageInfo) : MoreActionsForTextActionState()
    data class ReplyMessage(val chatMessageInfo: ChatMessageInfo) : MoreActionsForTextActionState()
    data class ForwardMessage(val chatMessageInfo: ChatMessageInfo) : MoreActionsForTextActionState()
    data class CopyMessage(val chatMessageInfo: ChatMessageInfo) : MoreActionsForTextActionState()
    data class DeleteMessage(val chatMessageInfo: ChatMessageInfo) : MoreActionsForTextActionState()
    data class ReactionOnMessage(val chatMessageInfo: ChatMessageInfo, val reactionType: String) : MoreActionsForTextActionState()

    data class ReactedUsersView(val messageId: Int): MoreActionsForTextActionState()
    data class TaggedUser(val clickedText: String, val commentInfo: ChatMessageInfo) : MoreActionsForTextActionState()
}



sealed class SpontyActionState {
    data class LikeDisLike(val commentInfo: SpontyResponse) : SpontyActionState()
    data class CommentClick(val commentInfo: SpontyResponse) : SpontyActionState()
    data class VideoViewClick(val postVideoUrl: String, val postVideoThumbnailUrl: String?) : SpontyActionState()
    data class ImageClick(val imageUrl: String) : SpontyActionState()
    data class UserImageClick(val commentInfo: SpontyResponse) : SpontyActionState()
    data class CheckAction(val commentInfo: SpontyResponse) : SpontyActionState()
    data class DeleteSponty(val commentInfo: SpontyResponse) : SpontyActionState()
    data class ReportSponty(val commentInfo: SpontyResponse) : SpontyActionState()
    data class VenueClick(val commentInfo: SpontyResponse) : SpontyActionState()
    data class LocationSpontyClick(val commentInfo: SpontyResponse) : SpontyActionState()
    data class JoinUnJoinClick(val spontyResponse: SpontyResponse) : SpontyActionState()
    data class TaggedUser(val clickedText: String, val commentInfo: SpontyResponse) :
        SpontyActionState()

    data class TaggedCommentUser(val clickedText: String, val commentInfo: SpontyCommentResponse) :
        SpontyActionState()
}


sealed class SpontyCommentActionState {
    data class Like(val commentInfo: SpontyCommentResponse) : SpontyCommentActionState()
    data class DisLike(val commentInfo: SpontyCommentResponse) : SpontyCommentActionState()
    data class ReplyComment(val commentInfo: SpontyCommentResponse) : SpontyCommentActionState()
    data class ClickComment(val commentInfo: SpontyCommentResponse, val replyComment: Boolean) : SpontyCommentActionState()
    data class UserImageClick(val commentInfo: SpontyCommentResponse) : SpontyCommentActionState()
    data class TaggedUser(val clickedText: String, val commentInfo: SpontyCommentResponse) :
        SpontyCommentActionState()
}


data class MyPostRequest(
    @field:SerializedName("user_id")
    val userId: Int?,
)

data class MyBookmarkRequest(
    @field:SerializedName("user_id")
    val userId: Int?,

    @field:SerializedName("type")
    val type: String?,
)

data class PostTaggedPeopleRequest(
    @field:SerializedName("post_id")
    var postId: Int
)

sealed class PostTaggedPeopleState {
    data class UserProfileClick(val postTagsItem: PostTagsItem) : PostTaggedPeopleState()
    data class Follow(val postTagsItem: PostTagsItem) : PostTaggedPeopleState()
    data class Unfollow(val postTagsItem: PostTagsItem) : PostTaggedPeopleState()
}

data class MyTagRequest(
    @field:SerializedName("user_id")
    val userId: Int?,
)

@Keep
@Parcelize
data class MyTagBookmarkInfo(
    @field:SerializedName("object_type")
    val objectType: String? = null,

    @field:SerializedName("tag_id")
    val tagId: Int? = null,

    @field:SerializedName("book_id")
    val bookId: Int? = null,

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("caption")
    val caption: String? = null,

    @field:SerializedName("latitude")
    val latitude: String? = null,

    @field:SerializedName("longitude")
    val longitude: String? = null,

    @field:SerializedName("total_likes")
    val totalLikes: Int? = null,

    @field:SerializedName("total_comments")
    val totalComments: Int? = null,

    @field:SerializedName("follow_status")
    val followStatus: Boolean? = null,

    @field:SerializedName("human_readable_time")
    val humanReadableTime: String? = null,

    @field:SerializedName("bookmark_status")
    val bookmarkStatus: Boolean,

    @field:SerializedName("user")
    val user: ReelUserInfo? = null,

    //------------------Post------------------
    @field:SerializedName("post_like")
    val postLike: Boolean? = null,

    @field:SerializedName("post_tags")
    val postTags: List<PostTagsItem>? = null,

    @field:SerializedName("images")
    val images: List<PostImage>? = null,

    @field:SerializedName("post_likes")
    val postLikes: PostLikesUser? = null,

    @field:SerializedName("type")
    val type: Int? = null,

    @field:SerializedName("post_location")
    val postLocation: String? = null,

    //------------------Reel------------------
    @field:SerializedName("reels_tags")
    val reelsTags: List<ReelsTagsItem>? = null,

    @field:SerializedName("reels_like")
    val reelsLike: Boolean? = null,

    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,

    @field:SerializedName("gifthumbnail_url")
    val gifthumbnailUrl: String? = null,

    @field:SerializedName("video_url")
    val videoUrl: String? = null,

    @field:SerializedName("reel_location")
    val reelLocation: String? = null,

    var isSavePost :Boolean? = false
) : Parcelable

enum class MediaObjectType(val type: String) {
    Reel("reel"),
    POST("post")
}

sealed class PostMoreOption {
    object DeleteClick : PostMoreOption()
    object ReportClick : PostMoreOption()
    object DismissClick : PostMoreOption()
    object BlockClick : PostMoreOption()
}

sealed class DismissBottomSheet {
    object DismissClick : DismissBottomSheet()
}

object VerificationSuccess

data class ReportReason(
    @field:SerializedName("id")
    var id: Int ? = null,

    @field:SerializedName("title")
    var title: String ? = null,

    @field:SerializedName("created_at")
    var created_at: String ? = null,

    @field:SerializedName("updated_at")
    var updated_at: String ? = null,
)

data class ReportPostRequest(
    @field:SerializedName("post_id")
    var postId: Int,

    @field:SerializedName("report_id")
    var reportId: Int
)

