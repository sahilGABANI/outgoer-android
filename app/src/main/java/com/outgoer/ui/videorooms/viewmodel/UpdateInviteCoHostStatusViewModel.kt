package com.outgoer.ui.videorooms.viewmodel

import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.live.LiveRepository
import com.outgoer.api.live.model.CoHostRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class UpdateInviteCoHostStatusViewModel(
    private val liveRepository: LiveRepository,
    private val loggedInUserCache: LoggedInUserCache
) : BaseViewModel() {

    private val updateInviteCoHostStatusStatesSubject: PublishSubject<UpdateInviteCoHostStatus> = PublishSubject.create()
    val updateInviteCoHostStatusStates: Observable<UpdateInviteCoHostStatus> = updateInviteCoHostStatusStatesSubject.hide()

    val loggedInUser = loggedInUserCache.getLoggedInUser()?.loggedInUser

    fun rejectAsCoHost(channelId: String) {
        liveRepository.rejectCoHosts(CoHostRequest(channelId, loggedInUser?.id.toString()))
            .doOnSubscribe {
                updateInviteCoHostStatusStatesSubject.onNext(
                    UpdateInviteCoHostStatus.LoadingSettingState(
                        true
                    )
                )
            }.doAfterTerminate {
                updateInviteCoHostStatusStatesSubject.onNext(
                    UpdateInviteCoHostStatus.LoadingSettingState(
                        false
                    )
                )
            }.subscribeOnIoAndObserveOnMainThread({
                updateInviteCoHostStatusStatesSubject.onNext(UpdateInviteCoHostStatus.RejectedCoHostRequest)
            }, {
                updateInviteCoHostStatusStatesSubject.onNext(
                    UpdateInviteCoHostStatus.ErrorMessage(
                        it.localizedMessage ?: ""
                    )
                )
            }).autoDispose()
    }

    sealed class UpdateInviteCoHostStatus {
        data class ErrorMessage(val errorMessage: String) : UpdateInviteCoHostStatus()
        data class SuccessMessage(val successMessage: String) : UpdateInviteCoHostStatus()
        data class LoadingSettingState(val isLoading: Boolean) : UpdateInviteCoHostStatus()
        object RejectedCoHostRequest : UpdateInviteCoHostStatus()
    }
}