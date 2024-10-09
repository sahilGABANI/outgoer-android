package com.outgoer.api.sponty

import com.outgoer.api.post.model.UpdateCommentRequest
import com.outgoer.api.sponty.model.*
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject

class SpontyRepository(
    private val spontyRetrofitAPI: SpontyRetrofitAPI
) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    private val spontySubject: BehaviorSubject<String> = BehaviorSubject.create()
    val sponty: Observable<String> = spontySubject.hide()

    fun getAllSponty(pageNo: Int): Single<OutgoerResponse<List<SpontyResponse>>> {
        return spontyRetrofitAPI.getAllSponty(pageNo).flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }


    fun getNearbyAllSponty(): Single<OutgoerResponse<List<SpontyResponse>>> {
        return spontyRetrofitAPI.getAllNearBySponty().flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun createSponty(createSpontyRequest: CreateSpontyRequest): Single<OutgoerResponse<SpontyResponse>> {
        return spontyRetrofitAPI.createSponty(createSpontyRequest).flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun spontyReport(reportSpontyRequest: ReportSpontyRequest): Single<OutgoerCommonResponse> {
        return spontyRetrofitAPI.spontyReport(reportSpontyRequest).flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }


    fun getSpecificSpontyInfo(spontyId: Int): Single<OutgoerResponse<SpontyResponse>> {
        return spontyRetrofitAPI.getSpecificSpontyInfo(spontyId).flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getAllJoinSponty(allJoinSpontyRequest: AllJoinSpontyRequest): Single<OutgoerResponse<List<SpontyJoinResponse>>> {
        return spontyRetrofitAPI.getAllJoinSponty(allJoinSpontyRequest).flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun addRemoveSponty(allJoinSpontyRequest: AllJoinSpontyRequest): Single<OutgoerResponse<SpontyJoins>> {
        return spontyRetrofitAPI.addRemoveSponty(allJoinSpontyRequest).flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getAllLikes(spontyActionRequest: SpontyActionRequest): Single<OutgoerResponse<ArrayList<SpontyActionResponse>>> {
        return spontyRetrofitAPI.getAllLikes(spontyActionRequest).flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun addRemoveSpontyLike(spontyActionRequest: SpontyActionRequest): Single<OutgoerResponse<SpontyActionResponse>> {
        return spontyRetrofitAPI.addRemoveLikeSponty(spontyActionRequest).flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getAllComments(spontyActionRequest: SpontyActionRequest): Single<OutgoerResponse<ArrayList<SpontyCommentResponse>>> {
        return spontyRetrofitAPI.getAllComments(spontyActionRequest).flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun addSpontyComment(addSpontyCommentRequest: AddSpontyCommentRequest): Single<OutgoerResponse<SpontyCommentResponse>> {
        return spontyRetrofitAPI.addComment(addSpontyCommentRequest).flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun addSpontyReplyComments(addSpontyCommentRequest: AddSpontyCommentReplyRequest): Single<OutgoerResponse<SpontyCommentResponse>> {
        return spontyRetrofitAPI.addSpontyReplyComments(addSpontyCommentRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }
    fun addSpontyUpdateComments(addSpontyCommentRequest: UpdateCommentRequest, commentOrReplyId :Int): Single<OutgoerResponse<SpontyCommentResponse>> {
        return spontyRetrofitAPI.addSpontyUpdateComments(addSpontyCommentRequest,commentOrReplyId)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun addSpontyCommentsLike(addSpontyCommentRequest: SpontyCommentActionRequest): Single<OutgoerResponse<SpontyCommentResponse>> {
        return spontyRetrofitAPI.addSpontyCommentsLike(addSpontyCommentRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun removeComment(commentId: Int): Single<OutgoerCommonResponse> {
        return spontyRetrofitAPI.removeComment(commentId).flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun removeSponty(commentId: Int): Single<OutgoerCommonResponse> {
        return spontyRetrofitAPI.removeSponty(commentId).flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

}