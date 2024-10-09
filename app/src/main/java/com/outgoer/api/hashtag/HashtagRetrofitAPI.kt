package com.outgoer.api.hashtag

import com.outgoer.api.hashtag.model.HashtagResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface HashtagRetrofitAPI {
    @GET("hashtags")
    fun getHashtagList(
        @Query("page") page: Int,
        @Query("search") search: String? = null
    ): Single<OutgoerResponse<ArrayList<HashtagResponse>>>
}