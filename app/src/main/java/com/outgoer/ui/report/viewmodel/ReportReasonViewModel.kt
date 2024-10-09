package com.outgoer.ui.report.viewmodel

import com.outgoer.api.post.PostRepository
import com.outgoer.api.post.model.ReportReason
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class ReportReasonViewModel(private val postRepository: PostRepository) : BaseViewModel() {

    private val reportReasonStateSubject: PublishSubject<ReportReasonState> =
        PublishSubject.create()
    val reportReasonState: Observable<ReportReasonState> = reportReasonStateSubject.hide()

    fun getReportReason() {
        postRepository.getReportReason()
            .doOnSubscribe {
                reportReasonStateSubject.onNext(ReportReasonState.LoadingState(true))
            }.doAfterTerminate {
                reportReasonStateSubject.onNext(ReportReasonState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    reportReasonStateSubject.onNext(ReportReasonState.GetReason(it))
                }
            }, { throwable ->
                throwable.localizedMessage?.let {
                    reportReasonStateSubject.onNext(ReportReasonState.ErrorMessage(it))
                }
            }).autoDispose()
    }


}

sealed class ReportReasonState {
    data class ErrorMessage(val errorMessage: String) : ReportReasonState()
    data class SuccessMessage(val successMessage: String) : ReportReasonState()
    data class LoadingState(val isLoading: Boolean) : ReportReasonState()
    data class GetReason(val data: List<ReportReason>) : ReportReasonState()
}