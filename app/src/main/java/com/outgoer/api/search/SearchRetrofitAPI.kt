package com.outgoer.api.search

import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.post.model.MyTagBookmarkInfo
import com.outgoer.api.search.model.SearchAccountRequest
import com.outgoer.api.search.model.SearchPlacesRequest
import com.outgoer.api.search.model.SearchTopPostReelRequest
import com.outgoer.api.venue.model.VenueListInfo
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface SearchRetrofitAPI {

    @POST("users/top-post-reel")
    fun getTopPostReel(
        @Query("page") pageNo: Int,
        @Body request: SearchTopPostReelRequest
    ): Single<OutgoerResponse<List<MyTagBookmarkInfo>>?>

    @POST("users/search-accounts")
    fun searchAccounts(
        @Query("page") pageNo: Int,
        @Body request: SearchAccountRequest
    ): Single<OutgoerResponse<List<FollowUser>>>

    @POST("users/search-places")
    fun searchPlaces(
        @Query("page") pageNo: Int,
        @Body request: SearchPlacesRequest
    ): Single<OutgoerResponse<List<VenueListInfo>>>
}