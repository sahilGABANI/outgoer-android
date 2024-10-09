package com.outgoer.api.hashtag.model

import com.google.gson.annotations.SerializedName

data class HashtagResponse(
    @SerializedName("id")
    val id: Int,

    @SerializedName("user_id")
    val userId: Int? = null,

    @SerializedName("title")
    val title: String? = null,

    @SerializedName("total_reels")
    val totalReels: Int? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("deleted_at")
    val deletedAt: String? = null
)