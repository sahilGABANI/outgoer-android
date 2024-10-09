package com.outgoer.ui.latestevents.viewmodel

import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.GetVenueDetailRequest
import com.outgoer.api.venue.model.VenueEventInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class LatestEventsViewModel(
    private val venueRepository: VenueRepository,
) : BaseViewModel() {

    private val latestEventsViewStateSubjects: PublishSubject<LatestEventsViewState> = PublishSubject.create()
    val latestEventsViewState: Observable<LatestEventsViewState> = latestEventsViewStateSubjects.hide()

    private var venueEventInfoList: MutableList<VenueEventInfo> = mutableListOf()
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true


    fun pullToRefresh(venueId: Int) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        latestEventsRequest(venueId)
    }

    fun loadMoreLatestEvents(venueId: Int) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                latestEventsRequest(venueId)
            }
        }
    }

    fun latestEventsRequest(venueId: Int) {
        val request = GetVenueDetailRequest(
            venueId = venueId,
        )
        venueRepository.getLatestEventsDetail(pageNo, request)
            .doOnSubscribe {
                latestEventsViewStateSubjects.onNext(LatestEventsViewState.LoadingState(true))
            }.doAfterTerminate {
                latestEventsViewStateSubjects.onNext(LatestEventsViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNo == 1) {
                        venueEventInfoList = it.toMutableList()
                        latestEventsViewStateSubjects.onNext(LatestEventsViewState.LatestEventsList(venueEventInfoList))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            venueEventInfoList.addAll(it)
                            latestEventsViewStateSubjects.onNext(LatestEventsViewState.LatestEventsList(venueEventInfoList))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                latestEventsViewStateSubjects.onNext(LatestEventsViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    latestEventsViewStateSubjects.onNext(LatestEventsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun requestJoinEvent(eventId: Int) {
        venueRepository.requestJoinEvent(eventId)
            .doOnSubscribe {
                latestEventsViewStateSubjects.onNext(LatestEventsViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                latestEventsViewStateSubjects.onNext(LatestEventsViewState.RequestJoin(it.message ?: ""))
            }, { throwable ->
                latestEventsViewStateSubjects.onNext(LatestEventsViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    latestEventsViewStateSubjects.onNext(LatestEventsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class LatestEventsViewState {
    data class ErrorMessage(val errorMessage: String) : LatestEventsViewState()
    data class SuccessMessage(val successMessage: String) : LatestEventsViewState()
    data class LoadingState(val isLoading: Boolean) : LatestEventsViewState()
    data class LatestEventsList(val venueEventInfoList: List<VenueEventInfo>) : LatestEventsViewState()
    data class RequestJoin(val successMessage: String) : LatestEventsViewState()
}
