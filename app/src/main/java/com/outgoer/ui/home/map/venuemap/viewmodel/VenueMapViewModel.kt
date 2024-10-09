package com.outgoer.ui.home.map.venuemap.viewmodel

import com.outgoer.api.venue.VenueRepository
import com.outgoer.api.venue.model.*
import com.outgoer.base.BaseViewModel
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class MapVenueViewModel(
    private val venueRepository: VenueRepository
) : BaseViewModel() {

    private val venueCategoryStateSubject: PublishSubject<VenueCategoryViewState> = PublishSubject.create()
    val venueCategoryState: Observable<VenueCategoryViewState> = venueCategoryStateSubject.hide()

    fun getListOfVenue(): List<VenueMapInfo> {
        return venueRepository.getListOfVenue()
    }

    fun getVenueCategoryList() {
        venueRepository.getVenueCategoryList()
            .doOnSubscribe {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.VenueCategoryList(it))
                }
            }, { throwable ->
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getMapVenueByCategoryInfo(categoryId: Int, locationInfo: ArrayList<Double>, search: String? = null, isVenueEmpty: Boolean = false, isCategorySearch: Boolean = false) {
        val request = GetMapNearPlacesCategoryRequest(
            categoryId = categoryId,
            value = locationInfo,
            search = search
        )
        venueRepository.getMapVenueByCategoryIn(request, isVenueEmpty, isCategorySearch)
            .doOnSubscribe {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.VenueMapInfoListByCategory(it))
                }
            }, { throwable ->
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getMapPeopleByCategoryIn(categoryId: Int, locationInfo: ArrayList<Double>, search: String? = null, isVenueEmpty: Boolean = false) {
        val request = GetMapNearPlacesCategoryRequest(
            categoryId = categoryId,
            value = locationInfo,
            search = search
        )
        venueRepository.getMapVenueByCategoryIn(request, isVenueEmpty)
            .doOnSubscribe {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
//                    venueCategoryStateSubject.onNext(VenueCategoryViewState.VenueMapInfoListByCategory(it.filter { !it.userType.equals("venue_owner") }))
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.FriendsVenueInfoList(it.filter { it.userType.equals("user") }))
                }
            }, { throwable ->
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }


    fun getMapVenueByCategory(categoryId: Int, latitude: Double, longitude: Double, search: String? = null, isVenueEmpty: Boolean = false, isCategorySearch: Boolean = false) {
        val request = GetMapVenueByCategoryRequest(
            categoryId = categoryId,
            latitude = latitude.toString(),
            longitude = longitude.toString(),
            search = search
        )
        venueRepository.getMapVenueByCategory(request, isVenueEmpty, isCategorySearch)
            .doOnSubscribe {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.VenueMapInfoListByCategory(it.filter { it.userType.equals("venue_owner") }))
                }
            }, { throwable ->
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    fun getMapPeopleByCategory(categoryId: Int?, latitude: Double?, longitude: Double?,search: String?, isVenueEmpty: Boolean = false) {
        val request = GetMapVenueByCategoryRequest(
            categoryId = categoryId,
            latitude = latitude.toString(),
            longitude = longitude.toString(),
            search = search
        )
        venueRepository.getMapVenueByCategory(request, isVenueEmpty)
            .doOnSubscribe {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.VenueMapInfoListByCategory(it.filter { !it.userType.equals("venue_owner") }))
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.FriendsVenueInfoList(it.filter { it.userType.equals("user") }))
                }
            }, { throwable ->
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private var listOfVenueMapInfo: MutableList<VenueMapInfo> = mutableListOf()
    private var pageNo = 1
    private var isLoading = false
    private var isLoadMore = true

    fun pullToRefresh(categoryId: Int?, search: String?) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getOtherNearVenue(categoryId, search)
    }

    fun pullToRefreshFriends(categoryId: Int?, latitude: Double?, longitude: Double?,search: String?) {
        pageNo = 1
        isLoading = false
        isLoadMore = true
        getMapPeopleByCategory(categoryId, latitude,longitude , search)
    }

    fun loadMoreOtherNearFriends(categoryId: Int?, latitude: Double?, longitude: Double?,search: String?) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getMapPeopleByCategory(categoryId, latitude,longitude , search)
            }
        }
    }
    fun loadMoreOtherNearVenue(categoryId: Int?, search: String?) {
        if (!isLoading) {
            isLoading = true
            if (isLoadMore) {
                pageNo += 1
                getOtherNearVenue(categoryId, search)
            }
        }
    }

    private fun getOtherNearVenue(categoryId: Int?, search: String?) {
        val request = GetOtherNearVenueRequest(
            categoryId = categoryId,
            venueId = null,
            search = search
        )

        venueRepository.getOtherNearVenue(pageNo, request)
            .doOnSubscribe {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (pageNo == 1) {
                        listOfVenueMapInfo = it.toMutableList()
                        venueCategoryStateSubject.onNext(VenueCategoryViewState.OtherNearVenueInfoList(
                            listOfVenueMapInfo as ArrayList<VenueMapInfo>
                        ))
                        isLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfVenueMapInfo.addAll(it)
                            venueCategoryStateSubject.onNext(VenueCategoryViewState.OtherNearVenueInfoList(
                                listOfVenueMapInfo as ArrayList<VenueMapInfo>
                            ))
                            isLoading = false
                        } else {
                            isLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.ErrorMessage(it))
                }
            }).autoDispose()
    }

    private var listOfVenueInfo: MutableList<VenueListInfo> = mutableListOf()
    private var vsearchText = ""
    private var vpageNo = 1
    private var visLoading = false
    private var visLoadMore = true

    private fun getVenueList(latitude: Double, longitude: Double) {
        val request = GetVenueListRequest(
            search = vsearchText,
            latitude = latitude.toString(),
            longitude = longitude.toString()
        )
        venueRepository.getVenueList(vpageNo, request)
            .doOnSubscribe {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(true))
            }
            .doAfterTerminate {
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
            }
            .subscribeOnIoAndObserveOnMainThread({ response ->
                response?.let {
                    if (vpageNo == 1) {
                        listOfVenueInfo = response.toMutableList()
                        venueCategoryStateSubject.onNext(VenueCategoryViewState.VenueInfoList(
                            listOfVenueInfo as ArrayList<VenueListInfo>
                        ))
                        visLoading = false
                    } else {
                        if (!it.isNullOrEmpty()) {
                            listOfVenueInfo.addAll(it)
                            venueCategoryStateSubject.onNext(VenueCategoryViewState.VenueInfoList(
                                listOfVenueInfo as ArrayList<VenueListInfo>
                            ))
                            visLoading = false
                        } else {
                            visLoadMore = false
                        }
                    }
                }
            }, { throwable ->
                venueCategoryStateSubject.onNext(VenueCategoryViewState.LoadingState(false))
                throwable.localizedMessage?.let {
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.ErrorMessage(it))
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

    fun checkInOutVenue(checkInOutRequest: CheckInOutRequest) {
        venueRepository.checkInOutVenue(checkInOutRequest)
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({
                it.message?.let { message ->
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.SuccessMessage(message))
                    RxBus.publish(RxEvent.RefreshVenueList)
                }
            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }

    fun broadcastMessage(broadcastMessageRequest: BroadcastMessageRequest) {
        venueRepository.broadcastMessage(broadcastMessageRequest)
            .doOnSubscribe {}
            .subscribeOnIoAndObserveOnMainThread({
                it.message?.let { message ->
                    venueCategoryStateSubject.onNext(VenueCategoryViewState.SuccessMessage(message))
                }
            }, { throwable ->
                Timber.e(throwable)
            }).autoDispose()
    }
}

sealed class VenueCategoryViewState {
    data class ErrorMessage(val errorMessage: String) : VenueCategoryViewState()
    data class SuccessMessage(val successMessage: String) : VenueCategoryViewState()
    data class LoadingState(val isLoading: Boolean) : VenueCategoryViewState()
    data class VenueCategoryList(val venueCategoryList: List<VenueCategory>) : VenueCategoryViewState()
    data class VenueMapInfoListByCategory(val venueMapInfoList: List<VenueMapInfo>) : VenueCategoryViewState()
    data class OtherNearVenueInfoList(val listOfVenueMapInfo: ArrayList<VenueMapInfo>) : VenueCategoryViewState()
    data class FriendsVenueInfoList(val listOfFriendMapInfo: List<VenueMapInfo>) : VenueCategoryViewState()
    data class VenueInfoList(val listOfVenueInfo: ArrayList<VenueListInfo>) : VenueCategoryViewState()

}