package com.outgoer.ui.othernearvenue.viewmodel

import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.AddRemoveFavouriteVenueRequest
import com.outgoer.api.venue.model.GetOtherNearVenueRequest
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class OtherNearVenueViewModel(
    private val venueRepository: VenueRepository
) : BaseViewModel() {

    private val otherNearVenueListStateSubject: PublishSubject<OtherNearVenueViewState> = PublishSubject.create()
    val otherNearVenueListState: Observable<OtherNearVenueViewState> = otherNearVenueListStateSubject.hide()

    private var listOfVenueMapInfo: MutableList<VenueMapInfo> = mutableListOf()
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    fun pullToRefresh(categoryId: Int, venueId: Int) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getOtherNearVenue(categoryId, venueId)
    }

    fun loadMoreOtherNearVenue(categoryId: Int, venueId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getOtherNearVenue(categoryId, venueId)
            }
        }
    }

    private fun getOtherNearVenue(categoryId: Int, venueId: Int) {
        val request = GetOtherNearVenueRequest(
            categoryId = categoryId,
            venueId = venueId,
        )
        venueRepository.getOtherNearVenue(pageNo, request)
            .doOnSubscribe {
                otherNearVenueListStateSubject.onNext(OtherNearVenueViewState.LoadingState(true))
            }
            .doAfterTerminate {
                otherNearVenueListStateSubject.onNext(OtherNearVenueViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNo == 1) {
                        listOfVenueMapInfo = it.toMutableList()
                        otherNearVenueListStateSubject.onNext(OtherNearVenueViewState.OtherNearVenueInfoList(listOfVenueMapInfo))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfVenueMapInfo.addAll(it)
                            otherNearVenueListStateSubject.onNext(OtherNearVenueViewState.OtherNearVenueInfoList(listOfVenueMapInfo))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                otherNearVenueListStateSubject.onNext(OtherNearVenueViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    otherNearVenueListStateSubject.onNext(OtherNearVenueViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addRemoveFavouriteVenue(venueId: Int) {
        venueRepository.addRemoveFavouriteVenue(AddRemoveFavouriteVenueRequest(venueId))
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }
}

sealed class OtherNearVenueViewState {
    data class ErrorMessage(val errorMessage: String) : OtherNearVenueViewState()
    data class SuccessMessage(val successMessage: String) : OtherNearVenueViewState()
    data class LoadingState(val isLoading: Boolean) : OtherNearVenueViewState()
    data class OtherNearVenueInfoList(val listOfVenueMapInfo: List<VenueMapInfo>) : OtherNearVenueViewState()
}