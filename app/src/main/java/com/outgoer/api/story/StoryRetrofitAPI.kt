package com.outgoer.api.story

import com.outgoer.api.story.model.MentionUser
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.api.story.model.StoryRequest
import com.outgoer.api.story.model.ViewStoryRequest
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StoryRetrofitAPI {

    @GET("story")
    fun getListOfStories(
        @Query("page") pageNo: Int,
        @Query("timeZone") timeZone: String
    ): Single<OutgoerResponse<List<StoryListResponse>>>

    @POST("story/create")
    fun createStory(
        @Body storyRequest: StoryRequest
    ): Single<OutgoerResponse<StoryListResponse>>

    @POST("story/view")
    fun viewStory(
        @Body viewStoryRequest: ViewStoryRequest
    ): Single<OutgoerCommonResponse>

    @POST("story/view-list")
    fun viewListStory(
        @Query("page") pageNo: Int,
        @Body viewStoryRequest: ViewStoryRequest
    ): Single<OutgoerResponse<ArrayList<MentionUser>>>

    @DELETE("story/delete/{id}")
    fun deleteStory(
        @Path("id") id: Int
    ): Single<OutgoerCommonResponse>
}