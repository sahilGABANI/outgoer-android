package com.outgoer.api.event_category

import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.base.network.OutgoerResponseConverter
import io.reactivex.Single

class EventCategoryRepository(private val eventCategoryRetrofitAPI: EventCategoryRetrofitAPI) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun getEventCategory(): Single<ArrayList<VenueCategory>> {
        return eventCategoryRetrofitAPI.getEventCategory().flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }
}