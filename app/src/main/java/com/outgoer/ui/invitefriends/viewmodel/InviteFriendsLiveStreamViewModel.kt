package com.outgoer.ui.invitefriends.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class InviteFriendsLiveStreamViewModel(
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val inviteFriendsLiveStreamStateSubject: PublishSubject<InviteFriendsLiveStreamViewState> = PublishSubject.create()
    val inviteFriendsLiveStreamState: Observable<InviteFriendsLiveStreamViewState> = inviteFriendsLiveStreamStateSubject.hide()

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
                inviteFriendsLiveStreamStateSubject.onNext(InviteFriendsLiveStreamViewState.LoadingState(true))
            }
            .doAfterTerminate {
                inviteFriendsLiveStreamStateSubject.onNext(InviteFriendsLiveStreamViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNo == 1) {
                        listOfFollowers = response.toMutableList()
                        inviteFriendsLiveStreamStateSubject.onNext(InviteFriendsLiveStreamViewState.FollowerList(listOfFollowers))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfFollowers.addAll(it)
                            inviteFriendsLiveStreamStateSubject.onNext(InviteFriendsLiveStreamViewState.FollowerList(listOfFollowers))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                inviteFriendsLiveStreamStateSubject.onNext(InviteFriendsLiveStreamViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    inviteFriendsLiveStreamStateSubject.onNext(InviteFriendsLiveStreamViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class InviteFriendsLiveStreamViewState {
    data class ErrorMessage(val errorMessage: String) : InviteFriendsLiveStreamViewState()
    data class SuccessMessage(val successMessage: String) : InviteFriendsLiveStreamViewState()
    data class LoadingState(val isLoading: Boolean) : InviteFriendsLiveStreamViewState()

    data class FollowerList(val listOfFollowers: List<FollowUser>) : InviteFriendsLiveStreamViewState()
    data class UpdateFollowerList(val selectedFollowList: List<FollowUser>) : InviteFriendsLiveStreamViewState()
}