package com.outgoer.ui.home.newmap.venueevents.viewmodel

import com.outgoer.api.event.EventRepository
import com.outgoer.api.event.model.*
import com.outgoer.api.profile.model.ReportEventRequest
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueEventViewModel(private val eventRepository: EventRepository) : BaseViewModel() {

    private val eventsViewStateSubjects: PublishSubject<EventViewState> = PublishSubject.create()
    val eventsViewState: Observable<EventViewState> = eventsViewStateSubjects.hide()

    fun getEventsList(search: String? = null, categoryId: Int? = null) {
        eventRepository.getEventsList(search, categoryId)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it?.let {
                    eventsViewStateSubjects.onNext(EventViewState.EventListDetails(it))
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getEventsDetails(eventId: Int) {
        eventRepository.getEventsDetails(eventId)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it?.let {
                    eventsViewStateSubjects.onNext(EventViewState.EventDetails(it))
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getEventCategoryList() {
        eventRepository.getEventCategoryList()
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it.data?.let {
                    eventsViewStateSubjects.onNext(EventViewState.VenueCategoryList(it))
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun createEvents(createEventResponse: CreateEventResponse) {
        eventRepository.createEvents(createEventResponse)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it?.let {
                    eventsViewStateSubjects.onNext(EventViewState.EventListDetails(it))
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addRemoveEventRequest(joinRequest: JoinRequest) {
        eventRepository.addRemoveEventRequest(joinRequest)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it?.let {
                    eventsViewStateSubjects.onNext(EventViewState.AddRemoveEventDetails(it))
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun acceptRejectRequest(requestId: Int, joinRequest: RequestResult) {
        eventRepository.acceptRejectRequest(requestId, joinRequest)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it?.let {
                    eventsViewStateSubjects.onNext(
                        EventViewState.AcceptEventDetails(
                            it.message ?: "", it.eventRequestStatus ?: 0
                        )
                    )
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun joinRequestList(requestList: RequestList) {
        eventRepository.joinRequestList(requestList)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ListRequestDetails(it))
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun reportEvent(reportEventRequest: ReportEventRequest) {
        eventRepository.reportEvent(reportEventRequest)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it.message?.let {
                    eventsViewStateSubjects.onNext(EventViewState.SuccessMessage(it))
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun deleteJoinRequestList(deletetId: Int) {
        eventRepository.deleteJoinRequestList(deletetId)
            .doOnSubscribe {
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                it.message?.let {
                    eventsViewStateSubjects.onNext(EventViewState.SuccessMessage(it))
                }
            }, { throwable ->
                eventsViewStateSubjects.onNext(EventViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    eventsViewStateSubjects.onNext(EventViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class EventViewState {
    data class ErrorMessage(val errorMessage: String) : EventViewState()
    data class SuccessMessage(val successMessage: String) : EventViewState()
    data class LoadingState(val isLoading: Boolean) : EventViewState()
    data class EventListDetails(val listofevent: EventListData) : EventViewState()
    data class EventDetails(val listofevent: EventData) : EventViewState()
    data class AddRemoveEventDetails(val joinrequest: JoinRequestResponse) : EventViewState()
    data class AcceptEventDetails(val acceptMessage: String, val accept: Int) : EventViewState()
    data class ListRequestDetails(val listofrequest: ArrayList<RequestResponseList>) :
        EventViewState()

    data class VenueCategoryList(val venueCategoryList: List<VenueCategory>) : EventViewState()

}
