package com.outgoer.api.event_category

import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface EventCategoryRetrofitAPI {

    @GET("users/event-category")
    fun getEventCategory(@Query("search") search: String? = null): Single<OutgoerResponse<ArrayList<VenueCategory>>>
}