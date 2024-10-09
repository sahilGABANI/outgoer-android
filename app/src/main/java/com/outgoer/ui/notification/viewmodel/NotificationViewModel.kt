package com.outgoer.ui.notification.viewmodel

import com.outgoer.api.notification.NotificationRepository
import com.outgoer.api.notification.model.NotificationInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class NotificationViewModel(
    private val notificationRepository: NotificationRepository
) : BaseViewModel() {

    private val notificationStateSubject: PublishSubject<NotificationViewState> = PublishSubject.create()
    val notificationState: Observable<NotificationViewState> = notificationStateSubject.hide()

    //-------------------Get All Notification Pagination-------------------
    private var listOfPostData: MutableList<NotificationInfo> = mutableListOf()
    private var pageNumber: Int = 1
    private var isLoadMore: Boolean = true
    private var isLoading: Boolean = false

    fun pullToRefresh(isReload: Boolean) {
        pageNumber = 1
        isLoadMore = true
        isLoading = false
        listOfPostData.clear()
        getAllNotification(isReload)
    }

    private fun getAllNotification(isReload: Boolean) {
        notificationRepository.getAllNotification(pageNumber)
            .doOnSubscribe {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(isReload))
            }
            .doAfterTerminate {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    if (pageNumber == 1) {
                        listOfPostData = it.toMutableList()
                        notificationStateSubject.onNext(NotificationViewState.GetAllNotificationInfo(listOfPostData))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfPostData.addAll(it)
                            notificationStateSubject.onNext(NotificationViewState.GetAllNotificationInfo(listOfPostData))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                throwable.printStackTrace()
                throwable.localizedMessage?.let {
                    Timber.e(it)
                }
            }).autoDispose()
    }


    fun loadMore() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNumber++
                getAllNotification(false)
            }
        }
    }
    //-------------------Get All Notification Pagination-------------------


    private var listOfNotification: MutableList<NotificationInfo> = mutableListOf()
    private var pageNumberN: Int = 1
    private var isLoadMoreN: Boolean = true
    private var isLoadingN: Boolean = false

    fun pullToRefreshN() {
        pageNumberN = 1
        isLoadMoreN = true
        isLoadingN = false
        listOfNotification.clear()
        getAdminNotification()
    }

    private fun getAdminNotification() {
        notificationRepository.getAdminNotification(pageNumberN)
            .doOnSubscribe {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(true))
            }
            .doAfterTerminate {
                notificationStateSubject.onNext(NotificationViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.data?.let {
                    if (pageNumberN == 1) {
                        listOfNotification = it.toMutableList()
                        notificationStateSubject.onNext(NotificationViewState.GetAllNotificationInfo(listOfNotification))
                        isLoadingN = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfNotification.addAll(it)
                            notificationStateSubject.onNext(NotificationViewState.GetAllNotificationInfo(listOfNotification))
                            isLoadingN = false
                        } else {
                            isLoadMoreN = false
                        }
                    }
                }
            }, { throwable ->
                throwable.printStackTrace()
                throwable.localizedMessage?.let {
                    Timber.e(it)
                }
            }).autoDispose()
    }


    fun loadMoreN() {
        if (!isLoadingN) {
            isLoadingN = true
            if (isLoadMoreN) {
                pageNumberN++
                getAdminNotification()
            }
        }
    }


    fun updateNotificationReadStatus(notificationId: Int) {
        notificationRepository.updateNotificationReadStatus(notificationId)
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response.data?.let {
//                    RxBus.publish(RxEvent.UpdateNotificationBadge(it.notificationStatus))
                }
            }, { throwable ->
                throwable.printStackTrace()
                throwable.localizedMessage?.let {
                    Timber.e(it)
                }
            }).autoDispose()
    }

}

sealed class NotificationViewState {
    data class ErrorMessage(val errorMessage: String) : NotificationViewState()
    data class SuccessMessage(val successMessage: String) : NotificationViewState()
    data class LoadingState(val isLoading: Boolean) : NotificationViewState()
    data class GetAllNotificationInfo(val notificationInfoList: List<NotificationInfo>) : NotificationViewState()
}