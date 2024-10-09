package com.outgoer.ui.createevent.viewmodel

import com.outgoer.api.event_category.EventCategoryRepository
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class EventCategoryViewModel(private val eventCategoryRepository: EventCategoryRepository) : BaseViewModel() {

    private val eventsCategoryViewStateSubjects: PublishSubject<EventCategoryViewState> = PublishSubject.create()
    val eventsCategoryViewState: Observable<EventCategoryViewState> = eventsCategoryViewStateSubjects.hide()

    fun getAllEventCategory()  {
        eventCategoryRepository.getEventCategory()
            .doOnSubscribe {
                eventsCategoryViewStateSubjects.onNext(EventCategoryViewState.LoadingState(true))
            }.doAfterTerminate {
                eventsCategoryViewStateSubjects.onNext(EventCategoryViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    eventsCategoryViewStateSubjects.onNext(EventCategoryViewState.VenueMapList(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    eventsCategoryViewStateSubjects.onNext(EventCategoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

}

sealed class EventCategoryViewState {
    data class ErrorMessage(val errorMessage: String) : EventCategoryViewState()
    data class SuccessMessage(val successMessage: String) : EventCategoryViewState()
    data class LoadingState(val isLoading: Boolean) : EventCategoryViewState()
    data class VenueMapList(val event: ArrayList<VenueCategory>) : EventCategoryViewState()
}
