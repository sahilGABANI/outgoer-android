package com.outgoer.ui.activateaccount.viewmodel

import com.outgoer.api.authentication.AuthenticationRepository
import com.outgoer.api.authentication.model.AccountActivationRequest
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ActivateAccountViewModel(
    private val authenticationRepository: AuthenticationRepository
) : BaseViewModel() {

    private val activateAccountStateSubject: PublishSubject<ActivateAccountViewState> = PublishSubject.create()
    val activateAccountState: Observable<ActivateAccountViewState> = activateAccountStateSubject.hide()

    fun activateAccount(request: AccountActivationRequest) {
        authenticationRepository.activateAccount(request)
            .doOnSubscribe {
                activateAccountStateSubject.onNext(ActivateAccountViewState.LoadingState(true))
            }
            .doAfterTerminate {
                activateAccountStateSubject.onNext(ActivateAccountViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.message?.let {
                    activateAccountStateSubject.onNext(ActivateAccountViewState.SuccessMessage(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    activateAccountStateSubject.onNext(ActivateAccountViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class ActivateAccountViewState {
        data class ErrorMessage(val errorMessage: String) : ActivateAccountViewState()
        data class SuccessMessage(val successMessage: String) : ActivateAccountViewState()
        data class LoadingState(val isLoading: Boolean) : ActivateAccountViewState()
    }
}