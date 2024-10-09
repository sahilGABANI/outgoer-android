package com.outgoer.ui.newvenuedetail.viewmodel

import com.outgoer.api.event.model.EventListData
import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.AddRemoveFavouriteVenueRequest
import com.outgoer.api.venue.model.GetVenueRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class VenueListViewModel(
    private val venueRepository: VenueRepository
) : BaseViewModel() {

    private val venueListStateSubject: PublishSubject<VenueViewState> = PublishSubject.create()
    val venueListState: Observable<VenueViewState> = venueListStateSubject.hide()

    fun addRemoveFavouriteVenue(venueId: Int) {
        venueRepository.addRemoveFavouriteVenue(AddRemoveFavouriteVenueRequest(venueId))
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }

    fun getEventVenueData(venueId: Int) {
        venueRepository.getEventVenueData(GetVenueRequest(venueId))
            .doOnSubscribe {
                venueListStateSubject.onNext(VenueViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueListStateSubject.onNext(VenueViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    venueListStateSubject.onNext(VenueViewState.EventDetailsInfo(it))
                }
            }, { throwable ->
                venueListStateSubject.onNext(VenueViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueListStateSubject.onNext(VenueViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class VenueViewState {
    data class ErrorMessage(val errorMessage: String) : VenueViewState()
    data class SuccessMessage(val successMessage: String) : VenueViewState()
    data class LoadingState(val visLoading: Boolean) : VenueViewState()
    data class EventDetailsInfo(val listOfVenueInfo: EventListData) : VenueViewState()
}