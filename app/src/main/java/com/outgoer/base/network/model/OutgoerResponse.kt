package com.outgoer.base.network.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.outgoer.api.authentication.model.AwsInformation
import com.outgoer.api.friend_venue.model.VenueDetails
import com.outgoer.api.venue.model.ReviewResponse

@Keep
data class OutgoerResponse<T>(
    @field:SerializedName("access_token")
    val accessToken: String? = null,

    @field:SerializedName("socket_token")
    val socketToken: String? = null,

    @field:SerializedName("data")
    val data: T? = null,

    @field:SerializedName("success")
    val success: Boolean = false,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("total_review")
    val totalReview: Int? = null,

    @field:SerializedName("review_avg")
    val reviewAvg: Double? = null,

    @field:SerializedName("deactive")
    val deactive: Boolean? = null,

    @field:SerializedName("email_verified")
    val emailVerified: Boolean? = null,

    @field:SerializedName("step")
    val step: Int? = null,

    @field:SerializedName("otp")
    val otp: Int? = null,

    @field:SerializedName("token_type")
    val tokenType: String? = null,

    @field:SerializedName("expires_in")
    val expiresIn: Int? = null,

    @field:SerializedName("join_status")
    val joinStatus: Int? = null,

    @field:SerializedName("like_status")
    val likeStatus: Int = 0,

    @field:SerializedName("event_request_status")
    val eventRequestStatus: Int? = null,

    @field:SerializedName("conversation_id")
    val conversationId: ArrayList<Int>? = null,

    @field:SerializedName("review_group")
    val reviewGroup: ArrayList<ReviewResponse>? = null,

    @field:SerializedName("user_review_added")
    val userReviewAdded: Boolean = false,

    @field:SerializedName("venue_details")
    val venueDetails: VenueDetails? = null,
)

@Keep
data class OutgoerCommonResponse(
    @field:SerializedName("success")
    val success: Boolean = false,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("isblock")
    val isblock: Boolean? = null,

    @field:SerializedName("event_request_status")
    val eventRequestStatus: Int? = null,

    @field:SerializedName("conversation_id")
    val conversationId: Int? = null,

    @field:SerializedName("user_tokens")
    val userTokens: ArrayList<String>? = null,

    @field:SerializedName("aws_data")
    val awsData: AwsInformation? = null,
)



@Keep
data class NewOutgoerCommonResponse(
    @field:SerializedName("success")
    val success: Boolean = false,

    @field:SerializedName("message")
    val message: String? = null,

    @field:SerializedName("registrationToken")
    val registrationToken: ArrayList<String>? = null,

    @field:SerializedName("conversation_id")
    val conversationId: ArrayList<Int>? = null,
)