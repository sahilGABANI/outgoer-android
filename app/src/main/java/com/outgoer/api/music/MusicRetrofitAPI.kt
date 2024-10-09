package com.outgoer.api.music

import com.outgoer.api.music.model.MusicCategoryResponse
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Query

interface MusicRetrofitAPI {

    @GET("music/category")
    fun getMusicCategory(): Single<OutgoerResponse<ArrayList<MusicCategoryResponse>>>

    @GET("music/bycategory")
    fun getMusicList(
        @Query("page") page: Int,
        @Query("category_id") categoryId: Int,
        @Query("search") search: String? = null
    ): Single<OutgoerResponse<ArrayList<MusicResponse>>>
}