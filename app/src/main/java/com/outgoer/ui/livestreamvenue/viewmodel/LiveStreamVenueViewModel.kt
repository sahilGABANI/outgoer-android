package com.outgoer.ui.livestreamvenue.viewmodel

import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.live.LiveRepository
import com.outgoer.api.live.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.currentTime
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LiveStreamVenueViewModel(
    private val liveRepository: LiveRepository,
    private val loggedInUserCache: LoggedInUserCache,
) : BaseViewModel() {

    private val liveStreamVenueStateSubject: PublishSubject<LiveStreamVenueViewState> = PublishSubject.create()
    val liveStreamVenueState: Observable<LiveStreamVenueViewState> = liveStreamVenueStateSubject.hide()

    private var channelId: String? = null
    private var liveId = -1
    private var loggedInUserId = loggedInUserCache.getUserId() ?: -1

    private var listOfLiveEventSendOrReadComment: MutableList<LiveEventSendOrReadComment> = mutableListOf()

    init {
        observeLiveWatchingCount()
        observeOtherLiveComment()
        observeLiveHeart()
    }

    fun updateChannelId(channelId: String?, liveId: Int) {
        this.channelId = channelId
        this.liveId = liveId
        loggedInUserCache.setEventChannelId(channelId ?: "")
        liveRoom()
    }

    fun endLiveEvent() {
        if (channelId.isNullOrEmpty()) {
            loggedInUserCache.setEventChannelId("")
            liveStreamVenueStateSubject.onNext(LiveStreamVenueViewState.LeaveLiveRoom)
        } else {
            liveRepository.endLiveEvent(EndLiveEventRequest(channelId))
                .doAfterTerminate {
                    loggedInUserCache.removeEventChannelId(channelId ?: "")
                    sendLiveEndEvent()
                    liveRoomDisconnect()
                    liveStreamVenueStateSubject.onNext(LiveStreamVenueViewState.LeaveLiveRoom)
                }
                .subscribeOnIoAndObserveOnMainThread({

                }, { throwable ->
                    Timber.e(throwable)
                })
        }
    }

    //Socket
    private fun liveRoom() {
        val request = LiveRoomRequest(
            channelId = channelId,
            liveId = liveId,
            userId = loggedInUserId,
        )
        liveRepository.liveRoom(request).subscribeOnIoAndObserveOnMainThread({
            Timber.i("liveRoom")
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun observeLiveWatchingCount() {
        liveRepository.observeLiveWatchingCount().subscribeOnIoAndObserveOnMainThread({
            liveStreamVenueStateSubject.onNext(LiveStreamVenueViewState.LiveWatchingCount(it.liveWatchingCount ?: 0))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun sendComment(comment: String) {
        val request = LiveEventSendOrReadComment(
            channelId = channelId,
            liveId = liveId,
            userId = loggedInUserId,
            //Additional Params
            id = currentTime.toString(),
            name = loggedInUserCache.getLoggedInUser()?.loggedInUser?.username ?: "",
            username = loggedInUserCache.getLoggedInUser()?.loggedInUser?.username ?: "",
            profileUrl = loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar ?: "",
            comment = comment,
        )
        liveRepository.sendComment(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("sendComment")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun observeOtherLiveComment() {
        liveRepository.observeOtherLiveComment().subscribeOnIoAndObserveOnMainThread({
            Timber.e(it.toString())
            updateComment(it)
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun updateComment(liveEventSendOrReadComment: LiveEventSendOrReadComment) {
        listOfLiveEventSendOrReadComment.add(liveEventSendOrReadComment)
        liveStreamVenueStateSubject.onNext(LiveStreamVenueViewState.UpdateComment(listOfLiveEventSendOrReadComment))
    }

    private fun sendLiveEndEvent() {
        val request = LiveEventEndSocketEvent(
            channelId = channelId,
            liveId = liveId,
            userId = loggedInUserId,
        )

        liveRepository.liveEnd(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("liveRoomDisconnect")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun liveRoomDisconnect() {
        val request = LiveRoomDisconnectRequest(
            channelId = channelId,
            liveId = liveId,
            userId = loggedInUserId,
        )

        liveRepository.liveRoomDisconnect(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("liveRoomDisconnect")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    fun sendHeart() {
        val request = SendHeartSocketEvent(
            id = currentTime.toString(),
            channelId = channelId,
            liveId = liveId,
            userId = loggedInUserId
        )
        liveRepository.sendHeart(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("sendHeart")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun observeLiveHeart() {
        liveRepository.observeLiveHeart().subscribeOnIoAndObserveOnMainThread({
            liveStreamVenueStateSubject.onNext(LiveStreamVenueViewState.LiveHeart(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }
}

sealed class LiveStreamVenueViewState {
    data class ErrorMessage(val errorMessage: String) : LiveStreamVenueViewState()
    data class SuccessMessage(val successMessage: String) : LiveStreamVenueViewState()
    data class LoadingState(val isLoading: Boolean) : LiveStreamVenueViewState()

    data class LiveWatchingCount(val liveWatchingCount: Int) : LiveStreamVenueViewState()
    object LeaveLiveRoom : LiveStreamVenueViewState()
    data class UpdateComment(val listOfLiveEventSendOrReadComment: List<LiveEventSendOrReadComment>) : LiveStreamVenueViewState()
    data class LiveHeart(val sendHeartSocketEvent: SendHeartSocketEvent) : LiveStreamVenueViewState()
}