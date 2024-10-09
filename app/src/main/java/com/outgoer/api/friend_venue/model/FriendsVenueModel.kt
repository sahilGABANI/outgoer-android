package com.outgoer.api.friend_venue.model

import com.google.gson.annotations.SerializedName


data class CheckInVenueResponse(
    @SerializedName("venue_id")
    val venueId: Int
)

data class UserVenueResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("avatar")
    val avatar: String,
)


data class VenueDetails(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("avatar")
    val avatar: String,

    @SerializedName("latitude")
    val latitude: String,

    @SerializedName("longitude")
    val longitude: String,

    @SerializedName("distance")
    val distance: Double,

    @SerializedName("review_avg")
    val reviewAvg: Double,

    @SerializedName("at_venue_count")
    val atVenueCount: Int,
)