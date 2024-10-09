package com.outgoer.ui.suggested.viewmodel

import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class SuggestedUsersViewModel(
    private val profileRepository: ProfileRepository,
    private val followUserRepository: FollowUserRepository,
) : BaseViewModel() {

    private val suggestedUserViewStateSubject: PublishSubject<SuggestedUsersViewState> = PublishSubject.create()
    val suggestedUserViewStates: Observable<SuggestedUsersViewState> = suggestedUserViewStateSubject.hide()

    private var listOfSuggestedUser: MutableList<OutgoerUser> = mutableListOf()
    private var pageNo = 1
    private var isLoadMore = true
    private var isLoading = false
    private val PER_PAGE_40 = 40

    fun resetSearchInfo(search: String) {
        isLoadMore = true
        isLoading = false
        pageNo = 1
        loadSuggestedUser(search)

    }
    fun loadSuggestedUser(search: String) {
        profileRepository.getSuggestedUsersList(pageNo = pageNo, perPage = PER_PAGE_40,search).doOnSubscribe {
            suggestedUserViewStateSubject.onNext(SuggestedUsersViewState.LoadingState(true))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoading = false
            response?.let {
                if (pageNo == 1) {
                    listOfSuggestedUser = response.toMutableList()
                    suggestedUserViewStateSubject.onNext(SuggestedUsersViewState.LoadSuggestedUserList(listOfSuggestedUser))
                } else {
                    if (!it.isNullOrEmpty()) {
                        listOfSuggestedUser.addAll(it)
                        suggestedUserViewStateSubject.onNext(SuggestedUsersViewState.LoadSuggestedUserList(listOfSuggestedUser))
                    } else {
                        isLoadMore = false
                    }
                }
            }
        }, { throwable ->
            isLoading = false
            suggestedUserViewStateSubject.onNext(SuggestedUsersViewState.LoadingState(false))
            Timber.e(throwable)
            throwable.localizedMessage?.let {
                suggestedUserViewStateSubject.onNext((SuggestedUsersViewState.ErrorMessage(it)))
            }
        }).autoDispose()
    }

    fun loadMore(search: String) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo++
                loadSuggestedUser(search)
            }
        }
    }

    fun followUnfollowUser(outgoerUser: OutgoerUser) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(outgoerUser.id))
            .subscribeOnIoAndObserveOnMainThread({
                Timber.e(it.message)
            }, { throwable ->
                Timber.e(throwable)
                throwable.localizedMessage?.let {
                    suggestedUserViewStateSubject.onNext((SuggestedUsersViewState.ErrorMessage(it)))
                }
            }).autoDispose()
    }
}

sealed class SuggestedUsersViewState {
    data class ErrorMessage(val errorMessage: String) : SuggestedUsersViewState()
    data class SuccessMessage(val successMessage: String) : SuggestedUsersViewState()
    data class LoadingState(val isLoading: Boolean) : SuggestedUsersViewState()
    data class LoadSuggestedUserList(val listOfSuggestedUser: List<OutgoerUser>) : SuggestedUsersViewState()
}