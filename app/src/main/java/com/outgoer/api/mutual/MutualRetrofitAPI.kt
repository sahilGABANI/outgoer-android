package com.outgoer.api.mutual

import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface MutualRetrofitAPI {

    @POST("follow")
    fun acceptRejectFollowRequest(
        @Body request: AcceptRejectRequest
    ): Single<OutgoerCommonResponse>

    @POST("get_mutual_friend")
    fun getAllMutualList(
        @Query("page") pageNo: Int,
        @Body request: GetFollowersAndFollowingRequest
    ): Single<OutgoerResponse<List<FollowUser>>>
}