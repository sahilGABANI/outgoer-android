package com.outgoer.ui.livestreamuser.viewmodel

import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.live.LiveRepository
import com.outgoer.api.live.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.currentTime
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LiveStreamUserViewModel(
    private val liveRepository: LiveRepository,
    private val loggedInUserCache: LoggedInUserCache,
) : BaseViewModel() {

    private val liveStreamUserStateSubject: PublishSubject<LiveStreamUserViewState> = PublishSubject.create()
    val liveStreamUserState: Observable<LiveStreamUserViewState> = liveStreamUserStateSubject.hide()

    private var channelId: String? = null
    private var liveId = -1
    private var loggedInUserId = loggedInUserCache.getUserId() ?: -1
    private var selectedCoHostUserMap: MutableMap<Int, FollowUser> = mutableMapOf()

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
            liveStreamUserStateSubject.onNext(LiveStreamUserViewState.LeaveLiveRoom)
        } else {
            liveRepository.endLiveEvent(EndLiveEventRequest(channelId))
                .doAfterTerminate {
                    loggedInUserCache.removeEventChannelId(channelId ?: "")
                    sendLiveEndEvent()
                    liveRoomDisconnect()
                    liveStreamUserStateSubject.onNext(LiveStreamUserViewState.LeaveLiveRoom)
                }
                .subscribeOnIoAndObserveOnMainThread({

                }, { throwable ->
                    Timber.e(throwable)
                })
        }
    }

    fun inviteCoHost(inviteUserMap: Map<Int, FollowUser>) {
        val coHostUserList = inviteUserMap.filter { it.value.isInvited }.values.toList()
            .filter { !it.isAlreadyInvited }.take(3)
        selectedCoHostUserMap.putAll(coHostUserList.map { it.id to it })
        liveRepository.inviteCoHosts(
            CoHostRequest(
                channelId = channelId,
                inviteIds = coHostUserList.map { it.id }.joinToString(",")
            )
        ).doOnSubscribe {
            liveStreamUserStateSubject.onNext(LiveStreamUserViewState.LoadingState(true))
        }.doAfterTerminate {
            liveStreamUserStateSubject.onNext(LiveStreamUserViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            liveStreamUserStateSubject.onNext(LiveStreamUserViewState.SuccessMessage(it.message ?: ""))
        }, {
            liveStreamUserStateSubject.onNext(LiveStreamUserViewState.SuccessMessage(it.message ?: ""))
        }).autoDispose()
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
            liveStreamUserStateSubject.onNext(LiveStreamUserViewState.LiveWatchingCount(it.liveWatchingCount ?: 0))
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
            profileVerified = loggedInUserCache.getLoggedInUser()?.loggedInUser?.profileVerified ?: 0
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
        liveStreamUserStateSubject.onNext(LiveStreamUserViewState.UpdateComment(listOfLiveEventSendOrReadComment))
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

    fun liveUserKick(userId: Int, channelId: String, liveId: Int) {
        val request = LiveEventKickUser(
            userId = userId,
            channelId = channelId,
            liveId = liveId,
        )
        liveRepository.liveUserKick(request)
            .subscribeOnIoAndObserveOnMainThread({
                Timber.i("liveUserKick")
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
            liveStreamUserStateSubject.onNext(LiveStreamUserViewState.LiveHeart(it))
        }, {
            Timber.e(it)
        }).autoDispose()
    }
}

sealed class LiveStreamUserViewState {
    data class ErrorMessage(val errorMessage: String) : LiveStreamUserViewState()
    data class SuccessMessage(val successMessage: String) : LiveStreamUserViewState()
    data class LoadingState(val isLoading: Boolean) : LiveStreamUserViewState()

    data class LiveWatchingCount(val liveWatchingCount: Int) : LiveStreamUserViewState()
    object LeaveLiveRoom : LiveStreamUserViewState()
    data class UpdateComment(val listOfLiveEventSendOrReadComment: List<LiveEventSendOrReadComment>) : LiveStreamUserViewState()
    data class LiveHeart(val sendHeartSocketEvent: SendHeartSocketEvent) : LiveStreamUserViewState()
}