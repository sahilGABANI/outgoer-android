package com.outgoer.ui.login.viewmodel

import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.model.ResendCodeRequest
import com.outgoer.api.authentication.model.VerifyUserRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VerifyResetPasswordViewModel(
    private val authenticationRepository: AuthenticationRepository
) : BaseViewModel() {

    private val verifyResetPasswordStateSubject: PublishSubject<VerifyResetPasswordState> = PublishSubject.create()
    val verifyResetPasswordState: Observable<VerifyResetPasswordState> = verifyResetPasswordStateSubject.hide()

    fun resendCode(resendCodeRequest: ResendCodeRequest) {
        authenticationRepository.resendCode(resendCodeRequest)
            .doOnSubscribe {
                verifyResetPasswordStateSubject.onNext(VerifyResetPasswordState.LoadingState(true))
            }.doAfterTerminate {
                verifyResetPasswordStateSubject.onNext(VerifyResetPasswordState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    verifyResetPasswordStateSubject.onNext(
                        VerifyResetPasswordState.SuccessMessage(
                            it
                        )
                    )
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    verifyResetPasswordStateSubject.onNext(VerifyResetPasswordState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun forgotPasswordVerifyCode(verifyUserRequest: VerifyUserRequest) {
        authenticationRepository.forgotPasswordVerifyCode(verifyUserRequest)
            .doOnSubscribe {
                verifyResetPasswordStateSubject.onNext(VerifyResetPasswordState.LoadingState(true))
            }.doAfterTerminate {
                verifyResetPasswordStateSubject.onNext(VerifyResetPasswordState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    verifyResetPasswordStateSubject.onNext(
                        VerifyResetPasswordState.SuccessMessage(
                            it
                        )
                    )
                }
                verifyResetPasswordStateSubject.onNext(VerifyResetPasswordState.ResetPasswordPage)
            }, { throwable ->
                throwable.localizedMessage?.let {
                    verifyResetPasswordStateSubject.onNext(VerifyResetPasswordState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class VerifyResetPasswordState {
        data class ErrorMessage(val errorMessage: String) : VerifyResetPasswordState()
        data class SuccessMessage(val successMessage: String) : VerifyResetPasswordState()
        data class LoadingState(val isLoading: Boolean) : VerifyResetPasswordState()
        object ResetPasswordPage : VerifyResetPasswordState()
    }
}