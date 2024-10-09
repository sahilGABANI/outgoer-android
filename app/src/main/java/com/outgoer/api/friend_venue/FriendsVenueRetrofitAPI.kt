package com.outgoer.api.friend_venue

import com.outgoer.api.friend_venue.model.CheckInVenueResponse
import com.outgoer.api.friend_venue.model.UserVenueResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST

interface FriendsVenueRetrofitAPI {

    @POST("users/checkin_venue")
    fun checkInFriendsVenue(@Body checkInVenueResponse: CheckInVenueResponse): Single<OutgoerResponse<ArrayList<UserVenueResponse>>>
}