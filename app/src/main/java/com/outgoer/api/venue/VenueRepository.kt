package com.outgoer.api.venue

import com.google.android.gms.maps.model.LatLng
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.event.model.EventListData
import com.outgoer.api.venue.model.*
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import timber.log.Timber

class VenueRepository(
    private val venueRetrofitAPI: VenueRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache,
) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    private var venueMapInfo: List<VenueMapInfo> = listOf()
    private var preLatLon : LatLng? = null

    fun getListOfVenue(): List<VenueMapInfo> {
        return  venueMapInfo.filter { it.userType == "venue_owner" }
    }

    fun getVenueCategoryList(): Single<List<VenueCategory>> {
        return venueRetrofitAPI.getVenueCategoryList()
            .flatMap { outgoerResponseConverter.convertToSingle(it) }
    }


    fun getReviews(getVenueRequest: GetVenueRequest): Single<OutgoerResponse<List<VenueReviewModel>>> {
        return venueRetrofitAPI.getReviews(getVenueRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun addReviews(addReviewRequest: AddReviewRequest): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.addReviews(addReviewRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun broadcastMessage(broadcastMessageRequest: BroadcastMessageRequest): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.broadcastMessage(broadcastMessageRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }
    fun addPhotos(addPhotoRequest: AddPhotoRequest): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.addPhotos(addPhotoRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun getMapVenueByCategoryIn(request: GetMapNearPlacesCategoryRequest, isVenueEmpty: Boolean = false, isCategorySearch: Boolean = false): Single<List<VenueMapInfo>> {

        if(isVenueEmpty) {
            venueMapInfo = listOf()
        }
        Timber.tag("MAP").i("venueMapInfo ${venueMapInfo.isEmpty()}")
        Timber.tag("MAP").i("preLatLon ${preLatLon.toString()}")
        Timber.tag("MAP").i("request ${request.toString()}")

        return venueRetrofitAPI.getMapVenueByCategoryIn(request)
            .doOnSuccess { response ->
                venueMapInfo = response.data ?: listOf()
            }.flatMap { outgoerResponseConverter.convertToSingle(it)  }
    }

    fun getMapVenueByCategory(request: GetMapVenueByCategoryRequest, isVenueEmpty: Boolean = false, isCategorySearch: Boolean = false): Single<List<VenueMapInfo>> {

        if(isVenueEmpty) {
            venueMapInfo = listOf()
        }
        Timber.tag("MAP").i("venueMapInfo ${venueMapInfo.isEmpty()}")
        Timber.tag("MAP").i("preLatLon ${preLatLon.toString()}")
        Timber.tag("MAP").i("request ${request.toString()}")
        Timber.tag("MAP").i("latitude comp ${preLatLon?.latitude == request.latitude.toDoubleOrNull()}")
        Timber.tag("MAP").i("longitude comp ${preLatLon?.longitude == request.longitude.toDoubleOrNull()}")
        return if (isCategorySearch || !request.search.isNullOrEmpty() || venueMapInfo.isEmpty()  || preLatLon == null || !(preLatLon?.latitude == request.latitude.toDoubleOrNull() && preLatLon?.longitude == request.longitude.toDoubleOrNull())) {
            preLatLon = LatLng(request.latitude.toDoubleOrNull() ?: 0.0, request.longitude.toDoubleOrNull() ?: 0.0)
            venueRetrofitAPI.getMapVenueByCategory(request)
                .doOnSuccess { response ->
                    venueMapInfo = response.data ?: listOf()
                }.flatMap { outgoerResponseConverter.convertToSingle(it)  }
        } else {
            Single.just(venueMapInfo)
        }
    }

    fun getVenueList(pageNo: Int, request: GetVenueListRequest): Single<List<VenueListInfo>> {
        return venueRetrofitAPI.getVenueList(pageNo, request)
            .doAfterSuccess {}.flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun getOtherNearVenue(pageNo: Int, request: GetOtherNearVenueRequest): Single<List<VenueMapInfo>> {
        return venueRetrofitAPI.getOtherNearVenue(pageNo, request)
            .flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun getVenueDetail(request: GetVenueDetailRequest): Single<VenueDetail> {
        return venueRetrofitAPI.getVenueDetail(request)
            .flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun getVenueGalleryList(pageNo: Int, request: GetVenueGalleryRequest): Single<List<VenueGalleryItem>> {
        return venueRetrofitAPI.getVenueGalleryList(pageNo, request)
            .flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun getLatestEventsDetail(pageNo: Int, getVenueDetailRequest: GetVenueDetailRequest): Single<List<VenueEventInfo>> {
        return venueRetrofitAPI.getLatestEventsDetail(pageNo, getVenueDetailRequest)
            .flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun addRemoveFavouriteVenue(request: AddRemoveFavouriteVenueRequest): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.addRemoveFavouriteVenue(request).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun getMyFavouriteVenueList(pageNo: Int): Single<List<VenueListInfo>> {
        return venueRetrofitAPI.getMyFavouriteVenueList(pageNo)
            .doAfterSuccess {}.flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun getVenueFollowersList(getVenueFollowersRequest: GetVenueFollowersRequest): Single<List<VenueListInfo>> {
        return venueRetrofitAPI.getVenueFollowersList(getVenueFollowersRequest)
            .doAfterSuccess {}.flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun getVenueGallery(pageNo: Int): Single<List<VenueGalleryItem>> {
        return venueRetrofitAPI.getVenueGallery(pageNo, GetVenueAllGalleryRequest(loggedInUserCache.getUserId()))
            .doAfterSuccess {}.flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun addVenueMedia(request: AddVenueMediaListRequest): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.addVenueMedia(request).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun deleteVenueMedia(request: DeleteVenueGalleryRequest): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.deleteVenueMedia(request).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun createEvent(request: AddUpdateEventRequest): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.createEvent(request).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun updateEvent(eventId: Int, request: AddUpdateEventRequest): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.updateEvent(eventId, request).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun deleteEvent(eventId: Int): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.deleteEvent(eventId).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun requestJoinEvent(eventId: Int): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.requestJoinEvent(RequestJoinEventRequest(eventId)).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun checkInOutVenue(checkInOutRequest: CheckInOutRequest): Single<OutgoerCommonResponse> {
        return venueRetrofitAPI.checkInOutVenue(checkInOutRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun getEventVenueData(getVenueRequest: GetVenueRequest): Single<OutgoerResponse<EventListData>> {
        return venueRetrofitAPI.getEventVenueData(getVenueRequest).flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }
}