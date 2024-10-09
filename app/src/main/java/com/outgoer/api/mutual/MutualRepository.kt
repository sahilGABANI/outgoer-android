package com.outgoer.api.mutual

import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import io.reactivex.Single

class MutualRepository(private val mutualRetrofitAPI: MutualRetrofitAPI) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun acceptRejectFollowRequest(acceptRejectRequest: AcceptRejectRequest): Single<OutgoerCommonResponse> {
        return mutualRetrofitAPI.acceptRejectFollowRequest(acceptRejectRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun getAllMutualList(pageNo: Int, request: GetFollowersAndFollowingRequest): Single<List<FollowUser>> {
        return mutualRetrofitAPI.getAllMutualList(pageNo, request).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }
}