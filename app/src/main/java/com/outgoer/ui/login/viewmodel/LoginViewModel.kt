package com.outgoer.ui.login.viewmodel

import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.DeactivatedAccountException
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class LoginViewModel(
    private val authenticationRepository: AuthenticationRepository
) : BaseViewModel() {

    private val loginStateSubject: PublishSubject<LoginViewState> = PublishSubject.create()
    val loginState: Observable<LoginViewState> = loginStateSubject.hide()

    fun login(loginRequest: LoginRequest) {
        authenticationRepository.login(loginRequest)
            .doOnSubscribe {
                loginStateSubject.onNext(LoginViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                loginStateSubject.onNext(LoginViewState.LoadingState(false))
                if (response.success) {
                    manageLoginResponse(response)
                } else {
                    response.message?.let {
                        loginStateSubject.onNext(LoginViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                loginStateSubject.onNext(LoginViewState.LoadingState(false))
                if (throwable is DeactivatedAccountException) {
                    loginStateSubject.onNext(
                        LoginViewState.DeactivateAccount(
                            throwable.localizedMessage ?: ""
                        )
                    )
                } else {
                    throwable.localizedMessage?.let {
                        loginStateSubject.onNext(LoginViewState.ErrorMessage(it))
                    }
                }
            }).autoDispose()
    }

    fun checkSocialId(request: CheckSocialIdExistRequest) {
        authenticationRepository.checkSocialId(request).doOnSubscribe {
            loginStateSubject.onNext(LoginViewState.LoadingState(true))
        }.subscribeOnIoAndObserveOnMainThread({ response ->
            loginStateSubject.onNext(LoginViewState.LoadingState(false))
            response.data?.let {
                if (it.username.isNullOrEmpty() || it.email.isNullOrEmpty()) {
                    loginStateSubject.onNext(LoginViewState.AddUsernameEmailDialog(it.username, it.email))
                } else {
                    loginStateSubject.onNext(LoginViewState.ContinueSocialLogin(it.username, it.email))
                }
            }
        }, { throwable ->
            loginStateSubject.onNext(LoginViewState.LoadingState(false))
            if (throwable is DeactivatedAccountException) {
                loginStateSubject.onNext(
                    LoginViewState.DeactivateAccount(
                        throwable.localizedMessage ?: ""
                    )
                )
            } else {
                throwable.localizedMessage?.let {
                    loginStateSubject.onNext(LoginViewState.ErrorMessage(it))
                }
            }
        }).autoDispose()
    }

    fun socialLogin(socialMediaLoginRequest: SocialMediaLoginRequest) {
        authenticationRepository.socialLogin(socialMediaLoginRequest)
            .doOnSubscribe {
                loginStateSubject.onNext(LoginViewState.LoadingState(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                loginStateSubject.onNext(LoginViewState.LoadingState(false))
                if (response.success) {
                    manageLoginResponse(response)
                } else {
                    response.message?.let {
                        loginStateSubject.onNext(LoginViewState.ErrorMessage(it))
                    }
                }
            }, { throwable ->
                loginStateSubject.onNext(LoginViewState.LoadingState(false))
                if (throwable is DeactivatedAccountException) {
                    loginStateSubject.onNext(
                        LoginViewState.DeactivateAccount(
                            throwable.localizedMessage ?: ""
                        )
                    )
                } else {
                    throwable.localizedMessage?.let {
                        loginStateSubject.onNext(LoginViewState.ErrorMessage(it))
                    }
                }
            }).autoDispose()
    }

    private fun manageLoginResponse(response: OutgoerResponse<OutgoerUser>) {
        response.message?.let {
            loginStateSubject.onNext(LoginViewState.SuccessMessage(it))
        }
        if (response.data?.emailVerified == 1) {
            loginStateSubject.onNext(LoginViewState.HomePageNavigation(response.data))
            loginStateSubject.onNext(LoginViewState.SubscribeTopicAfterLogin(response.conversationId ?: arrayListOf()))
        } else {
            response.message?.let {
                loginStateSubject.onNext(LoginViewState.SuccessMessage(it))
            }
            loginStateSubject.onNext(LoginViewState.VerificationNavigation)
        }
    }

    fun checkUsername(username: String) {
        authenticationRepository.checkUsername(ChekUsernameRequest(username))
            .doOnSubscribe {
                loginStateSubject.onNext(LoginViewState.CheckUsernameLoading(true))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                loginStateSubject.onNext(LoginViewState.CheckUsernameLoading(false))
                response.data?.usernameExist?.let {
                    loginStateSubject.onNext(LoginViewState.CheckUsernameExist(it))
                }
            }, { throwable ->
                loginStateSubject.onNext(LoginViewState.CheckUsernameLoading(false))
                throwable.localizedMessage?.let {
                    loginStateSubject.onNext(LoginViewState.ErrorMessage(it))
                }
                Timber.d(throwable)
            }).autoDispose()
    }

    sealed class LoginViewState {
        data class ErrorMessage(val errorMessage: String) : LoginViewState()
        data class SuccessMessage(val successMessage: String) : LoginViewState()
        data class SubscribeTopicAfterLogin(val topicIds: ArrayList<Int>) : LoginViewState()
        data class LoadingState(val isLoading: Boolean) : LoginViewState()
        data class DeactivateAccount(val deactivateMessage: String) : LoginViewState()
        object VerificationNavigation : LoginViewState()
        data class ContinueSocialLogin(val username: String, val emailId: String) : LoginViewState()
        data class AddUsernameEmailDialog(val username: String?, val emailId: String?) : LoginViewState()
        data class HomePageNavigation(val outgoerUser: OutgoerUser) : LoginViewState()

        data class CheckUsernameLoading(val isLoading: Boolean) : LoginViewState()
        data class CheckUsernameExist(val isUsernameExist: Int) : LoginViewState()
    }
}