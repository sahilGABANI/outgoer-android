package com.outgoer.api.reels

import com.outgoer.api.post.model.ReportPostRequest
import com.outgoer.api.reels.model.*
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.*

interface ReelsRetrofitAPI {
    @GET("reels")
    fun getAllReels(@Query("page") pageNo: Int, @Query("tab_type") tabType: Int): Single<OutgoerResponse<List<ReelInfo>>?>

    @POST("reels/create")
    fun createReel(@Body request: CreateReelRequest): Single<OutgoerCommonResponse>

    @DELETE("reels/delete/{id}")
    fun deleteReel(@Path("id") reelId: Int): Single<OutgoerCommonResponse>

    @POST("reels/all-like")
    fun getReelAllLikes(@Query("page") pageNo: Int, @Body request: ReelAllLikeRequest): Single<OutgoerResponse<List<ReelAllLike>>?>

    @POST("reels/add-remove-like")
    fun addLikeToReel(@Body request: AddReelLikeRequest): Single<OutgoerResponse<AddReelLikeResponse>>

    @POST("reels/add-remove-like")
    fun removeLikeFromReel(@Body request: RemoveReelLikeRequest): Single<OutgoerCommonResponse>

    @POST("reels/all-comments")
    fun getAllReelComments(
        @Query("page") pageNo: Int,
        @Body request: GetAllReelCommentsRequest,
    ): Single<OutgoerResponse<List<ReelCommentInfo>>>

    @POST("reels/add-comment")
    fun addComment(@Body request: AddReelCommentRequest): Single<OutgoerResponse<ReelCommentInfo>>

    @POST("reels/add-comment-replay")
    fun addCommentReply(@Body request: AddReelCommentReplyRequest): Single<OutgoerResponse<ReelCommentInfo>>

    @POST("reels/update-comment/{id}")
    fun updateCommentOrReply(
        @Body request: UpdateReelCommentRequest,
        @Path("id") commentOrReplyId: Int,
    ): Single<OutgoerResponse<ReelCommentInfo>>

    @POST("reels/comment/add-remove-like")
    fun addLikeToComment(@Body request: AddLikeToReelCommentRequest): Single<OutgoerResponse<ReelCommentInfo>>

    @POST("reels/comment/add-remove-like")
    fun removeLikeFromComment(@Body request: RemoveLikeFromReelCommentRequest): Single<OutgoerCommonResponse>

    @DELETE("reels/remove-comment/{id}")
    fun deleteCommentOrReply(@Path("id") commentOrReplyId: Int): Single<OutgoerCommonResponse>

    @POST("reels/add-remove-bookmark")
    fun addReelToBookmark(@Body request: AddBookmarkToReelRequest): Single<OutgoerResponse<AddBookmarkToReelUserResponse>>

    @POST("reels/add-remove-bookmark")
    fun removeReelToBookmark(@Body request: RemoveBookmarkFromReelRequest): Single<OutgoerCommonResponse>

    @POST("users/my-reel")
    fun getMyReel(@Query("page") pageNo: Int, @Body request: MyReelRequest): Single<OutgoerResponse<List<ReelInfo>>?>

    @POST("users/my-bookmark-reel")
    fun getMyBookmarkReel(@Query("page") pageNo: Int, @Body request: MyBookmarkReelRequest): Single<OutgoerResponse<List<ReelInfo>>?>

    @POST("reels/get-reel-tagged-people")
    fun getReelTaggedPeople(@Body request: ReelTaggedPeopleRequest): Single<OutgoerResponse<List<ReelsTagsItem>>?>

    @GET("reels/show/{id}")
    fun getReelById(@Path("id") reelId: Int): Single<OutgoerResponse<ReelInfo>?>

    @POST("reels/bytag")
    fun getReelsByHashTag(@Query("page") pagNo:Int, @Body request: GetReelsByHashTagRequest) : Single<OutgoerResponse<List<ReelInfo>>?>

    @POST("reels/report")
    fun reportReel(@Body request: ReportReelRequest): Single<OutgoerCommonResponse>
}