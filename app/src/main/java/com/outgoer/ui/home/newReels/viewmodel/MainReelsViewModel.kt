package com.outgoer.ui.home.newReels.viewmodel

import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.GetVenueDetailRequest
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.venuedetail.viewmodel.VenueDetailViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class MainReelsViewModel(
    private val profileRepository: ProfileRepository,
    private val venueRepository: VenueRepository
) : BaseViewModel() {

    private val mainReelsViewStatesSubject: PublishSubject<MainReelViewState> =
        PublishSubject.create()
    val mainReelsViewState: Observable<MainReelViewState> = mainReelsViewStatesSubject.hide()

    fun myProfile() {
        profileRepository.myProfile()
            .doOnSubscribe {
                mainReelsViewStatesSubject.onNext(MainReelViewState.LoadingState(true))
            }
            .doAfterTerminate {
                mainReelsViewStatesSubject.onNext(MainReelViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.data?.let {
                        mainReelsViewStatesSubject.onNext(MainReelViewState.MyProfileData(it))
                    }
                } else {
                    response.message?.let {
                        mainReelsViewStatesSubject.onNext(MainReelViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    mainReelsViewStatesSubject.onNext(MainReelViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getVenueDetail(venueId: Int) {
        venueRepository.getVenueDetail(GetVenueDetailRequest(venueId = venueId))
            .doOnSubscribe {
                mainReelsViewStatesSubject.onNext(MainReelViewState.LoadingState(true))
            }
            .doAfterTerminate {
                mainReelsViewStatesSubject.onNext(MainReelViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    mainReelsViewStatesSubject.onNext(MainReelViewState.LoadVenueDetail(it))
                }
            }, { throwable ->
                mainReelsViewStatesSubject.onNext(MainReelViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    mainReelsViewStatesSubject.onNext(MainReelViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

}

sealed class MainReelViewState {
    data class ErrorMessage(val errorMessage: String) : MainReelViewState()
    data class SuccessMessage(val successMessage: String) : MainReelViewState()
    data class LoadingState(val isLoading: Boolean) : MainReelViewState()
    data class MyProfileData(val outgoerUser: OutgoerUser) : MainReelViewState()
    data class LoadVenueDetail(val venueDetail: VenueDetail) : MainReelViewState()

}