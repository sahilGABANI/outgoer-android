package com.outgoer.ui.venuedetail.viewmodel

import com.outgoer.api.follow.FollowUserRepository
import com.outgoer.api.follow.model.AcceptRejectRequest
import com.outgoer.api.profile.ProfileRepository
import com.outgoer.api.profile.model.BlockUserRequest
import com.outgoer.api.profile.model.ReportUserRequest
import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class VenueDetailViewModel(
    private val venueRepository: VenueRepository,
    private val followUserRepository: FollowUserRepository,
    private val profileRepository: ProfileRepository
) : BaseViewModel() {

    private val venueDetailStateSubject: PublishSubject<VenueDetailViewState> = PublishSubject.create()
    val venueDetailState: Observable<VenueDetailViewState> = venueDetailStateSubject.hide()

    fun followUnfollow(userId: Int) {
        followUserRepository.acceptRejectFollowRequest(AcceptRejectRequest(userId))
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                throwable.printStackTrace()
                throwable.localizedMessage?.let {
                    Timber.e(it)
                }
            }).autoDispose()
    }

    fun getReviews(venueId: Int) {
        venueRepository.getReviews(GetVenueRequest(venueId))
            .doOnSubscribe {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.VenueReview((it.data ?: arrayListOf<VenueReviewModel>()) as ArrayList<VenueReviewModel>))
                    venueDetailStateSubject.onNext(VenueDetailViewState.VenueReviewCount(it.reviewAvg ?: 0.0, it.totalReview ?: 0))
                    venueDetailStateSubject.onNext(VenueDetailViewState.VenueReviewGroupCount(it.totalReview ?: 0, it.reviewGroup ?: arrayListOf(), it.userReviewAdded))
                }
            }, { throwable ->
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addReviews(addReviewRequest: AddReviewRequest) {
        venueRepository.addReviews(addReviewRequest)
            .doOnSubscribe {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.AddReviewSuccessMessage(it))
                }
            }, { throwable ->
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addPhotos(addPhotoRequest: AddPhotoRequest) {
        venueRepository.addPhotos(addPhotoRequest)
            .doOnSubscribe {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.AddPhotoSuccessMessage(it))
                }
            }, { throwable ->
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getVenueDetail(venueId: Int) {
        venueRepository.getVenueDetail(GetVenueDetailRequest(venueId = venueId))
            .doOnSubscribe {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.LoadVenueDetail(it))
                }
            }, { throwable ->
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getOtherNearVenue(categoryId: Int, venueId: Int) {
        val request = GetOtherNearVenueRequest(
            categoryId = categoryId,
            venueId = venueId,
        )
        venueRepository.getOtherNearVenue(1, request)
            .doOnSubscribe {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.OtherNearVenueInfoList(it.toMutableList()))
                }
            }, { throwable ->
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.ErrorMessage(it))
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


    fun blockUserProfile(blockUserRequest: BlockUserRequest) {
        profileRepository.blockUserProfile(blockUserRequest)
            .doOnSubscribe {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.message?.let {
                        venueDetailStateSubject.onNext(VenueDetailViewState.SuccessMessage(it))
                    }
                } else {
                    response.message?.let {
                        venueDetailStateSubject.onNext(VenueDetailViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun reportUserVenue(reportUserRequest: ReportUserRequest) {
        profileRepository.reportUserVenue(reportUserRequest)
            .doOnSubscribe {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueDetailStateSubject.onNext(VenueDetailViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                if (response.success) {
                    response.message?.let {
                        venueDetailStateSubject.onNext(
                            VenueDetailViewState.SuccessMessage(
                                it
                            )
                        )
                    }
                } else {
                    response.message?.let {
                        venueDetailStateSubject.onNext(VenueDetailViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    venueDetailStateSubject.onNext(VenueDetailViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun checkInOutVenue(checkInOutRequest: CheckInOutRequest) {
        venueRepository.checkInOutVenue(checkInOutRequest)
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({
                it.message?.let { message ->
                    venueDetailStateSubject.onNext(VenueDetailViewState.SuccessCheckInOutMessage(message))
                    venueDetailStateSubject.onNext(VenueDetailViewState.SuccessCheckInOut)
                }
            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }
}

sealed class VenueDetailViewState {
    data class ErrorMessage(val errorMessage: String) : VenueDetailViewState()
    data class SuccessMessage(val successMessage: String) : VenueDetailViewState()
    data class AddReviewSuccessMessage(val successMessage: String) : VenueDetailViewState()
    data class AddPhotoSuccessMessage(val successMessage: String) : VenueDetailViewState()
    data class LoadingState(val isLoading: Boolean) : VenueDetailViewState()
    data class LoadVenueDetail(val venueDetail: VenueDetail) : VenueDetailViewState()
    data class VenueReview(val listofvenue: ArrayList<VenueReviewModel>) : VenueDetailViewState()
    data class VenueReviewCount(val reviewAvg: Double, val totalReviews: Int) : VenueDetailViewState()
    data class OtherNearVenueInfoList(val listOfVenueMapInfo: List<VenueMapInfo>) : VenueDetailViewState()
    data class VenueReviewGroupCount(val totalReviews: Int, val listofreview: List<ReviewResponse>, val review: Boolean) : VenueDetailViewState()
    data class SuccessCheckInOutMessage(val successMessage: String) : VenueDetailViewState()
    object SuccessCheckInOut: VenueDetailViewState()
}