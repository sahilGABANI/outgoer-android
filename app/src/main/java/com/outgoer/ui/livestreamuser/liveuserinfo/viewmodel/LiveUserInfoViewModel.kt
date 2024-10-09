package com.outgoer.ui.livestreamuser.liveuserinfo.viewmodel

import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LiveUserInfoViewModel(
    private val profileRepository: ProfileRepository,
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val liveUserInfoViewStateSubject: PublishSubject<LiveUserInfoViewState> = PublishSubject.create()
    val liveUserInfoViewState: Observable<LiveUserInfoViewState> = liveUserInfoViewStateSubject.hide()

    fun getUserProfile(userId: Int) {
        profileRepository.getUserProfile(userId)
            .doOnSubscribe {
                liveUserInfoViewStateSubject.onNext(LiveUserInfoViewState.LoadingState(true))
            }
            .doAfterTerminate {
                liveUserInfoViewStateSubject.onNext(LiveUserInfoViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.data?.let {
                        liveUserInfoViewStateSubject.onNext(LiveUserInfoViewState.LoadUserProfileDetail(it))
                    }
                } else {
                    response.message?.let {
                        liveUserInfoViewStateSubject.onNext(LiveUserInfoViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    liveUserInfoViewStateSubject.onNext(LiveUserInfoViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun followUnfollow(userId: Int) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(userId))
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                throwable.printStackTrace()
                throwable.localizedMessage?.let {
                    Timber.e(it)
                }
            }).autoDispose()
    }

    sealed class LiveUserInfoViewState {
        data class ErrorMessage(val errorMessage: String) : LiveUserInfoViewState()
        data class SuccessMessage(val successMessage: String) : LiveUserInfoViewState()
        data class LoadingState(val isLoading: Boolean) : LiveUserInfoViewState()
        data class LoadUserProfileDetail(val outgoerUser: OutgoerUser) : LiveUserInfoViewState()
    }
}