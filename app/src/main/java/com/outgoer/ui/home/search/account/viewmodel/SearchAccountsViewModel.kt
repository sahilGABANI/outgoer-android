package com.outgoer.ui.home.search.account.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.search.SearchRepository
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class SearchAccountsViewModel(
    private val searchRepository: SearchRepository,
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val searchAccountsViewStateSubject: PublishSubject<SearchAccountsViewState> = PublishSubject.create()
    val searchAccountsViewState: Observable<SearchAccountsViewState> = searchAccountsViewStateSubject.hide()

    //-------------------Search Account Pagination-------------------
    private var listOfSearchAccountData: MutableList<FollowUser> = mutableListOf()
    private var searchText: String? = null
    private var pageNo: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    init {
        searchRepository.searchString.subscribeOnIoAndObserveOnMainThread({
            listOfSearchAccountData.clear()
            this.searchText = it
            pageNo = 1
            isLoading = false
            isLoadMore = true
            if (it.isNotEmpty()){
                searchAccounts(true)
            } else {
                searchAccounts(false)
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun loadMoreSearchAccount() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                searchAccounts(false)
            }
        }
    }

    fun resetSearchAccountPagination(isReload: Boolean) {
        listOfSearchAccountData.clear()
        pageNo = 1
        isLoadMore = true
        isLoading = false
        searchAccounts(isReload)
    }

    fun searchAccounts(isReload: Boolean) {
        searchRepository.searchAccounts(pageNo, searchText ?: "").doOnSubscribe {
            searchAccountsViewStateSubject.onNext(SearchAccountsViewState.LoadingState(isReload))
        }.doAfterTerminate {
            searchAccountsViewStateSubject.onNext(SearchAccountsViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoading = false
            response?.let {
                if (pageNo == 1) {
                    listOfSearchAccountData.clear()
                    listOfSearchAccountData = it.toMutableList()
                    searchAccountsViewStateSubject.onNext(SearchAccountsViewState.SearchAccountList(listOfSearchAccountData))
                } else {
                    if (!it.isNullOrEmpty()) {
                        listOfSearchAccountData.addAll(it)
                        searchAccountsViewStateSubject.onNext(SearchAccountsViewState.SearchAccountList(listOfSearchAccountData))
                    } else {
                        isLoadMore = false
                    }
                }
            }
        }, { throwable ->
            searchAccountsViewStateSubject.onNext(SearchAccountsViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                searchAccountsViewStateSubject.onNext(SearchAccountsViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }
    //-------------------Search Account Pagination-------------------

    fun acceptRejectFollowRequest(followUser: FollowUser) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(followUser.id))
            .doOnSubscribe {
                searchAccountsViewStateSubject.onNext(SearchAccountsViewState.LoadingState(true))
            }
            .doAfterTerminate {
                searchAccountsViewStateSubject.onNext(SearchAccountsViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d(it.message)
            }, { throwable ->
                searchAccountsViewStateSubject.onNext(SearchAccountsViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    searchAccountsViewStateSubject.onNext(SearchAccountsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class SearchAccountsViewState {
    data class ErrorMessage(val ErrorMessage: String) : SearchAccountsViewState()
    data class LoadingState(val isLoading: Boolean) : SearchAccountsViewState()

    data class SearchAccountList(val listOfSearchAccountData: List<FollowUser>) : SearchAccountsViewState()
}