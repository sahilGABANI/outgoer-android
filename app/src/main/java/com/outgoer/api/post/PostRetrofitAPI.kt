package com.outgoer.api.post

import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.chat.model.SharePostReelsRequest
import com.outgoer.api.post.model.*
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.*

interface PostRetrofitAPI {

    @POST("conversation/share-reel-post")
    fun getShareReelsPostToChat(@Body sharePostReelsRequest: SharePostReelsRequest): Single<OutgoerResponse<ArrayList<ChatMessageInfo>>>

    @GET("post")
    fun getAllPost(@Query("page") pageNo: Int): Single<OutgoerResponse<List<PostInfo>>?>

    @POST("post/all-like")
    fun getPostUserAllLikes(@Query("page") pageNo: Int, @Body postUserAllLikesRequest: PostUserAllLikesRequest): Single<OutgoerResponse<List<PostLikesUser>>?>

    @POST("post/add-remove-like")
    fun addLikesToPost(@Body addLikesRequest: AddLikesRequest): Single<OutgoerResponse<AddPostLikeUserResponse>>

    @POST("post/add-remove-like")
    fun removeLikeFromPost(@Body removeLikesRequest: RemoveLikesRequest): Single<OutgoerCommonResponse>

    @POST("post/all-comments")
    fun getListOfPostComments(
        @Query("page") pageNo: Int,
        @Body postUserAllCommentRequest: PostUserAllCommentRequest
    ): Single<OutgoerResponse<List<CommentInfo>>>

    @POST("post/add-comment")
    fun addComment(@Body addCommentRequest: AddCommentRequest): Single<OutgoerResponse<CommentInfo>>

    @POST("post/add-comment-replay")
    fun addCommentReply(@Body addCommentReplyRequest: AddCommentReplyRequest): Single<OutgoerResponse<CommentInfo>>

    @POST("post/update-comment/{id}")
    fun updateCommentOrReply(
        @Body updateCommentRequest: UpdateCommentRequest,
        @Path("id") commentOrReplyId: Int,
    ): Single<OutgoerResponse<CommentInfo>>

    @POST("post/comment/add-remove-like")
    fun addLikeToComment(@Body addLikeToCommentRequest: AddLikeToCommentRequest): Single<OutgoerResponse<CommentInfo>>

    @POST("post/comment/add-remove-like")
    fun removeLikeFromComment(@Body removeLikeFromCommentRequest: RemoveLikeFromCommentRequest): Single<OutgoerCommonResponse>

    @DELETE("post/remove-comment/{id}")
    fun deleteCommentOrReply(@Path("id") commentOrReplyId: Int): Single<OutgoerCommonResponse>

    @POST("post/tag-people")
    fun getPeopleForTag(@Query("page") pageNo: Int, @Body request: PeopleForTagRequest): Single<OutgoerResponse<List<PeopleForTag>>?>

    @POST("post/create")
    fun createPost(@Body request: CreatePostRequest): Single<OutgoerCommonResponse>

    @POST("post/add-remove-bookmark")
    fun addPostToBookmark(@Body request: AddBookmarkRequest): Single<OutgoerResponse<AddPostBookmarkUserResponse>>

    @POST("post/add-remove-bookmark")
    fun removePostToBookmark(@Body request: RemoveBookmarkRequest): Single<OutgoerCommonResponse>

    @POST("users/my-post")
    fun getMyPost(@Query("page") pageNo: Int, @Body request: MyPostRequest): Single<OutgoerResponse<List<PostInfo>>?>

    @POST("users/my-bookmark")
    fun getMyBookmark(@Query("page") pageNo: Int, @Body request: MyBookmarkRequest): Single<OutgoerResponse<List<MyTagBookmarkInfo>>?>

    @DELETE("post/delete/{id}")
    fun deletePost(@Path("id") postId: Int): Single<OutgoerCommonResponse>

    @POST("post/get-post-tagged-people")
    fun getPostTaggedPeople(@Body request: PostTaggedPeopleRequest): Single<OutgoerResponse<List<PostTagsItem>>?>

    @POST("users/my-tag")
    fun getMyTag(@Query("page") pageNo: Int, @Body request: MyTagRequest): Single<OutgoerResponse<List<MyTagBookmarkInfo>>?>

    @GET("post/show/{id}")
    fun getPostById(@Path("id") postId: Int): Single<OutgoerResponse<PostInfo>?>

    @GET("post/get-report")
    fun getReportReason(): Single<OutgoerResponse<List<ReportReason>>?>

    @POST("post/report")
    fun reportPost(@Body request: ReportPostRequest): Single<OutgoerCommonResponse>
}