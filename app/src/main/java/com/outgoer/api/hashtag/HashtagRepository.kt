package com.outgoer.api.hashtag

import com.outgoer.api.hashtag.model.HashtagResponse
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class HashtagRepository(private val hashtagRetrofitAPI: HashtagRetrofitAPI) {

    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()
    fun getHashtagList(
        page: Int,
        search: String? = null
    ): Single<OutgoerResponse<ArrayList<HashtagResponse>>> {
        return hashtagRetrofitAPI.getHashtagList(page, search)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }
}