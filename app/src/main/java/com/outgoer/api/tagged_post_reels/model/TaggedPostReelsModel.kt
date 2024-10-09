package com.outgoer.api.tagged_post_reels.model

import com.google.gson.annotations.SerializedName

data class TaggedPostReelsRequest(
    @field:SerializedName("tab_type")
    var tabType: String? = null,

    @field:SerializedName("venue_id")
    var venueId: Int? = null,
)


data class TaggedPostReelsViewRequest(
    @field:SerializedName("view_type")
    var viewType: String? = null,

    @field:SerializedName("venue_id")
    var venueId: Int? = null,
)


data class TaggedPostReelsViewResponse(
    @field:SerializedName("venue_id")
    var venueId: Int? = null,

    @field:SerializedName("user_id")
    var userId: Int? = null,

    @field:SerializedName("view_type")
    var viewType: String? = null,

    @field:SerializedName("is_view")
    var isView: Int? = null,

    @field:SerializedName("updated_at")
    var updatedAt: String? = null,

    @field:SerializedName("created_at")
    var createdAt: String? = null,

    @field:SerializedName("id")
    var id: Int? = null
)