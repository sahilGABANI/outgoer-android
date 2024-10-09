package com.outgoer.ui.watchliveevent.viewmodel

import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.live.LiveRepository
import com.outgoer.api.live.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.currentTime
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class WatchLiveVideoViewModel(
    private val liveRepository: LiveRepository,
    private val loggedInUserCache: LoggedInUserCache
) : BaseViewModel() {

    private val watchLiveVideoListStatesSubject: PublishSubject<WatchLiveVideoListState> = PublishSubject.create()
    val watchLiveVideoListStates: Observable<WatchLiveVideoListState> = watchLiveVideoListStatesSubject.hide()

    private var channelId: String? = null
    private var liveId = -1
    private var loggedInUserId = loggedInUserCache.getUserId() ?: -1

    private var liveEventInfo: LiveEventInfo? = null

    private var listOfLiveEventSendOrReadComment: MutableList<LiveEventSendOrReadComment> = mutableListOf()

    init {
        loggedInUserCache.invitedAsCoHost.subscribeAndObserveOnMainThread {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.InviteCoHostNotification(it))
        }.autoDispose()

        observeLiveWatchingCount()
        observeOtherLiveComment()
        observeLiveEventEnd()
        observeLiveEventUserKick()
        observeLiveHeart()
    }

    fun updateLiveEventInfo(liveEventInfo: LiveEventInfo) {
        this.liveEventInfo = liveEventInfo
        this.channelId = liveEventInfo.channelId
        this.liveId = liveEventInfo.id
    }

    fun joinLiveEvent(
        isCoHost: Boolean,
        isFromNotification: Boolean = false,
        type: String? = null,
        liveEventInfo: LiveEventInfo,
    ) {
        val coHostType = if (isCoHost) {
            ROLE_PUBLISHER
        } else {
            ROLE_ATTENDEE
        }
        liveRepository.joinLiveEvent(
            JoinLiveEventRequest(
                channelId, roleType = if (!type.isNullOrEmpty()) {
                    type
                } else {
                    coHostType
                }
            )
        ).doOnSubscribe {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(true))
        }.doAfterTerminate {
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            liveRoom()
            if (!isFromNotification) {
                watchLiveVideoListStatesSubject.onNext(
                    WatchLiveVideoListState.JoinEventTokenInfo(
                        liveEventInfo.copy(token = response.token),
                        isCoHost
                    )
                )
            }
        }, {
            watchLiveVideoListStatesSubject.onNext(
                WatchLiveVideoListState.ErrorMessage(
                    it.localizedMessage ?: "Please try again"
                )
            )
        }).autoDispose()
    }

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
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LiveWatchingCount(it.liveWatchingCount ?: 0))
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
            updateComment(it)
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    private fun updateComment(liveEventSendOrReadComment: LiveEventSendOrReadComment) {
        listOfLiveEventSendOrReadComment.add(liveEventSendOrReadComment)
        watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.UpdateComment(listOfLiveEventSendOrReadComment))
    }

    private fun observeLiveEventEnd() {
        liveRepository.observeLiveEventEnd().subscribeOnIoAndObserveOnMainThread({
            Timber.e(it.toString())
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LiveEventEnd)
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun liveRoomDisconnect(isKicked: Int? = null) {
        val request = LiveRoomDisconnectRequest(
            channelId = channelId,
            liveId = liveId,
            userId = loggedInUserId,
            isKicked = isKicked,
        )
        liveRepository.liveRoomDisconnect(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("liveRoomDisconnect")
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun observeLiveEventUserKick() {
        liveRepository.observeLiveEventUserKick().subscribeOnIoAndObserveOnMainThread({
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.KickUserComment(it))
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
            watchLiveVideoListStatesSubject.onNext(WatchLiveVideoListState.LiveHeart(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    sealed class WatchLiveVideoListState {
        data class ErrorMessage(val errorMessage: String) : WatchLiveVideoListState()
        data class SuccessMessage(val successMessage: String) : WatchLiveVideoListState()
        data class LoadingState(val isLoading: Boolean) : WatchLiveVideoListState()
        data class LiveWatchingCount(val liveWatchingCount: Int) : WatchLiveVideoListState()
        data class UpdateComment(val listOfLiveEventSendOrReadComment: List<LiveEventSendOrReadComment>) : WatchLiveVideoListState()
        data class InviteCoHostNotification(val liveEventInfo: LiveEventInfo) : WatchLiveVideoListState()
        data class JoinEventTokenInfo(val liveEventInfo: LiveEventInfo, val isCoHost: Boolean) : WatchLiveVideoListState()
        object LiveEventEnd : WatchLiveVideoListState()
        data class KickUserComment(val liveEventKickUser: LiveEventKickUser) : WatchLiveVideoListState()
        data class LiveHeart(val sendHeartSocketEvent: SendHeartSocketEvent) : WatchLiveVideoListState()
    }
}