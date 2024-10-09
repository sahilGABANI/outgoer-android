package com.outgoer.api.tagged_post_reels

import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.sponty.model.SpontyResponse
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsRequest
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsViewRequest
import com.outgoer.api.tagged_post_reels.model.TaggedPostReelsViewResponse
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class TaggedPostReelsRepository(
    private val taggedPostReelsRetrofitAPI: TaggedPostReelsRetrofitAPI,
) {

    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun getVenueTaggedReel(
        pageNo: Int,
        taggedPostReelsRequest: TaggedPostReelsRequest
    ): Single<OutgoerResponse<List<ReelInfo>>> {
        return taggedPostReelsRetrofitAPI.getVenueTaggedReel(pageNo, taggedPostReelsRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getVenueTaggedPost(
        pageNo: Int,
        taggedPostReelsRequest: TaggedPostReelsRequest
    ): Single<OutgoerResponse<List<PostInfo>>> {
        return taggedPostReelsRetrofitAPI.getVenueTaggedPost(pageNo, taggedPostReelsRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getVenueTaggedSponty(
        pageNo: Int,
        taggedPostReelsRequest: TaggedPostReelsRequest
    ): Single<OutgoerResponse<List<SpontyResponse>>> {
        return taggedPostReelsRetrofitAPI.getVenueTaggedSponty(pageNo, taggedPostReelsRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getTaggedViewChange(
        taggedPostReelsViewRequest: TaggedPostReelsViewRequest
    ): Single<OutgoerResponse<TaggedPostReelsViewResponse>> {
        return taggedPostReelsRetrofitAPI.getTaggedViewChange(taggedPostReelsViewRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }
}