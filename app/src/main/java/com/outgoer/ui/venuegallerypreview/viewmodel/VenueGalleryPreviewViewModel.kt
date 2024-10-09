package com.outgoer.ui.venuegallerypreview.viewmodel

import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.DeleteVenueGalleryRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueGalleryPreviewViewModel(
    private val venueRepository: VenueRepository
) : BaseViewModel() {

    private val venueGalleryPreviewStateSubject: PublishSubject<VenueGalleryPreviewViewState> = PublishSubject.create()
    val venueGalleryPreviewState: Observable<VenueGalleryPreviewViewState> = venueGalleryPreviewStateSubject.hide()

    fun deleteVenueMedia(venueId: Int?) {
        val request = DeleteVenueGalleryRequest(
            venueIdList = listOf(venueId)
        )
        venueRepository.deleteVenueMedia(request)
            .doOnSubscribe {
                venueGalleryPreviewStateSubject.onNext(VenueGalleryPreviewViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueGalleryPreviewStateSubject.onNext(VenueGalleryPreviewViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({
                venueGalleryPreviewStateSubject.onNext(VenueGalleryPreviewViewState.DeleteVenueMediaSuccess)
            }, { throwable ->
                venueGalleryPreviewStateSubject.onNext(VenueGalleryPreviewViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueGalleryPreviewStateSubject.onNext(VenueGalleryPreviewViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class VenueGalleryPreviewViewState {
        data class ErrorMessage(val errorMessage: String) : VenueGalleryPreviewViewState()
        data class SuccessMessage(val successMessage: String) : VenueGalleryPreviewViewState()
        data class LoadingState(val isLoading: Boolean) : VenueGalleryPreviewViewState()
        object DeleteVenueMediaSuccess : VenueGalleryPreviewViewState()
    }
}