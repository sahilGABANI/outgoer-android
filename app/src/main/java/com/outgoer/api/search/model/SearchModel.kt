package com.outgoer.api.search.model

import com.google.gson.annotations.SerializedName

data class SearchTopPostReelRequest(
    @field:SerializedName("search")
    val search: String = "",
)

data class SearchAccountRequest(
    @field:SerializedName("search")
    val search: String = "",
)

data class SearchPlacesRequest(
    @field:SerializedName("search")
    val search: String = "",
)