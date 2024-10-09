package com.outgoer.ui.add_hashtag.viewmodel

import com.outgoer.api.hashtag.HashtagRepository
import com.outgoer.api.hashtag.model.HashtagResponse
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class HashtagViewModel(
    private val hashtagRepository: HashtagRepository
) : BaseViewModel() {

    private val hashtagStateSubject: PublishSubject<HashtagInfoViewState> = PublishSubject.create()
    val hashtagState: Observable<HashtagInfoViewState> = hashtagStateSubject.hide()

    private var listOfHashtag: MutableList<HashtagResponse> = mutableListOf()

    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    fun resetPagination(search: String? = null) {
        listOfHashtag.clear()
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        getHashtagList(search)
    }

    fun loadMore(search: String? = null) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getHashtagList(search)
            }
        }
    }
    fun getHashtagList(search: String? = null) {
        hashtagRepository.getHashtagList(pageNumber, search)
            .doOnSubscribe {
                hashtagStateSubject.onNext(HashtagInfoViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({

                if(pageNumber == 1) {
                    listOfHashtag.clear()
                }
                hashtagStateSubject.onNext(HashtagInfoViewState.LoadingState(false))

                it.data?.let { hashTagList ->
                    listOfHashtag.addAll(hashTagList)
                    hashtagStateSubject.onNext(HashtagInfoViewState.GetHashtagList(listOfHashtag))
                }
            }, { throwable ->
                hashtagStateSubject.onNext(HashtagInfoViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    hashtagStateSubject.onNext(HashtagInfoViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class HashtagInfoViewState {
        data class LoadingState(val isLoading: Boolean) : HashtagInfoViewState()
        data class ErrorMessage(val errorMessage: String) : HashtagInfoViewState()
        data class GetHashtagList(val listofHashtagInfo: MutableList<HashtagResponse>) : HashtagInfoViewState()
    }
}