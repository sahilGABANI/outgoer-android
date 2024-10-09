package com.outgoer.ui.watchliveevent.viewmodel

import com.outgoer.api.live.LiveRepository
import com.outgoer.api.live.model.LiveEventWatchingUserRequest
import com.outgoer.api.live.model.LiveJoinResponse
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LiveWatchingUserViewModel(
    private val liveRepository: LiveRepository
) : BaseViewModel() {

    private val liveStreamingStatesSubject: PublishSubject<LiveStreamingViewState> = PublishSubject.create()
    val liveStreamingStates: Observable<LiveStreamingViewState> = liveStreamingStatesSubject.hide()

    fun liveJoinUserEvent(liveId: Int) {
        liveRepository.liveJoinUsers(LiveEventWatchingUserRequest(liveId))
            .subscribeOnIoAndObserveOnMainThread({
                it.data?.let { joinUser ->
                    liveStreamingStatesSubject.onNext(LiveStreamingViewState.LiveJoinUser(joinUser))
                }
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    sealed class LiveStreamingViewState {
        data class ErrorMessage(val errorMessage: String) : LiveStreamingViewState()
        data class SuccessMessage(val successMessage: String) : LiveStreamingViewState()
        data class LoadingState(val isLoading: Boolean) : LiveStreamingViewState()

        data class LiveJoinUser(val listUserJoin: List<LiveJoinResponse>) : LiveStreamingViewState()
    }
}