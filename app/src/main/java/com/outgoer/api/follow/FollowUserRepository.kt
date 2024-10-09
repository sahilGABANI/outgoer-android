package com.outgoer.api.follow

import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import io.reactivex.Single

class FollowUserRepository(
    private val followUserRetrofitAPI: FollowUserRetrofitAPI,
) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun acceptRejectFollowRequest(acceptRejectRequest: AcceptRejectRequest): Single<OutgoerCommonResponse> {
        return followUserRetrofitAPI.acceptRejectFollowRequest(acceptRejectRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun getAllFollowersList(pageNo: Int, request: GetFollowersAndFollowingRequest): Single<List<FollowUser>> {
        return followUserRetrofitAPI.getAllFollowersList(pageNo, request).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun getAllFollowingList(pageNo: Int, request: GetFollowersAndFollowingRequest): Single<List<FollowUser>> {
        return followUserRetrofitAPI.getAllFollowingList(pageNo, request).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }
}