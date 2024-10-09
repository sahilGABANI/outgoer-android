package com.outgoer.ui.login.viewmodel

import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.model.ResetPasswordRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ResetPasswordViewModel(
    private val authenticationRepository: AuthenticationRepository
): BaseViewModel() {

    private val resetPasswordStateSubject: PublishSubject<ResetPasswordState> = PublishSubject.create()
    val resetPasswordState: Observable<ResetPasswordState> = resetPasswordStateSubject.hide()

    fun resetPassword(resetPasswordRequest: ResetPasswordRequest) {
        authenticationRepository.resetPassword(resetPasswordRequest)
            .doOnSubscribe {
                resetPasswordStateSubject.onNext(ResetPasswordState.LoadingState(true))
            }.doAfterTerminate {
                resetPasswordStateSubject.onNext(ResetPasswordState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    resetPasswordStateSubject.onNext(ResetPasswordState.SuccessMessage(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    resetPasswordStateSubject.onNext(ResetPasswordState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class ResetPasswordState {
        data class ErrorMessage(val errorMessage: String) : ResetPasswordState()
        data class SuccessMessage(val successMessage: String) : ResetPasswordState()
        data class LoadingState(val isLoading: Boolean) : ResetPasswordState()
    }
}