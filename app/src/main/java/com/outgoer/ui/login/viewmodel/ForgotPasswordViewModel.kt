package com.outgoer.ui.login.viewmodel

import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.model.ForgotPasswordRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ForgotPasswordViewModel(
    private val authenticationRepository: AuthenticationRepository
) : BaseViewModel() {

    private val forgotPasswordStateSubject: PublishSubject<ForgotPasswordState> = PublishSubject.create()
    val forgotPasswordState: Observable<ForgotPasswordState> = forgotPasswordStateSubject.hide()

    fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest) {
        authenticationRepository.forgotPassword(forgotPasswordRequest)
            .doOnSubscribe {
                forgotPasswordStateSubject.onNext(ForgotPasswordState.LoadingState(true))
            }.doAfterTerminate {
                forgotPasswordStateSubject.onNext(ForgotPasswordState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    forgotPasswordStateSubject.onNext(ForgotPasswordState.SuccessMessage(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    forgotPasswordStateSubject.onNext(ForgotPasswordState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class ForgotPasswordState {
        data class ErrorMessage(val errorMessage: String) : ForgotPasswordState()
        data class SuccessMessage(val successMessage: String) : ForgotPasswordState()
        data class LoadingState(val isLoading: Boolean) : ForgotPasswordState()
    }
}