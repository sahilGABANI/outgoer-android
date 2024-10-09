package com.outgoer.ui.userverification.viewmodel

import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.api.profile.model.GetUserProfileRequest
import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.GetVenueDetailRequest
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.ui.home.newReels.viewmodel.MainReelViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class UserVerificationViewModel(private val profileRepository: ProfileRepository,
                                private val venueRepository: VenueRepository
) :
    BaseViewModel() {

    private val verificationViewStatesSubject: PublishSubject<VerificationViewState> =
        PublishSubject.create()
    val verificationViewStates: Observable<VerificationViewState> =
        verificationViewStatesSubject.hide()

    fun myProfile() {
        profileRepository.myProfile()
            .doOnSubscribe {
                verificationViewStatesSubject.onNext(VerificationViewState.LoadingState(true))
            }
            .doAfterTerminate {
                verificationViewStatesSubject.onNext(VerificationViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.data?.let {
                        verificationViewStatesSubject.onNext(VerificationViewState.MyProfileData(it))
                    }
                } else {
                    response.message?.let {
                        verificationViewStatesSubject.onNext(VerificationViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    verificationViewStatesSubject.onNext(VerificationViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getVenueDetail(venueId: Int) {
        venueRepository.getVenueDetail(GetVenueDetailRequest(venueId = venueId))
            .doOnSubscribe {
                verificationViewStatesSubject.onNext(VerificationViewState.LoadingState(true))
            }
            .doAfterTerminate {
                verificationViewStatesSubject.onNext(VerificationViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    verificationViewStatesSubject.onNext(VerificationViewState.LoadVenueDetail(it))
                }
            }, { throwable ->
                verificationViewStatesSubject.onNext(VerificationViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    verificationViewStatesSubject.onNext(VerificationViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun sendVerificationRequest(userId: Int) {
        profileRepository.sendVerificationRequest(GetUserProfileRequest(userId = userId))
            .doOnSubscribe {
                verificationViewStatesSubject.onNext(VerificationViewState.LoadingState(true))
            }
            .doAfterTerminate {
                verificationViewStatesSubject.onNext(VerificationViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                   if(it.success) {
                       verificationViewStatesSubject.onNext(VerificationViewState.SuccessMessage(it.message.toString()))
                   }
                }
            }, { throwable ->
                verificationViewStatesSubject.onNext(VerificationViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    verificationViewStatesSubject.onNext(VerificationViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


}

sealed class VerificationViewState {
    data class ErrorMessage(val errorMessage: String) : VerificationViewState()
    data class SuccessMessage(val successMessage: String) : VerificationViewState()
    data class LoadingState(val isLoading: Boolean) : VerificationViewState()
    data class MyProfileData(val outgoerUser: OutgoerUser) : VerificationViewState()
    data class LoadVenueDetail(val venueDetail: VenueDetail) : VerificationViewState()

}