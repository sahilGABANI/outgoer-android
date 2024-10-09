package com.outgoer.api.group.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

data class ManageGroupRequest(
    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("user_ids")
    val userIds: String? = null
)


data class GroupMemberRequest(
    @field:SerializedName("search")
    val search: String? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int,

)

data class CreateGroupRequest(
    @field:SerializedName("group_name")
    val groupName: String? = null,

    @field:SerializedName("group_description")
    val groupDescription: String? = null,

    @field:SerializedName("group_pic")
    val groupPic: String? = null,

    @field:SerializedName("user_ids")
    val userIds: String? = null
)

data class UpdateGroupRequest(
    @field:SerializedName("group_name")
    val groupName: String? = null,

    @field:SerializedName("group_description")
    val groupDescription: String? = null,

    @field:SerializedName("group_pic")
    val groupPic: String? = null
)

data class GroupInfoResponse(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("sender_idsender_id")
    val senderId: Int? = null,

    @field:SerializedName("receiver_id")
    val receiverId: Int? = null,

    @field:SerializedName("file_path")
    val filePath: String? = null,

    @field:SerializedName("group_name")
    val groupName: String? = null,

    @field:SerializedName("group_description")
    val groupDescription: String? = null,

    @field:SerializedName("last_message")
    val lastMessage: String? = null,

    @field:SerializedName("message_type")
    val messageType: String? = null,

    @field:SerializedName("file_type")
    val fileType: String? = null,

    @field:SerializedName("last_datetime")
    val lastDatetime: String? = null,

    @field:SerializedName("status")
    val status: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("last_msg_userid")
    val lastMsgUserid: Int? = null,

    @field:SerializedName("users")
    val users: ArrayList<GroupUserInfo>? = null
)

@Parcelize
data class GroupUserInfo(
    @field:SerializedName("id")
    val id: Int? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("status")
    val status: Int,

    @field:SerializedName("role")
    var role: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("profile_url")
    val profileUrl: String? = null,

    val isSelected: Boolean = false
): Parcelable