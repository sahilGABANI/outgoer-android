package com.outgoer.api.createvenue

import com.outgoer.api.createvenue.model.CreateVenueResponse
import com.outgoer.api.venue.model.*
import com.outgoer.base.network.model.NewOutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.Path

interface CreateVenueRetrofitAPI {

    @POST("users/venue")
    fun getVenueDetail(
        @Body request: GetVenueDetailRequest
    ): Single<OutgoerResponse<VenueDetail>>


    @POST("auth/signup_venue")
    fun createVenue(@Body registerVenueRequest: RegisterVenueRequest): Single<OutgoerResponse<CreateVenueResponse>>

    @POST("users/update-venue-profile")
    fun updateVenue(@Body registerVenueRequest: RegisterVenueRequest): Single<OutgoerResponse<CreateVenueResponse>>

    @POST("users/gallery/create")
    fun updateVenuePhotos(@Body addVenueGalleryRequest: AddVenueGalleryRequest): Single<OutgoerCommonResponse>

    @POST("users/venue_gallery")
    fun getVenueGallery(@Body getVenueDetailRequest: GetVenueDetailRequest): Single<OutgoerResponse<ArrayList<VenueGalleryItem>>>

    @DELETE("users/gallery/delete/{venue_id}")
    fun getVenueGallery(@Path("venue_id") venueId: Int): Single<OutgoerCommonResponse>

    @POST("auth/check_venue")
    fun checkVenue(@Body registerVenueRequest: RegisterVenueRequest): Single<NewOutgoerCommonResponse>
}