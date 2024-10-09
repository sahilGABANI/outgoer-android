package com.outgoer.api.friend_venue

import com.outgoer.api.friend_venue.model.CheckInVenueResponse
import com.outgoer.api.friend_venue.model.UserVenueResponse
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class FriendsVenueRepository(private val friendsVenueRetrofitAPI: FriendsVenueRetrofitAPI) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun checkInFriendsVenue(checkInVenueResponse: CheckInVenueResponse): Single<OutgoerResponse<ArrayList<UserVenueResponse>>> {
        return friendsVenueRetrofitAPI.checkInFriendsVenue(checkInVenueResponse).flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

}