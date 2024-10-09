package com.outgoer.ui.home.search.place.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.search.SearchRepository
import com.outgoer.api.venue.model.VenueListInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.followdetail.viewmodel.FollowersViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class SearchPlacesViewModel(
    private val searchRepository: SearchRepository,
    private val followUserRepository: FollowUserRepository
) : BaseViewModel() {

    private val searchPlacesViewStateSubject: PublishSubject<SearchPlacesViewState> = PublishSubject.create()
    val searchPlacesViewState: Observable<SearchPlacesViewState> = searchPlacesViewStateSubject.hide()

    //-------------------Search Places Pagination-------------------
    private var listOfSearchPlacesData: MutableList<VenueListInfo> = mutableListOf()
    private var searchText: String? = null
    private var pageNo: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    init {
        searchRepository.searchString.subscribeOnIoAndObserveOnMainThread({
            listOfSearchPlacesData.clear()
            this.searchText = it
            pageNo = 1
            isLoading = false
            isLoadMore = true
            if (it.isNotEmpty()) {
                searchPlaces(true)
            } else {
                searchPlaces(false)
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

//    fun searchPlacesBySearchText(searchText: String) {
//        listOfSearchPlacesData.clear()
//        this.searchText = searchText
//        pageNo = 1
//        isLoading = false
//        isLoadMore = true
//        searchPlaces()
//    }

    fun loadMoreSearchPlaces() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                searchPlaces(false)
            }
        }
    }

    fun resetSearchPlacesPagination(isReload: Boolean) {
        listOfSearchPlacesData.clear()
        searchText = ""
        pageNo = 1
        isLoadMore = true
        isLoading = false
        searchPlaces(isReload)
    }

    fun searchPlaces(isReload: Boolean) {
        searchRepository.searchPlaces(pageNo, searchText ?: "").doOnSubscribe {
            searchPlacesViewStateSubject.onNext(SearchPlacesViewState.LoadingState(isReload))
        }.doAfterTerminate {
            searchPlacesViewStateSubject.onNext(SearchPlacesViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoading = false
            response?.let {
                if (pageNo == 1) {
                    listOfSearchPlacesData = response.toMutableList()
                    searchPlacesViewStateSubject.onNext(SearchPlacesViewState.SearchPlacesList(listOfSearchPlacesData))
                } else {
                    if (!it.isNullOrEmpty()) {
                        listOfSearchPlacesData.addAll(it)
                        searchPlacesViewStateSubject.onNext(SearchPlacesViewState.SearchPlacesList(listOfSearchPlacesData))
                    } else {
                        isLoadMore = false
                    }
                }
            }
        }, { throwable ->
            searchPlacesViewStateSubject.onNext(SearchPlacesViewState.LoadingState(false))
            throwable.localizedMessage?.let {
                searchPlacesViewStateSubject.onNext(SearchPlacesViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }
    //-------------------Search Places Pagination-------------------


    fun acceptRejectFollowRequest(userId : Int) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(userId))
            .doOnSubscribe {
                searchPlacesViewStateSubject.onNext(SearchPlacesViewState.LoadingState(true))
            }
            .doAfterTerminate {
                searchPlacesViewStateSubject.onNext(SearchPlacesViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({
                Timber.d(it.message)
            }, { throwable ->
                searchPlacesViewStateSubject.onNext(SearchPlacesViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    searchPlacesViewStateSubject.onNext(SearchPlacesViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class SearchPlacesViewState {
    data class ErrorMessage(val ErrorMessage: String) : SearchPlacesViewState()
    data class LoadingState(val isLoading: Boolean) : SearchPlacesViewState()

    data class SearchPlacesList(val listOfSearchPlacesData: List<VenueListInfo>) : SearchPlacesViewState()
}