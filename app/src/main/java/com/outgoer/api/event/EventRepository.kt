package com.outgoer.api.event

import com.outgoer.api.event.model.*
import com.outgoer.api.profile.model.ReportEventRequest
import com.outgoer.api.venue.model.RequestSearchVenue
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class EventRepository(private val eventRetrofitAPI: EventRetrofitAPI) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun getNearGooglePlaces(search: String?= null): Single<OutgoerResponse<ArrayList<GooglePlaces>>> {
        return eventRetrofitAPI.getNearGooglePlaces(search).flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun getEventsList(search: String? = null, categoryId:Int?=null): Single<EventListData> {
        return eventRetrofitAPI.getEventsList(search , categoryId).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun getEventsDetails(eventId: Int): Single<EventData> {
        return eventRetrofitAPI.getEventsDetails(eventId).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun getEventCategoryList(): Single<OutgoerResponse<List<VenueCategory>>> {
        return eventRetrofitAPI.getEventCategoryList().flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun getEventsList(page: Int): Single<ArrayList<VenueMapInfo>> {
        return eventRetrofitAPI.getNearVenueList(page).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun createEvents(createEventResponse: CreateEventResponse): Single<EventListData> {
        return eventRetrofitAPI.createEvents(createEventResponse).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun addRemoveEventRequest(joinRequest: JoinRequest): Single<JoinRequestResponse> {
        return eventRetrofitAPI.addRemoveEventRequest(joinRequest).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun acceptRejectRequest(requestId: Int, joinRequest: RequestResult): Single<OutgoerCommonResponse> {
        return eventRetrofitAPI.acceptRejectRequest(requestId, joinRequest).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun joinRequestList(requestList: RequestList): Single<ArrayList<RequestResponseList>> {
        return eventRetrofitAPI.joinRequestList(requestList).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun deleteJoinRequestList(deletetId: Int): Single<OutgoerCommonResponse> {
        return eventRetrofitAPI.deleteJoinRequestList(deletetId).flatMap {
            outgoerResponseConverter.convertCommonResponse(it)
        }
    }

    fun searchEventVenue(pageNo: Int, request: RequestSearchVenue): Single<List<VenueMapInfo>> {
        return eventRetrofitAPI.searchEventVenue(pageNo, request)
            .doAfterSuccess {}.flatMap { outgoerResponseConverter.convertToSingle(it) }
    }

    fun reportEvent(reportEventRequest: ReportEventRequest): Single<OutgoerCommonResponse> {
        return eventRetrofitAPI.reportEvent(reportEventRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }
}