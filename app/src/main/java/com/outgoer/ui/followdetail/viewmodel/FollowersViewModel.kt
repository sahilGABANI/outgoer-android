package com.outgoer.ui.followdetail.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class FollowersViewModel(
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val followersViewStateSubject: PublishSubject<FollowersViewState> = PublishSubject.create()
    val followersViewState: Observable<FollowersViewState> = followersViewStateSubject.hide()

    private var listOfFollowers: MutableList<FollowUser> = mutableListOf()
    private var searchText = ""
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    fun searchFollowersList(userId: Int, searchText: String) {
        this.searchText = searchText
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getFollowersList(userId)
    }

    fun loadMoreFollowersList(userId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getFollowersList(userId)
            }
        }
    }

    private fun getFollowersList(userId: Int) {
        followUserRepository.getAllFollowersList(pageNo, GetFollowersAndFollowingRequest(userId, searchText))
            .doOnSubscribe {
                followersViewStateSubject.onNext(FollowersViewState.LoadingState(true))
            }
            .doAfterTerminate {
                followersViewStateSubject.onNext(FollowersViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNo == 1) {
                        listOfFollowers.clear()
                        listOfFollowers.addAll(response)
                        followersViewStateSubject.onNext(FollowersViewState.FollowerList(listOfFollowers))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfFollowers.addAll(it)
                            followersViewStateSubject.onNext(FollowersViewState.FollowerList(listOfFollowers))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                followersViewStateSubject.onNext(FollowersViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    followersViewStateSubject.onNext(FollowersViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun acceptRejectFollowRequest(followUser: FollowUser) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(followUser.id))
            .doOnSubscribe {
                followersViewStateSubject.onNext(FollowersViewState.LoadingState(true))
            }
            .doAfterTerminate {
                followersViewStateSubject.onNext(FollowersViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d(it.message)
            }, { throwable ->
                followersViewStateSubject.onNext(FollowersViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    followersViewStateSubject.onNext(FollowersViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class FollowersViewState {
    data class ErrorMessage(val ErrorMessage: String) : FollowersViewState()
    data class LoadingState(val isLoading: Boolean) : FollowersViewState()
    data class FollowerList(val listOfFollowers: List<FollowUser>) : FollowersViewState()
}