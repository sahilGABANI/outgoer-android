package com.outgoer.api.reels

import com.outgoer.api.post.model.ReportPostRequest
import com.outgoer.api.reels.model.*
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class ReelsRepository(
    private val reelsRetrofitAPI: ReelsRetrofitAPI,
) {

    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()
    var listOfReelsInfo: MutableList<ReelInfo> = mutableListOf()

    fun getAllReels(pageNo: Int, tabType: Int): Single<OutgoerResponse<List<ReelInfo>>?> {
        return reelsRetrofitAPI.getAllReels(pageNo, tabType)
            .doAfterSuccess {
                if(pageNo == 1) {
                    listOfReelsInfo.clear()
                }

                it?.data?.let { reelList -> listOfReelsInfo.addAll(reelList) }
            }
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun createReel(request: CreateReelRequest): Single<OutgoerCommonResponse> {
        return reelsRetrofitAPI.createReel(request)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun deleteReel(reelId: Int): Single<OutgoerCommonResponse> {
        return reelsRetrofitAPI.deleteReel(reelId)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun getReelAllLikes(pageNo: Int, request: ReelAllLikeRequest): Single<OutgoerResponse<List<ReelAllLike>>?> {
        return reelsRetrofitAPI.getReelAllLikes(pageNo, request).flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun addLikeToReel(request: AddReelLikeRequest): Single<AddReelLikeResponse> {
        return reelsRetrofitAPI.addLikeToReel(request).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun removeLikeFromReel(request: RemoveReelLikeRequest): Single<OutgoerCommonResponse> {
        return reelsRetrofitAPI.removeLikeFromReel(request).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun getAllReelComments(pageNo: Int, request: GetAllReelCommentsRequest): Single<List<ReelCommentInfo>> {
        return reelsRetrofitAPI.getAllReelComments(pageNo, request).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun addComment(request: AddReelCommentRequest): Single<ReelCommentInfo> {
        return reelsRetrofitAPI.addComment(request).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun addCommentReply(request: AddReelCommentReplyRequest): Single<ReelCommentInfo> {
        return reelsRetrofitAPI.addCommentReply(request).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun updateCommentOrReply(request: UpdateReelCommentRequest, commentOrReplyId: Int): Single<ReelCommentInfo> {
        return reelsRetrofitAPI.updateCommentOrReply(request, commentOrReplyId)
            .flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun addLikeToComment(request: AddLikeToReelCommentRequest): Single<ReelCommentInfo> {
        return reelsRetrofitAPI.addLikeToComment(request).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun removeLikeFromComment(request: RemoveLikeFromReelCommentRequest): Single<OutgoerCommonResponse> {
        return reelsRetrofitAPI.removeLikeFromComment(request).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun deleteCommentOrReply(commentOrReplyId: Int): Single<OutgoerCommonResponse> {
        return reelsRetrofitAPI.deleteCommentOrReply(commentOrReplyId).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun addReelToBookmark(request: AddBookmarkToReelRequest): Single<AddBookmarkToReelUserResponse> {
        return reelsRetrofitAPI.addReelToBookmark(request).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun removeReelToBookmark(request: RemoveBookmarkFromReelRequest): Single<OutgoerCommonResponse> {
        return reelsRetrofitAPI.removeReelToBookmark(request).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun getMyReel(pageNo: Int, userId: Int?): Single<OutgoerResponse<List<ReelInfo>>?> {
        val request = MyReelRequest(userId)
        return reelsRetrofitAPI.getMyReel(pageNo, request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getMyBookmarkReel(pageNo: Int, userId: Int?): Single<OutgoerResponse<List<ReelInfo>>?> {
        val request = MyBookmarkReelRequest(userId)
        return reelsRetrofitAPI.getMyBookmarkReel(pageNo, request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getReelTaggedPeople(reelId: Int): Single<OutgoerResponse<List<ReelsTagsItem>>?> {
        return reelsRetrofitAPI.getReelTaggedPeople(ReelTaggedPeopleRequest(reelId))
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getReelById(reelId: Int): Single<OutgoerResponse<ReelInfo>?> {
        return reelsRetrofitAPI.getReelById(reelId)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getReelsByHashTag(pageNo: Int, tagId:Int): Single<OutgoerResponse<List<ReelInfo>>?> {
        val request = GetReelsByHashTagRequest(tagId)
        return reelsRetrofitAPI.getReelsByHashTag(pageNo,request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun reportReel(request: ReportReelRequest): Single<OutgoerCommonResponse> {
        return reelsRetrofitAPI.reportReel(request)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }
}