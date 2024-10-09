package com.outgoer.api.live

import com.outgoer.api.live.model.*
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import com.outgoer.socket.SocketDataManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

class LiveRepository(
    private val liveRetrofitAPI: LiveRetrofitAPI,
    private val socketDataManager: SocketDataManager,
) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun createLiveEvent(request: CreateLiveEventRequest): Single<LiveEventInfo> {
        return liveRetrofitAPI.createLiveEvent(request)
            .flatMap { outgoerResponseConverter.convert(it) }
    }

    fun joinLiveEvent(request: JoinLiveEventRequest): Single<LiveEventInfo> {
        return liveRetrofitAPI.joinLiveEvent(request)
            .flatMap { outgoerResponseConverter.convert(it) }
    }

    fun endLiveEvent(request: EndLiveEventRequest): Single<OutgoerCommonResponse> {
        return liveRetrofitAPI.endLiveEvent(request)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun getAllActiveLiveEvent(): Single<AllActiveEventInfo> {
        return liveRetrofitAPI.getAllActiveLiveEvent().flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun verifyEvent(request: LiveEventVerifyRequest): Single<OutgoerCommonResponse> {
        return liveRetrofitAPI.verifyEvent(request)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun inviteCoHosts(coHostRequest: CoHostRequest): Single<OutgoerCommonResponse> {
        return liveRetrofitAPI.inviteOrRejectCoHosts(coHostRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun rejectCoHosts(request: CoHostRequest): Single<OutgoerCommonResponse> {
        return liveRetrofitAPI.inviteOrRejectCoHosts(request)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun liveJoinUsers(request: LiveEventWatchingUserRequest): Single<OutgoerResponse<List<LiveJoinResponse>>> {
        return liveRetrofitAPI.liveJoinUser(request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }


    //Socket
    fun liveRoom(request: LiveRoomRequest): Completable {
        return socketDataManager.liveRoom(request)
    }

    fun observeLiveWatchingCount(): Observable<LiveEventWatchingCount> {
        return socketDataManager.observeLiveWatchingCount()
    }

    fun sendComment(request: LiveEventSendOrReadComment): Completable {
        return socketDataManager.sendComment(request)
    }

    fun observeOtherLiveComment(): Observable<LiveEventSendOrReadComment> {
        return socketDataManager.observeOtherLiveComment()
    }

    fun liveEnd(request: LiveEventEndSocketEvent): Completable {
        return socketDataManager.liveEnd(request)
    }

    fun observeLiveEventEnd(): Observable<LiveEventEndSocketEvent> {
        return socketDataManager.observeLiveEventEnd()
    }

    fun liveRoomDisconnect(request: LiveRoomDisconnectRequest): Completable {
        return socketDataManager.liveRoomDisconnect(request)
    }

    fun liveUserKick(request: LiveEventKickUser): Completable {
        return socketDataManager.liveUserKick(request)
    }

    fun observeLiveEventUserKick(): Observable<LiveEventKickUser> {
        return socketDataManager.observeLiveEventUserKick()
    }

    fun sendHeart(request: SendHeartSocketEvent): Completable {
        return socketDataManager.sendHeart(request)
    }

    fun observeLiveHeart(): Observable<SendHeartSocketEvent> {
        return socketDataManager.observeLiveHeart()
    }
}