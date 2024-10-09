package com.outgoer.ui.followdetail.viewmodel

import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.follow.model.GetFollowersAndFollowingRequest
import com.outgoer.api.mutual.MutualRepository
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class MutualViewModel(private val mutualRepository: MutualRepository) : BaseViewModel() {

    private val mutualViewStateSubject: PublishSubject<MutualViewState> = PublishSubject.create()
    val mutualViewState: Observable<MutualViewState> = mutualViewStateSubject.hide()

    private var listOfMutual: MutableList<FollowUser> = mutableListOf()
    private var searchText = ""
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    fun searchMutualList(userId: Int, searchText: String) {
        this.searchText = searchText
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getMutualList(userId)
    }

    fun loadMoreMutualList(userId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getMutualList(userId)
            }
        }
    }

    private fun getMutualList(userId: Int) {
        mutualRepository.getAllMutualList(
            pageNo,
            GetFollowersAndFollowingRequest(userId, searchText)
        )
            .doOnSubscribe {
                mutualViewStateSubject.onNext(MutualViewState.LoadingState(true))
            }
            .doAfterTerminate {
                mutualViewStateSubject.onNext(MutualViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNo == 1) {
                        listOfMutual.clear()
                        listOfMutual.addAll(response)
                        mutualViewStateSubject.onNext(
                            MutualViewState.FollowerList(
                                listOfMutual
                            )
                        )
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfMutual.addAll(it)
                            mutualViewStateSubject.onNext(
                                MutualViewState.FollowerList(
                                    listOfMutual
                                )
                            )
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                mutualViewStateSubject.onNext(MutualViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mutualViewStateSubject.onNext(MutualViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun acceptRejectFollowRequest(followUser: FollowUser) {
        mutualRepository.acceptRejectFollowRequest(AcceptRejectRequest(followUser.id))
            .doOnSubscribe {
                mutualViewStateSubject.onNext(MutualViewState.LoadingState(true))
            }
            .doAfterTerminate {
                mutualViewStateSubject.onNext(MutualViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d(it.message)
            }, { throwable ->
                mutualViewStateSubject.onNext(MutualViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mutualViewStateSubject.onNext(MutualViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class MutualViewState {
    data class ErrorMessage(val ErrorMessage: String) : MutualViewState()
    data class LoadingState(val isLoading: Boolean) : MutualViewState()
    data class FollowerList(val listOfMutual: List<FollowUser>) : MutualViewState()
}