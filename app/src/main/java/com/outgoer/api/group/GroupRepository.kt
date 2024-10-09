package com.outgoer.api.group

import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.group.model.CreateGroupRequest
import com.outgoer.api.group.model.GroupInfoResponse
import com.outgoer.api.group.model.GroupMemberRequest
import com.outgoer.api.group.model.GroupUserInfo
import com.outgoer.api.group.model.ManageGroupRequest
import com.outgoer.api.group.model.UpdateGroupRequest
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class GroupRepository(
    private val groupRetrofitAPI: GroupRetrofitAPI,
) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun createGroup(createGroupRequest: CreateGroupRequest): Single<OutgoerResponse<ChatConversationInfo>> {
        return groupRetrofitAPI.createGroup(createGroupRequest).flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun updateGroup(
        groupId: Int,
        updateGroupRequest: UpdateGroupRequest
    ): Single<OutgoerResponse<GroupInfoResponse>> {
        return groupRetrofitAPI.updateGroup(groupId, updateGroupRequest).flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun getGroupMemberInfo(page: Int, groupMemberRequest: GroupMemberRequest): Single<OutgoerResponse<ArrayList<GroupUserInfo>>> {
        return groupRetrofitAPI.getGroupMemberInfo(page, groupMemberRequest).flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun getGroupInfo(groupId: Int): Single<OutgoerResponse<ChatConversationInfo>> {
        return groupRetrofitAPI.getGroupInfo(groupId).flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun deleteGroupInfo(groupId: Int): Single<OutgoerCommonResponse> {
        return groupRetrofitAPI.deleteGroupInfo(groupId).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun setGroupAdmin(manageGroupRequest: ManageGroupRequest): Single<OutgoerCommonResponse> {
        return groupRetrofitAPI.setGroupAdmin(manageGroupRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun removeGroupUser(manageGroupRequest: ManageGroupRequest): Single<OutgoerCommonResponse> {
        return groupRetrofitAPI.removeGroupUser(manageGroupRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun removeGroupAdminUser(manageGroupRequest: ManageGroupRequest): Single<OutgoerCommonResponse> {
        return groupRetrofitAPI.removeGroupAdminUser(manageGroupRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun addGroupUser(manageGroupRequest: ManageGroupRequest): Single<OutgoerCommonResponse> {
        return groupRetrofitAPI.addGroupUser(manageGroupRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

}