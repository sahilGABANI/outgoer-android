package com.outgoer.ui.home.profile.viewmodel

import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.AddRemoveFavouriteVenueRequest
import com.outgoer.api.venue.model.GetVenueFollowersRequest
import com.outgoer.api.venue.model.VenueListInfo
import com.outgoer.base.BaseViewModel
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class MyFavouriteVenueViewModel(
    private val venueRepository: VenueRepository
) : BaseViewModel() {

    private val myFavouriteVenueListStateSubject: PublishSubject<MyFavouriteVenueViewState> = PublishSubject.create()
    val myFavouriteVenueListState: Observable<MyFavouriteVenueViewState> = myFavouriteVenueListStateSubject.hide()

    private var listOfVenueInfo: MutableList<VenueListInfo> = mutableListOf()
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    fun resetMyFavouriteVenuePagination() {
        listOfVenueInfo.clear()
        pageNo = 1
        isLoadMore = true
        isLoading = false
        getVenueList()
    }

    fun loadMoreVenueList() {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getVenueList()
            }
        }
    }

    private fun getVenueList() {
        venueRepository.getMyFavouriteVenueList(pageNo)
            .doOnSubscribe {
                myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.LoadingState(true))
            }
            .doAfterTerminate {
                myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNo == 1) {
                        listOfVenueInfo = response.toMutableList()
                        myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.MyFavouriteVenueInfoList(listOfVenueInfo))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfVenueInfo.addAll(it)
                            myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.MyFavouriteVenueInfoList(listOfVenueInfo))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun addRemoveFavouriteVenue(venueId: Int) {
        venueRepository.addRemoveFavouriteVenue(AddRemoveFavouriteVenueRequest(venueId))
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({

            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }

    fun getVenueFollowersList(getVenueFollowersRequest: GetVenueFollowersRequest) {
        venueRepository.getVenueFollowersList(getVenueFollowersRequest)
            .doOnSubscribe {
                myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.LoadingState(true))
            }
            .doAfterTerminate {
                myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.MyFavouriteVenueInfoList(it))
                }
            }, { throwable ->
                myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    myFavouriteVenueListStateSubject.onNext(MyFavouriteVenueViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    sealed class MyFavouriteVenueViewState {
        data class ErrorMessage(val errorMessage: String) : MyFavouriteVenueViewState()
        data class SuccessMessage(val successMessage: String) : MyFavouriteVenueViewState()
        data class LoadingState(val isLoading: Boolean) : MyFavouriteVenueViewState()
        data class MyFavouriteVenueInfoList(val listOfVenueInfo: List<VenueListInfo>) : MyFavouriteVenueViewState()
    }
}