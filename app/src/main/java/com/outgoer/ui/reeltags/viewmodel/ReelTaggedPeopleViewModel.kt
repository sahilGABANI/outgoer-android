package com.outgoer.ui.reeltags.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.reels.ReelsRepository
import com.outgoer.api.reels.model.ReelsTagsItem
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReelTaggedPeopleViewModel(
    private val reelsRepository: ReelsRepository,
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val reelTaggedPeopleViewStateSubject: PublishSubject<ReelTaggedPeopleViewState> = PublishSubject.create()
    val reelTaggedPeopleViewState: Observable<ReelTaggedPeopleViewState> = reelTaggedPeopleViewStateSubject.hide()

    fun getReelTaggedPeople(reelId: Int) {
        reelsRepository.getReelTaggedPeople(reelId).doOnSubscribe {
            reelTaggedPeopleViewStateSubject.onNext(ReelTaggedPeopleViewState.LoadingState(true))
        }.doAfterTerminate {
            reelTaggedPeopleViewStateSubject.onNext(ReelTaggedPeopleViewState.LoadingState(false))
        }.doAfterSuccess {
            reelTaggedPeopleViewStateSubject.onNext(ReelTaggedPeopleViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            response?.data?.let {
                reelTaggedPeopleViewStateSubject.onNext(ReelTaggedPeopleViewState.GetTaggedPeopleList(it))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                reelTaggedPeopleViewStateSubject.onNext(ReelTaggedPeopleViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    fun followUnfollow(reelsTagsItem: ReelsTagsItem) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(reelsTagsItem.userId ?: 0))
            .doOnSubscribe {
                reelTaggedPeopleViewStateSubject.onNext(ReelTaggedPeopleViewState.LoadingState(true))
            }
            .doAfterTerminate {
                reelTaggedPeopleViewStateSubject.onNext(ReelTaggedPeopleViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                reelTaggedPeopleViewStateSubject.onNext(ReelTaggedPeopleViewState.SuccessMessage(it.toString()))
            }, {
                reelTaggedPeopleViewStateSubject.onNext((ReelTaggedPeopleViewState.ErrorMessage(it.message.toString())))
            }).autoDispose()
    }
}

sealed class ReelTaggedPeopleViewState {
    data class SuccessMessage(val successMessage: String) : ReelTaggedPeopleViewState()
    data class ErrorMessage(val errorMessage: String) : ReelTaggedPeopleViewState()
    data class LoadingState(val isLoading: Boolean) : ReelTaggedPeopleViewState()
    data class GetTaggedPeopleList(val listOfTaggedPeople: List<ReelsTagsItem>) : ReelTaggedPeopleViewState()
}