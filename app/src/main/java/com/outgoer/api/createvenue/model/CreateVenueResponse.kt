package com.outgoer.api.createvenue.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.outgoer.api.venue.model.VenueMediaRequest
import kotlinx.android.parcel.Parcelize


@Parcelize
data class Pivot(

	@field:SerializedName("role_id")
	val roleId: Int? = null,

	@field:SerializedName("model_type")
	val modelType: String? = null,

	@field:SerializedName("model_id")
	val modelId: Int? = null
): Parcelable

@Parcelize
data class CreateVenueResponse(
	@SerializedName("id")
	val id: Int,

	@SerializedName("name")
	val name: String? = null,

	@SerializedName("username")
	val username: String? = null,

	@SerializedName("email")
	val email: String? = null,

	@SerializedName("phone_code")
	val phoneCode: String? = null,

	@SerializedName("phone")
	val phone: String? = null,

	@SerializedName("user_type")
	val userType: String? = null,

	@SerializedName("email_verified")
	val emailVerified: Int? = null,

	@SerializedName("avatar")
	val avatar: String? = null,

	@SerializedName("about")
	val about: String? = null,

	@SerializedName("category_id")
	val categoryId: String? = null,

	@SerializedName("venue_address")
	val venueAddress: String? = null,

	@SerializedName("latitude")
	val latitude: String? = null,

	@SerializedName("longitude")
	val longitude: String? = null,

	@SerializedName("platform")
	val platform: String? = null,

	@SerializedName("push_token")
	val pushToken: String? = null,

	@SerializedName("profile_verified")
	val profileVerified: Int? = null,

	@SerializedName("deactive")
	val deactive: Int? = null,

	@SerializedName("total_post")
	val totalPost: Int? = null,

	@SerializedName("total_reels")
	val totalReels: Int? = null,

	@SerializedName("total_followers")
	val totalFollowers: Int? = null,

	@SerializedName("total_following")
	val totalFollowing: Int? = null,

	@SerializedName("created_at")
	val createdAt: String? = null,

	@SerializedName("updated_at")
	val updatedAt: String? = null,

	@SerializedName("deleted_at")
	val deletedAt: String? = null,

	@SerializedName("description")
	val description: String? = null,

	@SerializedName("web_link")
	val webLink: String? = null,

	@SerializedName("web_title")
	val webTitle: String? = null,

	@SerializedName("fb_link")
	val fbLink: String? = null,

	@SerializedName("venue_category")
	var venueCategory: String? = null,

	@SerializedName("roles")
	val roles: List<RolesItem?>? = null,

	@SerializedName("cover_image")
	var coverImage: String? = null,

	@SerializedName("gallery")
	var gallery: ArrayList<VenueMediaRequest>? = arrayListOf(),

): Parcelable

@Parcelize
data class RolesItem(

	@field:SerializedName("updated_at")
	val updatedAt: String? = null,

	@field:SerializedName("name")
	val name: String? = null,

	@field:SerializedName("created_at")
	val createdAt: String? = null,

	@field:SerializedName("pivot")
	val pivot: Pivot? = null,

	@field:SerializedName("id")
	val id: Int? = null,

	@field:SerializedName("guard_name")
	val guardName: String? = null
): Parcelable
