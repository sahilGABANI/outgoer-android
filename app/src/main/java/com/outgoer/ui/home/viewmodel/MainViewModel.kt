package com.outgoer.ui.home.viewmodel

import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.model.UpdateNotificationTokenRequest
import com.outgoer.api.venue.model.GeoFenceResponse
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class MainViewModel(
    private val authenticationRepository: AuthenticationRepository
) : BaseViewModel() {

    companion object {
        val mainPageStateSubjects: PublishSubject<MainPageViewState> = PublishSubject.create()
        val mainPageState: Observable<MainPageViewState> = mainPageStateSubjects.hide()
    }

    fun updateNotificationToken(updateNotificationTokenRequest: UpdateNotificationTokenRequest) {
        authenticationRepository.updateNotificationToken(updateNotificationTokenRequest)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
                    mainPageStateSubjects.onNext(MainPageViewState.NotificationAlertState(it.notificationStatus))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("UpdateFCMToken".plus(it))
                }
            }).autoDispose()
    }

    fun getCheckIn() {
        authenticationRepository.getCheckIn()
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data.let {
                    mainPageStateSubjects.onNext(MainPageViewState.GetLocationInfo(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    Timber.tag("<><>").e("UpdateFCMToken".plus(it))
                }
            }).autoDispose()
    }

    sealed class MainPageViewState {
        data class ErrorMessage(val errorMessage: String) : MainPageViewState()
        data class SuccessMessage(val successMessage: String) : MainPageViewState()
        data class LoadingState(val isLoading: Boolean) : MainPageViewState()
        data class NotificationAlertState(val notificationStatus: Boolean) : MainPageViewState()
        data class GetLocationInfo(val geoFence: GeoFenceResponse?) : MainPageViewState()
    }
}