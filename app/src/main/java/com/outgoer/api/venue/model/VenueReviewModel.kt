package com.outgoer.api.venue.model

import com.google.gson.annotations.SerializedName

data class VenueReviewModel(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("venue_id")
    val venueId: Int,

    @field:SerializedName("user_id")
    val userId: Int,

    @field:SerializedName("review_text")
    val reviewText: String? = null,

    @field:SerializedName("rating")
    val rating: Double = 0.0,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("images")
    val images: ArrayList<ReviewImage> = arrayListOf(),

    @field:SerializedName("user")
    val user: ReviewUser
)

data class ReviewImage(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("venue_review_id")
    val venueReviewId: Int,

    @field:SerializedName("media")
    val media: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null
)

data class ReviewUser(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("email")
    val email: String? = null,

    @field:SerializedName("avatar")
    val avatar: String? = null
)

data class GetVenueRequest(
    @field:SerializedName("venue_id")
    val venueId: Int
)

data class AddReviewRequest(
    @field:SerializedName("venue_id")
    val venueId: Int,

    @field:SerializedName("review_text")
    val reviewText: String? = null,

    @field:SerializedName("rating")
    val rating: Double = 0.0,

    @field:SerializedName("review_images")
    val reviewImages: ArrayList<String> = arrayListOf(),
)

data class AddPhotoRequest(
    @field:SerializedName("gallery")
    val gallery: ArrayList<String> = arrayListOf(),
)

data class ReviewResponse(
    @field:SerializedName("rating_group")
    val ratingGroup: String? = null,

    @field:SerializedName("rating_count")
    val ratingCount: Int = 0,
)

data class BroadcastMessageRequest(
    @field:SerializedName("broadcast_message")
    val broadcastMessage: String? = null
)