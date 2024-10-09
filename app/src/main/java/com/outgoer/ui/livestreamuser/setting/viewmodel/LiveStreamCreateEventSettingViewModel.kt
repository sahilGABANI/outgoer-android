package com.outgoer.ui.livestreamuser.setting.viewmodel

import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.live.LiveRepository
import com.outgoer.api.live.model.CreateLiveEventRequest
import com.outgoer.api.live.model.LiveEventInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LiveStreamCreateEventSettingViewModel(
    private val liveRepository: LiveRepository
) : BaseViewModel() {

    private val liveStreamCreateEventSettingStatesSubject: PublishSubject<LiveStreamCreateEventSettingState> = PublishSubject.create()
    val liveStreamCreateEventSettingStates: Observable<LiveStreamCreateEventSettingState> = liveStreamCreateEventSettingStatesSubject.hide()

    private var invitedUserList: List<FollowUser> = listOf()

    fun createLiveEvent(request: CreateLiveEventRequest) {
        val createLiveEventRequest = request.copy(inviteIds = invitedUserList.map { it.id }.joinToString(","))
        liveRepository.createLiveEvent(createLiveEventRequest).doOnSubscribe {
            liveStreamCreateEventSettingStatesSubject.onNext(LiveStreamCreateEventSettingState.LoadingSettingState(true))
        }.doAfterTerminate {
            liveStreamCreateEventSettingStatesSubject.onNext(LiveStreamCreateEventSettingState.LoadingSettingState(false))
        }.subscribeOnIoAndObserveOnMainThread({
            liveStreamCreateEventSettingStatesSubject.onNext(LiveStreamCreateEventSettingState.LoadCreateEventInfo(it))
        }, { throwable ->
            Timber.e(throwable)
            throwable.localizedMessage?.let {
                liveStreamCreateEventSettingStatesSubject.onNext(LiveStreamCreateEventSettingState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun updateInviteUser(inviteUserMap: Map<Int, FollowUser>) {
        invitedUserList = inviteUserMap.filter { it.value.isInvited }.values.toList()
        Timber.i(invitedUserList.map { it.id }.joinToString(","))
    }
}

sealed class LiveStreamCreateEventSettingState {
    data class ErrorMessage(val errorMessage: String) : LiveStreamCreateEventSettingState()
    data class SuccessMessage(val successMessage: String) : LiveStreamCreateEventSettingState()
    data class LoadingSettingState(val isLoading: Boolean) : LiveStreamCreateEventSettingState()
    data class LoadCreateEventInfo(val liveEventInfo: LiveEventInfo) : LiveStreamCreateEventSettingState()
}