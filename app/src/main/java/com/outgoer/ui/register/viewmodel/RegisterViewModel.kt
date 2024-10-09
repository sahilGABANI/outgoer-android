package com.outgoer.ui.register.viewmodel

import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.model.ChekUsernameRequest
import com.outgoer.api.authentication.model.RegisterRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.DeactivatedAccountException
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class RegisterViewModel(private val authenticationRepository: AuthenticationRepository) : BaseViewModel() {

    private val registerStateSubject: PublishSubject<RegisterViewState> = PublishSubject.create()
    val registerState: Observable<RegisterViewState> = registerStateSubject.hide()

    fun register(registerRequest: RegisterRequest) {
        authenticationRepository.register(registerRequest).doOnSubscribe {
            registerStateSubject.onNext(RegisterViewState.LoadingState(true))
        }.doOnSuccess {

        }.doOnError {

        }.subscribeOnIoAndObserveOnMainThread({ response ->
            if (response.success) {
                response.message?.let {
                    registerStateSubject.onNext(RegisterViewState.SuccessMessage(it))
                }
                registerStateSubject.onNext(RegisterViewState.VerificationNavigation)
            } else {
                response.message?.let {
                    registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                }

                if(!(response.emailVerified ?: true)) {
                    registerStateSubject.onNext(RegisterViewState.VerificationNavigation)
                }
            }
        }, { throwable ->
            registerStateSubject.onNext(RegisterViewState.LoadingState(false))
            if (throwable is DeactivatedAccountException) {
                registerStateSubject.onNext(
                    RegisterViewState.DeactivateAccount(
                        throwable.localizedMessage ?: ""
                    )
                )
            } else {
                throwable.localizedMessage?.let {
                    registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                }
            }
        }).autoDispose()
    }

    fun checkUsername(username: String) {
        authenticationRepository.checkUsername(ChekUsernameRequest(username))
            .doOnSubscribe {
                registerStateSubject.onNext(RegisterViewState.CheckUsernameLoading(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                registerStateSubject.onNext(RegisterViewState.CheckUsernameLoading(false))
                response.data?.usernameExist?.let {
                    registerStateSubject.onNext(RegisterViewState.CheckUsernameExist(it))
                }
            }, { throwable ->
                registerStateSubject.onNext(RegisterViewState.CheckUsernameLoading(false))
                throwable.localizedMessage?.let {
                    registerStateSubject.onNext(RegisterViewState.ErrorMessage(it))
                }
                Timber.d(throwable)
            }).autoDispose()
    }

    sealed class RegisterViewState {
        data class ErrorMessage(val errorMessage: String) : RegisterViewState()
        data class SuccessMessage(val successMessage: String) : RegisterViewState()
        data class LoadingState(val isLoading: Boolean) : RegisterViewState()
        object VerificationNavigation : RegisterViewState()
        data class DeactivateAccount(val deactivateMessage: String) : RegisterViewState()

        data class CheckUsernameLoading(val isLoading: Boolean) : RegisterViewState()
        data class CheckUsernameExist(val isUsernameExist: Int) : RegisterViewState()
    }
}