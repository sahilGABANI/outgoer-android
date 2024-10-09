package com.outgoer.ui.home.search.top.viewmodel

import com.outgoer.api.post.model.MyTagBookmarkInfo
import com.outgoer.api.search.SearchRepository
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class SearchTopViewModel(
    private val searchRepository: SearchRepository
) : BaseViewModel() {

    private val searchTopViewStateSubject: PublishSubject<SearchTopViewState> = PublishSubject.create()
    val searchTopViewState: Observable<SearchTopViewState> = searchTopViewStateSubject.hide()

    //-------------------Top Search Pagination-------------------
    private var listOfSearchTopData: MutableList<MyTagBookmarkInfo> = mutableListOf()
    private var searchText: String? = null
    private var pageNo: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    init {
        searchRepository.searchString.subscribeOnIoAndObserveOnMainThread({
            listOfSearchTopData.clear()
            this.searchText = it
            pageNo = 1
            isLoading = false
            isLoadMore = true
            if (it.isNotEmpty()){
                getTopPostReel(true)
            } else {
                getTopPostReel(false)
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun loadMoreTopPostReel() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getTopPostReel(false)
            }
        }
    }



    fun resetTopPostReelPagination(isReload: Boolean) {
        listOfSearchTopData.clear()
        searchText = ""
        pageNo = 1
        isLoadMore = true
        isLoading = false
        getTopPostReel(isReload)
    }

    fun getTopPostReel(isReload: Boolean) {
        searchRepository.getTopPostReel(pageNo, searchText ?: "").doOnSubscribe {
            searchTopViewStateSubject.onNext(SearchTopViewState.LoadingState(isReload))
        }.doAfterTerminate {
            searchTopViewStateSubject.onNext(SearchTopViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            isLoading = false
            response?.data?.let {
                if (pageNo == 1) {
                    listOfSearchTopData = response.data.toMutableList()
//                    listOfSearchTopData.distinct()
                    searchTopViewStateSubject.onNext(SearchTopViewState.SearchTopList(listOfSearchTopData))
                } else {
                    if (!it.isNullOrEmpty()) {
                        listOfSearchTopData.addAll(it)
//                        listOfSearchTopData.distinct()
                        searchTopViewStateSubject.onNext(SearchTopViewState.SearchTopList(listOfSearchTopData))
                    } else {
                        isLoadMore = false
                    }
                }
            }
        }, { throwable ->
            throwable.printStackTrace()
            throwable.localizedMessage?.let {
                searchTopViewStateSubject.onNext(SearchTopViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }
    //-------------------Top Search Pagination-------------------
}

sealed class SearchTopViewState {
    data class ErrorMessage(val errorMessage: String) : SearchTopViewState()
    data class SuccessMessage(val successMessage: String) : SearchTopViewState()
    data class LoadingState(val isLoading: Boolean) : SearchTopViewState()

    data class SearchTopList(val listOfSearchTopData: List<MyTagBookmarkInfo>) : SearchTopViewState()
}