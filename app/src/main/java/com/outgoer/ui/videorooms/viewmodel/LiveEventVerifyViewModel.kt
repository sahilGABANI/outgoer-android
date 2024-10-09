package com.outgoer.ui.videorooms.viewmodel

import com.outgoer.api.live.LiveRepository
import com.outgoer.api.live.model.LiveEventVerifyRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LiveEventVerifyViewModel(
    private val liveRepository: LiveRepository
) : BaseViewModel() {

    private val liveEventVerifyStatesSubject: PublishSubject<LiveEventVerifyState> = PublishSubject.create()
    val liveEventVerifyStates: Observable<LiveEventVerifyState> = liveEventVerifyStatesSubject.hide()

    fun verifyEvent(liveEventVerifyRequest: LiveEventVerifyRequest) {
        liveRepository.verifyEvent(liveEventVerifyRequest)
            .doOnSubscribe {
                liveEventVerifyStatesSubject.onNext(LiveEventVerifyState.LoadingState(true))
            }.doAfterTerminate {
                liveEventVerifyStatesSubject.onNext(LiveEventVerifyState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({
                liveEventVerifyStatesSubject.onNext(LiveEventVerifyState.SuccessMessage(it.message ?: ""))
            }, {
                liveEventVerifyStatesSubject.onNext(LiveEventVerifyState.ErrorMessage(it.localizedMessage ?: ""))
            }).autoDispose()
    }

    sealed class LiveEventVerifyState {
        data class ErrorMessage(val errorMessage: String) : LiveEventVerifyState()
        data class SuccessMessage(val successMessage: String) : LiveEventVerifyState()
        data class LoadingState(val isLoading: Boolean) : LiveEventVerifyState()
    }
}