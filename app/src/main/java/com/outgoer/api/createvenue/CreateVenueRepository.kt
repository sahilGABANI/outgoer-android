package com.outgoer.api.createvenue

import com.outgoer.api.createvenue.model.CreateVenueResponse
import com.outgoer.api.venue.model.*
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.NewOutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class CreateVenueRepository(private val createVenueRetrofitAPI: CreateVenueRetrofitAPI) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun getVenueDetail(request: GetVenueDetailRequest): Single<VenueDetail> {
        return createVenueRetrofitAPI.getVenueDetail(request)
            .flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun createVenue(registerVenueRequest: RegisterVenueRequest): Single<OutgoerResponse<CreateVenueResponse>> {
        return createVenueRetrofitAPI.createVenue(registerVenueRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun updateVenue(registerVenueRequest: RegisterVenueRequest): Single<OutgoerResponse<CreateVenueResponse>> {
        return createVenueRetrofitAPI.updateVenue(registerVenueRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun checkVenue(registerVenueRequest: RegisterVenueRequest): Single<NewOutgoerCommonResponse> {
        return createVenueRetrofitAPI.checkVenue(registerVenueRequest)
            .flatMap { outgoerResponseConverter.convertNewCommonResponse(it) }
    }

    fun updateVenuePhotos(addVenueGalleryRequest: AddVenueGalleryRequest): Single<OutgoerCommonResponse> {
        return createVenueRetrofitAPI.updateVenuePhotos(addVenueGalleryRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun getVenueGallery(getVenueDetailRequest: GetVenueDetailRequest): Single<OutgoerResponse<ArrayList<VenueGalleryItem>>> {
        return createVenueRetrofitAPI.getVenueGallery(getVenueDetailRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun removeVenuePhotoFromGallery(venueId: Int): Single<OutgoerCommonResponse> {
        return createVenueRetrofitAPI.getVenueGallery(venueId)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

}