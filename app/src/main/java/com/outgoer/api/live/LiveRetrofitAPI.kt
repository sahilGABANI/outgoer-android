package com.outgoer.api.live

import com.outgoer.api.live.model.*
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface LiveRetrofitAPI {
    @POST("live/create")
    fun createLiveEvent(@Body request: CreateLiveEventRequest): Single<OutgoerResponse<LiveEventInfo>>

    @POST("live/join")
    fun joinLiveEvent(@Body request: JoinLiveEventRequest): Single<OutgoerResponse<LiveEventInfo>>

    @POST("live/end")
    fun endLiveEvent(@Body request: EndLiveEventRequest): Single<OutgoerCommonResponse>

    @GET("live/active")
    fun getAllActiveLiveEvent(): Single<OutgoerResponse<AllActiveEventInfo>>

    @POST("live/verify_event")
    fun verifyEvent(@Body request: LiveEventVerifyRequest): Single<OutgoerCommonResponse>

    @POST("live/publisher")
    fun inviteOrRejectCoHosts(@Body request: CoHostRequest): Single<OutgoerCommonResponse>

    @POST("live/join_users")
    fun liveJoinUser(@Body request: LiveEventWatchingUserRequest): Single<OutgoerResponse<List<LiveJoinResponse>>>
}