package com.outgoer.ui.videorooms.viewmodel

import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.live.LiveRepository
import com.outgoer.api.live.model.AllActiveEventInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class VideoRoomsViewModel(
    private val liveRepository: LiveRepository,
    private val loggedInUserCache: LoggedInUserCache
) : BaseViewModel() {

    private val videoRoomsStateSubject: PublishSubject<VideoRoomsViewState> = PublishSubject.create()
    val videoRoomsState: Observable<VideoRoomsViewState> = videoRoomsStateSubject.hide()

    init {
        loggedInUserCache.invitedAsCoHost.subscribeAndObserveOnMainThread {
            getAllActiveLiveEvent()
        }.autoDispose()

    }

    fun getAllActiveLiveEvent() {
        liveRepository.getAllActiveLiveEvent().doOnSubscribe {
            videoRoomsStateSubject.onNext(VideoRoomsViewState.LoadingState(true))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            response?.let {
                videoRoomsStateSubject.onNext(VideoRoomsViewState.LoadAllActiveEventList(response))
            }
        }, { throwable ->
            Timber.e(throwable)
            throwable.localizedMessage?.let {
                videoRoomsStateSubject.onNext((VideoRoomsViewState.ErrorMessage(it)))
            }
        }).autoDispose()
    }
}

sealed class VideoRoomsViewState {
    data class ErrorMessage(val errorMessage: String) : VideoRoomsViewState()
    data class SuccessMessage(val successMessage: String) : VideoRoomsViewState()
    data class LoadingState(val isLoading: Boolean) : VideoRoomsViewState()

    data class LoadAllActiveEventList(val allActiveEventInfo: AllActiveEventInfo) : VideoRoomsViewState()
}