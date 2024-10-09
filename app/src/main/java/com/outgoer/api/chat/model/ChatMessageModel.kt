package com.outgoer.api.chat.model

import android.os.Parcelable
import androidx.annotation.Keep
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.group.model.GroupUserInfo
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.story.model.StoriesResponse
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

data class ConversationRequest(
    @field:SerializedName("receiver_id")
    val receiverId: Int,
)

data class GetMessageListRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int,
)

data class GetConversationListRequest(
    @field:SerializedName("search")
    val search: String? = null,

    @field:SerializedName("page")
    val page: Int? = null,

    @field:SerializedName("per_page")
    val perPage: Int? = null,
)

@Parcelize
@Keep
data class ChatConversationInfo(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("sender_id")
    val senderId: Int,

    @field:SerializedName("file_path")
    var filePath: String? = null,

    @field:SerializedName("group_name")
    val groupName: String? = null,

    @field:SerializedName("group_description")
    val groupDescription: String? = null,

    @field:SerializedName("chat_type")
    var chatType: String? = null,

    @field:SerializedName("receiver_id")
    val receiverId: Int,

    @field:SerializedName("name")
    var name: String? = null,

    @field:SerializedName("username")
    var username: String? = null,

    @field:SerializedName("email")
    var email: String? = null,

    @field:SerializedName("last_message")
    var lastMessage: String? = null,

    @field:SerializedName("message_type")
    var fileType: MessageType? = null,

    @field:SerializedName("unread_count")
    var unreadCount: Int = 0,

    @field:SerializedName("conversation_id")
    var conversationId: Int = 0,

    @field:SerializedName("created_at")
    var createdAt: String? = null,

    @field:SerializedName("conversation_updated_at")
    var conversationUpdatedAt: String? = null,

    @field:SerializedName("profile_url")
    var profileUrl: String? = null,

    @field:SerializedName("last_msg_userid")
    var lastMsgUserid: Int = 0,

    @field:SerializedName("last_msg_name")
    var lastMsgName: String? = null,

    @field:SerializedName("users")
    val users: ArrayList<GroupUserInfo>? = null,

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

    @field:SerializedName("profile_verified")
    val profileVerified: Int? = null,
) : Parcelable

enum class MessageType {
    @SerializedName("chat_started")
    ChatStarted,

    @SerializedName("text")
    Text,

    @SerializedName("video")
    Video,

    @SerializedName("image")
    Image,

    @SerializedName("gif")
    GIF,

    @SerializedName("audio")
    Audio,

    @SerializedName("post")
    Post,

    @SerializedName("reel")
    Reel,

    @SerializedName("reply")
    Reply,

    @SerializedName("forward")
    Forward,

    @SerializedName("reaction_emoji")
    REACTION_EMOJI,

    Typing
}

@Parcelize
data class ChatMessageInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("sender_id")
    val senderId: Int? = null,

    @field:SerializedName("receiver_id")
    val receiverId: Int? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("reply_name")
    val replyName: String? = null,

    @field:SerializedName("reply_message")
    val replyMessage: String? = null,

    @field:SerializedName("file")
    val file: String? = null,

    @field:SerializedName("thumbnail")
    val thumbnail: String? = null,

    @field:SerializedName("chat_type")
    val chatType: String? = null,

    @field:SerializedName("file_url")
    val fileUrl: String? = null,

    @field:SerializedName("video_url")
    val videoUrl: String? = null,

    @field:SerializedName("message_type")
    val fileType: MessageType? = null,

    @field:SerializedName("file_size")
    val fileSize: String? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("tag_id")
    val tagId: Int? = null,

    @field:SerializedName("story_id")
    val storyId: Int? = null,

    @field:SerializedName("story")
    val story: StoriesResponse? = null,


    @field:SerializedName("tag_name")
    val tagName: String? = null,

    @field:SerializedName("is_read")
    var isRead: Int? = null,

    @field:SerializedName("delete_status")
    val deleteStatus: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("file_local_url")
    val fileLocalUrl: String? = null,

    @field:SerializedName("uploadProgress")
    val uploadProgress: Long? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("human_readable_time")
    val humanReadableTime: String? = null,

    @field:SerializedName("profile_url")
    val profileUrl: String? = null,

    @field:SerializedName("duration")
    val duration: String? = null,

    @field:SerializedName("video_file_url")
    val videoFileUrl: String? = null,

    @field:SerializedName("reel_id")
    val reelId: Int? = null,

    @field:SerializedName("reel_obj")
    val reelObj: ReelInfo? = null,

    @field:SerializedName("post_id")
    val postId: Int? = null,

    @field:SerializedName("post_obj")
    val postObj: PostObjInfo? = null,

    @field:SerializedName("reaction_data")
    var reactionData: ReactionData? = null,


    @field:SerializedName("mentions")
    val mentions: ArrayList<GroupUserMentions>? = null,

    @Expose(serialize = false, deserialize = false)
    var showDate: Boolean = false,

    @Expose(serialize = false, deserialize = false)
    val recentMessageTime: String? = null,

    var isPlay: Boolean = false
) : Parcelable

@Parcelize
data class GroupUserMentions(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("message_id")
    val message_id: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("user")
    val user: OutgoerUser,

): Parcelable


@Parcelize
data class ChatMessageListener(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("reaction_data")
    var reactionData: ReactionData? = null,
): Parcelable

@Parcelize
data class ReactionData(
    @field:SerializedName("reaction_counts")
    val reactionCounts: ReactionCounts? = null,

    @field:SerializedName("is_reacted_by_user")
    val isReactedByUser: @RawValue Any? = null,

    @field:SerializedName("reaction_by_user")
    var reactionByUser: String? = null,

    @field:SerializedName("total_count")
    val totalCount: Int? = null
): Parcelable


@Parcelize
data class ReactionCounts(
    @field:SerializedName("like")
    var like: Int = 0,

    @field:SerializedName("love")
    var love: Int = 0,

    @field:SerializedName("laughing")
    var laughing: Int = 0,

    @field:SerializedName("expression")
    var expression: Int = 0,

    @field:SerializedName("sad")
    var sad: Int = 0,

    @field:SerializedName("pray")
    var pray: Int = 0,
): Parcelable


@Parcelize
data class PostObjInfo(
    @field:SerializedName("post_id")
    val postId: Int? = null,

    @field:SerializedName("images")
    val images: ArrayList<PostImageInfo>? = null,

    @field:SerializedName("user")
    val user: OutgoerUser? = null,
): Parcelable

@Parcelize
data class PostImageInfo(

    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("post_id")
    val postId: Int? = null,

    @field:SerializedName("image")
    val image: String? = null,

    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("music_id")
    val musicId: Int? = null,

    @field:SerializedName("seq_no")
    val seqNo: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("is_check")
    val isCheck: Int? = null,

    @field:SerializedName("video_url")
    val videoUrl: String? = null,

    @field:SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null

):Parcelable

data class ChatSendMessageRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int ?= null,

    @field:SerializedName("sender_id")
    val senderId: Int,

    @field:SerializedName("receiver_id")
    val receiverId: Int,

    @field:SerializedName("message_type")
    val fileType: MessageType? = null,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("file_url")
    var fileUrl: String? = null,

    @field:SerializedName("video_url")
    var videoUrl: String? = null,

    @field:SerializedName("file_size")
    val fileSize: String? = null,

    @field:SerializedName("thumbnail")
    val thumbnail: String? = null,

    @field:SerializedName("file")
    val file: String? = null,

    @field:SerializedName("profile_url")
    val profileUrl: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("chat_type")
    val chatType: String? = null,

    @field:SerializedName("group_name")
    val groupName: String? = null,

    @field:SerializedName("story_id")
    val storyId: Int? = null,

    @field:SerializedName("username")
    val username: String? = null,


    @field:SerializedName("reply_id")
    val replyId: Int? = null,

    @field:SerializedName("reply_name")
    val replyName: String? = null,

    @field:SerializedName("reply_message")
    val replyMessage: String? = null,

    @field:SerializedName("mentions_ids")
    var mentions_ids: String? = "",


    @field:SerializedName("duration")
    val duration: String? = ""
)

data class JoinRoomRequest(
    @field:SerializedName("sender_id")
    var senderId: Int,

    @field:SerializedName("receiver_id")
    val receiverId: Int,

    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("chat_type")
    val chatType: String
)


sealed class ChatConversationActionState {
    data class ConversationClick(val chatConversationInfo: ChatConversationInfo) :
        ChatConversationActionState()

    data class GroupConversationClick(val groupChatConversationInfo: GroupChatConversationInfo) :
        ChatConversationActionState()
}

data class UpdateOnlineStatusRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("receiver_id")
    val receiverId: Int,
)

data class UpdateOnlineStatusResponse(
    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("receiver_id")
    val receiverId: Int,

    @field:SerializedName("is_online")
    val isOnline: Boolean
)

data class ChatOnlineStatusResponse(

    @field:SerializedName("data")
    val data: List<ChatOnlineStatus>? = null
)

data class ChatOnlineStatus(

    @field:SerializedName("receiver_id")
    val receiverId: Int? = null,

    @field:SerializedName("is_online")
    val isOnline: Boolean? = null
)


data class SendMessageIsReadRequest(
    @field:SerializedName("message_ids")
    val messageIds: String,

    @field:SerializedName("sender_id")
    val senderId: Int,

    @field:SerializedName("receiver_id")
    val receiverId: Int,

    @field:SerializedName("conversation_id")
    val conversationId: Int,
)

data class SetUserOfflineRequest(
    @field:SerializedName("receiver_id")
    val senderId: Int
)

data class SetAppOnlineRequest(
    @field:SerializedName("receiver_id")
    val senderId: Int,
)

data class SetUserRoomRequest(
    @field:SerializedName("user_id")
    val userId: Int,
)

@Parcelize
data class GroupChatConversationInfo(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("sender_id")
    val senderId: Int,

    @field:SerializedName("receiver_id")
    val receiverId: Int,

    @field:SerializedName("file_path")
    val filePath: String? = null,

    @field:SerializedName("group_name")
    val groupName: String? = null,

    @field:SerializedName("group_description")
    val groupDescription: String? = null,

    @field:SerializedName("last_message")
    var lastMessage: String? = null,

    @field:SerializedName("message_type")
    var messageType: String? = null,

    @field:SerializedName("file_type")
    var fileType: String,

    @field:SerializedName("last_datetime")
    var lastDatetime: String? = null,

    @field:SerializedName("status")
    var status: Int = 0,

    @field:SerializedName("created_at")
    var createdAt: String? = null,

    @field:SerializedName("updated_at")
    var updatedAt: String? = null,

    @field:SerializedName("conversation_updated_at")
    var conversationUpdatedAt: String? = null,

    @field:SerializedName("last_msg_userid")
    var lastMsgUserid: Int = 0,

    @field:SerializedName("conversation_id")
    var conversationId: Int = 0,

    @field:SerializedName("name")
    var name: String? = null,

    @field:SerializedName("username")
    var username: String? = null,

    @field:SerializedName("profile_url")
    var profileUrl: String? = null,

    @field:SerializedName("unread_count")
    var unreadCount: Int = 0
) : Parcelable

@Parcelize
data class SharePostReelsRequest(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("type")
    var type: String? = null,

    @field:SerializedName("user_ids")
    var userIds: ArrayList<Int>? = null
) : Parcelable




data class AddReactionSocketEvent(
    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("sender_id")
    val senderId: Int? = null,

    @field:SerializedName("message_id")
    val messageId: Int? = null,

    @field:SerializedName("reaction_type")
    val reactionType: String? = null,

    @field:SerializedName("sender_name")
    val senderName: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("group_name")
    val groupName: String? = null,

    @field:SerializedName("receiver_id")
    val receiverId: Int? = null,

    @field:SerializedName("profile_url")
    val profileUrl: String? = null,

    @field:SerializedName("reel_obj")
    val reelObj: ReelInfo? = null,
)



data class RemoveReactionSocketEvent(
    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("sender_id")
    val senderId: Int? = null,

    @field:SerializedName("message_id")
    val messageId: Int? = null,

    @field:SerializedName("reaction_type")
    val reactionType: String? = null,

    @field:SerializedName("reel_obj")
    val reelObj: ReelInfo? = null,
)



data class MessageReactionSocketEvent(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("sender_id")
    val senderId: Int? = null,

    @field:SerializedName("message_id")
    val messageId: Int? = null,

    @field:SerializedName("reaction_type")
    val reactionType: Int? = null
)

data class MessageTypingSocketEvent(
    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("username")
    val username: String? = null
)

@Keep
@Parcelize
data class Reaction(
    @field:SerializedName("id") val id: Int,
    @field:SerializedName("conversation_id") val conversationId: Int? = null,
    @field:SerializedName("message_id") val messageId: Int? = null,
    @field:SerializedName("user_id") val userId: Int? = null,
    @field:SerializedName("reaction_type") val reactionType: String? = null,
    @field:SerializedName("reaction_emoji") val reactionEmoji: String? = null,
    @field:SerializedName("created_at") val createdAt: String? = null,
    @field:SerializedName("updated_at") val updatedAt: String? = null,
    @field:SerializedName("name") val name: String? = null,
    @field:SerializedName("username") val username: String? = null,
    @field:SerializedName("profile_verified") val profileVerified: Int? = null,
    @field:SerializedName("badge_request") val badgeRequest: Int? = null,
    @field:SerializedName("user_type") val userType: String? = null,
    @field:SerializedName("profile_url") val profileUrl: String? = null
) : Parcelable