package com.outgoer.api.venue

import com.outgoer.api.event.model.EventListData
import com.outgoer.api.venue.model.*
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.*

interface VenueRetrofitAPI {
    @GET("users/category")
    fun getVenueCategoryList(): Single<OutgoerResponse<List<VenueCategory>>>

    @POST("users/get-review")
    fun getReviews(@Body getVenueRequest: GetVenueRequest): Single<OutgoerResponse<List<VenueReviewModel>>>

    @POST("users/add-review")
    fun addReviews(@Body addReviewRequest: AddReviewRequest): Single<OutgoerCommonResponse>

    @POST("users/gallery/create")
    fun addPhotos(@Body addPhotoRequest: AddPhotoRequest): Single<OutgoerCommonResponse>

    @POST("broadcast/message")
    fun broadcastMessage(@Body broadcastMessageRequest: BroadcastMessageRequest): Single<OutgoerCommonResponse>

    @POST("users/nearby_venue")
    fun getMapVenueByCategory(
        @Body request: GetMapVenueByCategoryRequest
    ): Single<OutgoerResponse<List<VenueMapInfo>>>

    @POST("users/near_map_places")
    fun getMapVenueByCategoryIn(
        @Body request: GetMapNearPlacesCategoryRequest
    ): Single<OutgoerResponse<List<VenueMapInfo>>>

    @POST("users/list_nearby_venue")
    fun getVenueList(
        @Query("page") pageNo: Int,
        @Body request: GetVenueListRequest
    ): Single<OutgoerResponse<List<VenueListInfo>>>

    @POST("users/other_near_places")
    fun getOtherNearVenue(
        @Query("page") pageNo: Int,
        @Body request: GetOtherNearVenueRequest
    ): Single<OutgoerResponse<List<VenueMapInfo>>>

    @POST("users/venue")
    fun getVenueDetail(
        @Body request: GetVenueDetailRequest
    ): Single<OutgoerResponse<VenueDetail>>

    @POST("users/venue_gallery")
    fun getVenueGalleryList(
        @Query("page") pageNo: Int,
        @Body request: GetVenueGalleryRequest
    ): Single<OutgoerResponse<List<VenueGalleryItem>>>

    @POST("users/venue_event")
    fun getLatestEventsDetail(
        @Query("page") pageNo: Int,
        @Body request: GetVenueDetailRequest
    ): Single<OutgoerResponse<List<VenueEventInfo>>>

    @POST("users/add-remove-favourite-venue")
    fun addRemoveFavouriteVenue(
        @Body request: AddRemoveFavouriteVenueRequest
    ): Single<OutgoerCommonResponse>

    @GET("users/all-favourite-venue")
    fun getMyFavouriteVenueList(
        @Query("page") pageNo: Int
    ): Single<OutgoerResponse<List<VenueListInfo>>>

    @POST("get_venue_followers")
    fun getVenueFollowersList(
        @Body getVenueFollowersRequest: GetVenueFollowersRequest
    ): Single<OutgoerResponse<List<VenueListInfo>>>

    @POST("users/venue_all_gallery")
    fun getVenueGallery(
        @Query("page") pageNo: Int,
        @Body request: GetVenueAllGalleryRequest
    ): Single<OutgoerResponse<List<VenueGalleryItem>>>

    @POST("users/gallery/create")
    fun addVenueMedia(
        @Body request: AddVenueMediaListRequest
    ): Single<OutgoerCommonResponse>

    @POST("users/gallery/delete")
    fun deleteVenueMedia(
        @Body request: DeleteVenueGalleryRequest
    ): Single<OutgoerCommonResponse>

    @POST("events/create")
    fun createEvent(
        @Body request: AddUpdateEventRequest
    ): Single<OutgoerCommonResponse>

    @POST("events/update/{eventId}")
    fun updateEvent(
        @Path("eventId") eventId: Int,
        @Body request: AddUpdateEventRequest
    ): Single<OutgoerCommonResponse>

    @DELETE("events/delete/{eventId}")
    fun deleteEvent(
        @Path("eventId") eventId: Int,
    ): Single<OutgoerCommonResponse>

    @POST("events/add-remove-event-request")
    fun requestJoinEvent(
        @Body request: RequestJoinEventRequest
    ): Single<OutgoerCommonResponse>


    @POST("check_in_out")
    fun checkInOutVenue(
        @Body checkInOutRequest: CheckInOutRequest
    ): Single<OutgoerCommonResponse>


    @POST("events/venue")
    fun getEventVenueData(
        @Body getVenueRequest: GetVenueRequest
    ): Single<OutgoerResponse<EventListData>>

}