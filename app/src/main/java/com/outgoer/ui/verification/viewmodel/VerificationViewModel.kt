package com.outgoer.ui.verification.viewmodel

import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.model.ResendCodeRequest
import com.outgoer.api.authentication.model.VerifyUserRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class VerificationViewModel(
    private val authenticationRepository: AuthenticationRepository
) : BaseViewModel() {

    private val verificationStateSubject: PublishSubject<VerificationViewState> =
        PublishSubject.create()
    val verificationState: Observable<VerificationViewState> = verificationStateSubject.hide()

    fun verifyEmail(request: VerifyUserRequest) {
        authenticationRepository.verifyEmail(request)
            .doOnSubscribe {
                verificationStateSubject.onNext(VerificationViewState.LoadingState(true))
            }.doAfterTerminate {
                verificationStateSubject.onNext(VerificationViewState.LoadingState(false))
            }.subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    verificationStateSubject.onNext(VerificationViewState.SuccessMessage(it))
                }
                verificationStateSubject.onNext(VerificationViewState.HomePageNavigation)
            }, { throwable ->
                throwable.localizedMessage?.let {
                    verificationStateSubject.onNext(VerificationViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun resendCode(resendCodeRequest: ResendCodeRequest) {
        authenticationRepository.resendCode(resendCodeRequest).doOnSubscribe {
            verificationStateSubject.onNext(VerificationViewState.LoadingState(true))
        }.doAfterTerminate {
            verificationStateSubject.onNext(VerificationViewState.LoadingState(false))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            response.message?.let {
                verificationStateSubject.onNext(VerificationViewState.SuccessMessage(it))
            }
        }, { throwable ->
            throwable.localizedMessage?.let {
                verificationStateSubject.onNext(VerificationViewState.ErrorMessage(it))
            }
        }).autoDispose()
    }

    sealed class VerificationViewState {
        data class ErrorMessage(val errorMessage: String) : VerificationViewState()
        data class SuccessMessage(val successMessage: String) : VerificationViewState()
        data class LoadingState(val isLoading: Boolean) : VerificationViewState()
        object HomePageNavigation : VerificationViewState()
    }
}