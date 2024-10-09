package com.outgoer.api.sponty

import com.outgoer.api.post.model.UpdateCommentRequest
import com.outgoer.api.sponty.model.*
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SpontyRetrofitAPI {

    @GET("sponty")
    fun getAllSponty(
        @Query("page") pageNo: Int
    ): Single<OutgoerResponse<List<SpontyResponse>>>


    @GET("sponty/nearby")
    fun getAllNearBySponty(): Single<OutgoerResponse<List<SpontyResponse>>>

    @POST("sponty/create")
    fun createSponty(
        @Body createSpontyRequest: CreateSpontyRequest
    ): Single<OutgoerResponse<SpontyResponse>>

    @GET("sponty/show/{sponty_id}")
    fun getSpecificSpontyInfo(
        @Path("sponty_id") spontyId: Int,
    ): Single<OutgoerResponse<SpontyResponse>>

    @POST("sponty/all-joins")
    fun getAllJoinSponty(
        @Body allJoinSpontyRequest: AllJoinSpontyRequest
    ): Single<OutgoerResponse<List<SpontyJoinResponse>>>

    @POST("sponty/add-remove-join")
    fun addRemoveSponty(
        @Body allJoinSpontyRequest: AllJoinSpontyRequest
    ): Single<OutgoerResponse<SpontyJoins>>

    @POST("sponty/add-remove-like")
    fun addRemoveLikeSponty(
        @Body spontyActionRequest: SpontyActionRequest
    ): Single<OutgoerResponse<SpontyActionResponse>>

    @POST("sponty/all-like")
    fun getAllLikes(
        @Body spontyActionRequest: SpontyActionRequest
    ): Single<OutgoerResponse<ArrayList<SpontyActionResponse>>>

    @POST("sponty/all-comments")
    fun getAllComments(
        @Body spontyActionRequest: SpontyActionRequest
    ): Single<OutgoerResponse<ArrayList<SpontyCommentResponse>>>

    @POST("sponty/add-comment")
    fun addComment(
        @Body addSpontyCommentRequest: AddSpontyCommentRequest
    ): Single<OutgoerResponse<SpontyCommentResponse>>

    @POST("sponty/add-comment-replay")
    fun addSpontyReplyComments(
        @Body addSpontyCommentRequest: AddSpontyCommentReplyRequest
    ): Single<OutgoerResponse<SpontyCommentResponse>>

    @POST("sponty/update-comment/{id}")
    fun addSpontyUpdateComments(
        @Body addSpontyCommentRequest: UpdateCommentRequest,
        @Path("id") commentOrReplyId: Int,
    ): Single<OutgoerResponse<SpontyCommentResponse>>

    @POST("sponty/comment/add-remove-like")
    fun addSpontyCommentsLike(
        @Body addSpontyCommentRequest: SpontyCommentActionRequest
    ): Single<OutgoerResponse<SpontyCommentResponse>>


    @POST("report/sponty")
    fun spontyReport(
        @Body reportSpontyRequest: ReportSpontyRequest
    ): Single<OutgoerCommonResponse>


    @DELETE("sponty/remove-comment/{commentId}")
    fun removeComment(
        @Path("commentId") commentId: Int
    ): Single<OutgoerCommonResponse>

    @DELETE("sponty/remove/{spontyId}")
    fun removeSponty(
        @Path("spontyId") commentId: Int
    ): Single<OutgoerCommonResponse>
}