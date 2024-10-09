package com.outgoer.ui.home.newmap.venuemap.bottomsheet

import com.outgoer.api.friend_venue.FriendsVenueRepository
import com.outgoer.api.friend_venue.model.CheckInVenueResponse
import com.outgoer.api.friend_venue.model.UserVenueResponse
import com.outgoer.api.friend_venue.model.VenueDetails
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VenueDetailsViewModel(
    private val friendsVenueRepository: FriendsVenueRepository
) : BaseViewModel() {

    private val venueDetailsStateSubject: PublishSubject<VenueDetailsViewState> = PublishSubject.create()
    val venueDetailsState: Observable<VenueDetailsViewState> = venueDetailsStateSubject.hide()

    fun getVenueCategoryList(checkInVenueResponse: CheckInVenueResponse) {
        friendsVenueRepository.checkInFriendsVenue(checkInVenueResponse)
            .doOnSubscribe {
                venueDetailsStateSubject.onNext(VenueDetailsViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueDetailsStateSubject.onNext(VenueDetailsViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.venueDetails?.let {
                    venueDetailsStateSubject.onNext(VenueDetailsViewState.VenueDetailsInfo(it))
                }
                response?.data?.let {
                    venueDetailsStateSubject.onNext(VenueDetailsViewState.VenueCheckInUserInfo(it))
                }
            }, { throwable ->
                venueDetailsStateSubject.onNext(VenueDetailsViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueDetailsStateSubject.onNext(VenueDetailsViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }
}

sealed class VenueDetailsViewState {
    data class ErrorMessage(val errorMessage: String) : VenueDetailsViewState()
    data class SuccessMessage(val successMessage: String) : VenueDetailsViewState()
    data class LoadingState(val isLoading: Boolean) : VenueDetailsViewState()
    data class VenueDetailsInfo(val venueDetails: VenueDetails) : VenueDetailsViewState()
    data class VenueCheckInUserInfo(val venueMapInfoList: ArrayList<UserVenueResponse>) : VenueDetailsViewState()
}