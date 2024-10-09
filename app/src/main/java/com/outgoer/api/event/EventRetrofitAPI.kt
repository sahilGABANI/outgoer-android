package com.outgoer.api.event

import com.outgoer.api.event.model.*
import com.outgoer.api.profile.model.ReportEventRequest
import com.outgoer.api.venue.model.RequestSearchVenue
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.*

interface EventRetrofitAPI {

    @GET("events")
    fun getEventsList(@Query("search") search: String? = null , @Query("category_id") categoryId: Int?=null): Single<OutgoerResponse<EventListData>>

    @POST("events/view")
    fun getEventsDetails(@Query("event_id") eventId: Int): Single<OutgoerResponse<EventData>>

    @GET("users/event-category")
    fun getEventCategoryList() : Single<OutgoerResponse<List<VenueCategory>>>

    @POST("events/create")
    fun createEvents(@Body createEventResponse: CreateEventResponse): Single<OutgoerResponse<EventListData>>

    @POST("events/add-remove-event-request")
    fun addRemoveEventRequest(@Body joinRequest: JoinRequest): Single<OutgoerResponse<JoinRequestResponse>>

    @POST("events/update_request/{requestId}")
    fun acceptRejectRequest(
        @Path("requestId") requestId: Int,
        @Body joinRequest: RequestResult
    ): Single<OutgoerCommonResponse>

    @POST("events/request")
    fun joinRequestList(@Body requestList: RequestList): Single<OutgoerResponse<ArrayList<RequestResponseList>>>

    @DELETE("events/delete/{deletetId}")
    fun deleteJoinRequestList(@Path("deletetId") deletetId: Int): Single<OutgoerCommonResponse>

    @GET("users/near_venue_places")
    fun getNearVenueList(@Query("page") page: Int): Single<OutgoerResponse<ArrayList<VenueMapInfo>>>

    @POST("users/search-places")
    fun searchEventVenue(@Query("page") page: Int, @Body request: RequestSearchVenue): Single<OutgoerResponse<List<VenueMapInfo>>>

    @POST("users/near_google_places")
    fun getNearGooglePlaces(@Query("search") search: String?): Single<OutgoerResponse<ArrayList<GooglePlaces>>>

    @POST("report/event")
    fun reportEvent(@Body reportEventRequest: ReportEventRequest): Single<OutgoerCommonResponse>
}