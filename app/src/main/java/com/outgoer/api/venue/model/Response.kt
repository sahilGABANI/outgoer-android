package com.outgoer.api.venue.model

import com.google.gson.annotations.SerializedName

data class Response(

	@field:SerializedName("nextpage")
	val nextpage: Int? = null,

	@field:SerializedName("data")
	val data: List<DataItem?>? = null,

	@field:SerializedName("success")
	val success: Boolean? = null,

	@field:SerializedName("message")
	val message: String? = null
)

data class DataItem(

	@field:SerializedName("profile_verified")
	val profileVerified: Int? = null,

	@field:SerializedName("distance")
	val distance: Any? = null,

	@field:SerializedName("latitude")
	val latitude: String? = null,

	@field:SerializedName("about")
	val about: Any? = null,

	@field:SerializedName("follow_status")
	val followStatus: Int? = null,

	@field:SerializedName("total_review")
	val totalReview: Int? = null,

	@field:SerializedName("avatar")
	val avatar: String? = null,

	@field:SerializedName("review_avg")
	val reviewAvg: Int? = null,

	@field:SerializedName("following_status")
	val followingStatus: Int? = null,

	@field:SerializedName("venue_favourite_status")
	val venueFavouriteStatus: Int? = null,

	@field:SerializedName("at_venue_count")
	val atVenueCount: Int? = null,

	@field:SerializedName("user_type")
	val userType: String? = null,

	@field:SerializedName("venue_address")
	val venueAddress: String? = null,

	@field:SerializedName("category_id")
	val categoryId: String? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("category")
	val category: Any? = null,

	@field:SerializedName("username")
	val username: String? = null,

	@field:SerializedName("longitude")
	val longitude: String? = null
)
