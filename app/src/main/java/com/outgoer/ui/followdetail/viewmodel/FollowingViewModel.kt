package com.outgoer.ui.followdetail.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.api.group.GroupRepository
import com.outgoer.api.group.model.ManageGroupRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class FollowingViewModel(
    private val followUserRepository: FollowUserRepository,
    private val groupRepository: GroupRepository
) : BaseViewModel() {

    private val followingViewStateSubject: PublishSubject<FollowingViewState> = PublishSubject.create()
    val followingViewState: Observable<FollowingViewState> = followingViewStateSubject.hide()

    private var listOfFollowing: MutableList<FollowUser> = mutableListOf()
    private var searchText = ""
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    fun searchFollowingList(userId: Int, searchText: String) {
        this.searchText = searchText
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getFollowingList(userId)
    }

    fun loadMoreFollowingList(userId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getFollowingList(userId)
            }
        }
    }

    fun resetPagination(userId: Int) {
        searchText = ""
        listOfFollowing.clear()
        pageNo = 1
        isLoadMore = true
        isLoading = false
        getFollowingList(userId)
    }

    private fun getFollowingList(userId: Int) {
        followUserRepository.getAllFollowingList(pageNo, GetFollowersAndFollowingRequest(userId, searchText))
            .doOnSubscribe {
                followingViewStateSubject.onNext(FollowingViewState.LoadingState(true))
            }
            .doAfterTerminate {
                followingViewStateSubject.onNext(FollowingViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNo == 1) {
                        listOfFollowing.clear()
                        listOfFollowing.addAll(response)
                        followingViewStateSubject.onNext(FollowingViewState.FollowingList(listOfFollowing))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfFollowing.addAll(it)
                            followingViewStateSubject.onNext(FollowingViewState.FollowingList(listOfFollowing))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                followingViewStateSubject.onNext(FollowingViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    followingViewStateSubject.onNext(FollowingViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun acceptRejectFollowRequest(followUser: FollowUser) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(followUser.id))
            .doOnSubscribe {
                followingViewStateSubject.onNext(FollowingViewState.LoadingState(true))
            }
            .doAfterTerminate {
                followingViewStateSubject.onNext(FollowingViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d(it.message)
            }, { throwable ->
                followingViewStateSubject.onNext(FollowingViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    followingViewStateSubject.onNext(FollowingViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addGroupUser(manageGroupRequest: ManageGroupRequest) {
        groupRepository.addGroupUser(manageGroupRequest)
            .doOnSubscribe {
                followingViewStateSubject.onNext(FollowingViewState.GroupLoadingState(true))
            }
            .doAfterTerminate {
                followingViewStateSubject.onNext(FollowingViewState.GroupLoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    followingViewStateSubject.onNext(FollowingViewState.AddUserToSuccessMessage(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("CreateGroupRequest".plus(it))
                }
            }).autoDispose()
    }
}

sealed class FollowingViewState {
    data class AddUserToSuccessMessage(val successMessage: String) : FollowingViewState()
    data class ErrorMessage(val ErrorMessage: String) : FollowingViewState()
    data class LoadingState(val isLoading: Boolean) : FollowingViewState()
    data class GroupLoadingState(val isLoading: Boolean) : FollowingViewState()
    data class FollowingList(val listOfFollowing: List<FollowUser>) : FollowingViewState()
}