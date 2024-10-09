package com.outgoer.api.group

import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.group.model.CreateGroupRequest
import com.outgoer.api.group.model.GroupInfoResponse
import com.outgoer.api.group.model.GroupMemberRequest
import com.outgoer.api.group.model.GroupUserInfo
import com.outgoer.api.group.model.ManageGroupRequest
import com.outgoer.api.group.model.UpdateGroupRequest
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.*

interface GroupRetrofitAPI {

    @POST("conversation/create_group")
    fun createGroup(
        @Body createGroupRequest: CreateGroupRequest
    ): Single<OutgoerResponse<ChatConversationInfo>>

    @POST("conversation/get_group_members")
    fun getGroupMemberInfo(
        @Query("page") page: Int,
        @Body groupMemberRequest: GroupMemberRequest
    ): Single<OutgoerResponse<ArrayList<GroupUserInfo>>>


    @POST("conversation/update_group/{group_id}")
    fun updateGroup(
        @Path("group_id") groupId: Int,
        @Body updateGroupRequest: UpdateGroupRequest
    ): Single<OutgoerResponse<GroupInfoResponse>>

    @GET("conversation/show_group/{group_id}")
    fun getGroupInfo(
        @Path("group_id") groupId: Int
    ): Single<OutgoerResponse<ChatConversationInfo>>

    @DELETE("conversation/delete_group/{group_id}}")
    fun deleteGroupInfo(
        @Path("group_id") groupId: Int
    ): Single<OutgoerCommonResponse>

    @POST("conversation/set_group_admin")
    fun setGroupAdmin(
        @Body manageGroupRequest: ManageGroupRequest
    ): Single<OutgoerCommonResponse>

    @POST("conversation/remove_group_user")
    fun removeGroupUser(
        @Body manageGroupRequest: ManageGroupRequest
    ): Single<OutgoerCommonResponse>

    @POST("conversation/remove_group_admin")
    fun removeGroupAdminUser(
        @Body manageGroupRequest: ManageGroupRequest
    ): Single<OutgoerCommonResponse>

    @POST("conversation/add_group_user")
    fun addGroupUser(
        @Body manageGroupRequest: ManageGroupRequest
    ): Single<OutgoerCommonResponse>
}

