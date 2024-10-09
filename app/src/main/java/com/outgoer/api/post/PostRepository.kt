package com.outgoer.api.post

import com.outgoer.api.chat.model.ChatMessageInfo
import com.outgoer.api.chat.model.SharePostReelsRequest
import com.outgoer.api.post.model.*
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject

class PostRepository(
    private val postRetrofitAPI: PostRetrofitAPI,
) {

    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()
    private var postId: Int? = 0

    private val commentCountStateSubject: PublishSubject<CommentInfo> = PublishSubject.create()
    val commentCountState: Observable<CommentInfo> = commentCountStateSubject.hide()

    private val deleteCommentStateSubject: PublishSubject<DeleteComment> = PublishSubject.create()
    val deleteCommentState: Observable<DeleteComment> = deleteCommentStateSubject.hide()
    fun getShareReelsPostToChat(sharePostReelsRequest: SharePostReelsRequest): Single<OutgoerResponse<ArrayList<ChatMessageInfo>>> {
        return postRetrofitAPI.getShareReelsPostToChat(sharePostReelsRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getAllPost(pageNo: Int): Single<OutgoerResponse<List<PostInfo>>?> {
        return postRetrofitAPI.getAllPost(pageNo)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getPostUserAllLikes(pageNo: Int, postUserAllLikesRequest: PostUserAllLikesRequest): Single<OutgoerResponse<List<PostLikesUser>>?> {
        return postRetrofitAPI.getPostUserAllLikes(pageNo, postUserAllLikesRequest).flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun addLikesToPost(addLikesRequest: AddLikesRequest): Single<AddPostLikeUserResponse> {
        return postRetrofitAPI.addLikesToPost(addLikesRequest).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun removeLikeFromPost(removeLikesRequest: RemoveLikesRequest): Single<OutgoerCommonResponse> {
        return postRetrofitAPI.removeLikeFromPost(removeLikesRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun getListOfPostComments(pageNo: Int, postUserAllCommentRequest: PostUserAllCommentRequest): Single<List<CommentInfo>> {
        return postRetrofitAPI.getListOfPostComments(pageNo, postUserAllCommentRequest).flatMap {
            postId = postUserAllCommentRequest.postId
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun addComment(addCommentRequest: AddCommentRequest): Single<CommentInfo> {
        return postRetrofitAPI.addComment(addCommentRequest).flatMap {
            setCommentData(it.data)
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    private fun setCommentData(data: CommentInfo?) {
        data?.let {
            commentCountStateSubject.onNext(it)
        }
    }

    fun addCommentReply(addCommentReplyRequest: AddCommentReplyRequest): Single<CommentInfo> {
        return postRetrofitAPI.addCommentReply(addCommentReplyRequest).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun updateCommentOrReply(
        updateCommentRequest: UpdateCommentRequest,
        commentOrReplyId: Int,
    ): Single<CommentInfo> {
        return postRetrofitAPI.updateCommentOrReply(updateCommentRequest, commentOrReplyId)
            .flatMap {
                outgoerResponseConverter.convertToSingle(it)
            }
    }

    fun addLikeToComment(addLikeToCommentRequest: AddLikeToCommentRequest): Single<CommentInfo> {
        return postRetrofitAPI.addLikeToComment(addLikeToCommentRequest).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun removeLikeFromComment(removeLikeFromCommentRequest: RemoveLikeFromCommentRequest): Single<OutgoerCommonResponse> {
        return postRetrofitAPI.removeLikeFromComment(removeLikeFromCommentRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun deleteCommentOrReply(commentOrReplyId: Int, replyComment: Boolean): Single<OutgoerCommonResponse> {
        return postRetrofitAPI.deleteCommentOrReply(commentOrReplyId).flatMap {
            deleteComment(replyComment,commentOrReplyId)
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    private fun deleteComment(replyComment: Boolean, deleteCommentId: Int) {
        if (!replyComment) {
            deleteCommentStateSubject.onNext(DeleteComment(deleteCommentId,postId))
        } else {
        }
    }

    fun getPeopleForTag(pageNo: Int, request: PeopleForTagRequest): Single<OutgoerResponse<List<PeopleForTag>>?> {
        return postRetrofitAPI.getPeopleForTag(pageNo, request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun createPost(request: CreatePostRequest): Single<OutgoerCommonResponse> {
        return postRetrofitAPI.createPost(request)
            .doAfterSuccess {}.flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun addPostToBookmark(request: AddBookmarkRequest): Single<AddPostBookmarkUserResponse> {
        return postRetrofitAPI.addPostToBookmark(request).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun removePostToBookmark(request: RemoveBookmarkRequest): Single<OutgoerCommonResponse> {
        return postRetrofitAPI.removePostToBookmark(request).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun getMyPost(pageNo: Int, userId: Int?): Single<OutgoerResponse<List<PostInfo>>?> {
        val request = MyPostRequest(userId = userId)
        return postRetrofitAPI.getMyPost(pageNo, request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getMyBookmark(pageNo: Int, userId: Int?,type :String): Single<OutgoerResponse<List<MyTagBookmarkInfo>>?> {
        val request = MyBookmarkRequest(userId = userId,type =type)
        return postRetrofitAPI.getMyBookmark(pageNo, request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun deletePost(postId: Int): Single<OutgoerCommonResponse> {
        return postRetrofitAPI.deletePost(postId).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun getPostTaggedPeople(postId: Int): Single<OutgoerResponse<List<PostTagsItem>>?> {
        return postRetrofitAPI.getPostTaggedPeople(PostTaggedPeopleRequest(postId))
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getMyTag(pageNo: Int, userId: Int?): Single<OutgoerResponse<List<MyTagBookmarkInfo>>?> {
        val request = MyTagRequest(userId = userId)
        return postRetrofitAPI.getMyTag(pageNo, request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getPostById(postId: Int): Single<OutgoerResponse<PostInfo>?> {
        return postRetrofitAPI.getPostById(postId)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getReportReason(): Single<OutgoerResponse<List<ReportReason>>?> {
        return postRetrofitAPI.getReportReason()
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun reportPost(request: ReportPostRequest): Single<OutgoerCommonResponse> {
        return postRetrofitAPI.reportPost(request)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }


}