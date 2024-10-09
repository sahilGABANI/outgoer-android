package com.outgoer.api.tagged_post_reels

import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsRequest
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsViewRequest
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsViewResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TaggedPostReelsRetrofitAPI {

    @POST("users/venue_post_reel_tags")
    fun getVenueTaggedReel(
        @Query("page") pageNo: Int,
        @Body taggedPostReelsRequest: TaggedPostReelsRequest
    ): Single<OutgoerResponse<List<ReelInfo>>>

    @POST("users/venue_post_reel_tags")
    fun getVenueTaggedPost(
        @Query("page") pageNo: Int,
        @Body taggedPostReelsRequest: TaggedPostReelsRequest
    ): Single<OutgoerResponse<List<PostInfo>>>

    @POST("users/venue_post_reel_tags")
    fun getVenueTaggedSponty(
        @Query("page") pageNo: Int,
        @Body taggedPostReelsRequest: TaggedPostReelsRequest
    ): Single<OutgoerResponse<List<SpontyResponse>>>


    @POST("sponty/view")
    fun getTaggedViewChange(
        @Body taggedPostReelsViewRequest: TaggedPostReelsViewRequest
    ): Single<OutgoerResponse<TaggedPostReelsViewResponse>>
}