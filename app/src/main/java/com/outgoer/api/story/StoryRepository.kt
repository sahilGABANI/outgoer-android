package com.outgoer.api.story

import com.outgoer.api.story.model.MentionUser
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.api.story.model.StoryRequest
import com.outgoer.api.story.model.ViewStoryRequest
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class StoryRepository(private val storyRetrofitAPI: StoryRetrofitAPI) {

    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun getListOfStories(pageNo: Int, timeZone: String): Single<OutgoerResponse<List<StoryListResponse>>> {
        return storyRetrofitAPI.getListOfStories(pageNo, timeZone)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun createStory(storyRequest: StoryRequest): Single<OutgoerResponse<StoryListResponse>> {
        return storyRetrofitAPI.createStory(storyRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun viewStory(viewStoryRequest: ViewStoryRequest): Single<OutgoerCommonResponse> {
        return storyRetrofitAPI.viewStory(viewStoryRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun viewListStory(pageNo: Int, viewStoryRequest: ViewStoryRequest): Single<OutgoerResponse<ArrayList<MentionUser>>> {
        return storyRetrofitAPI.viewListStory(pageNo, viewStoryRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }
    fun deleteStory(storyId: Int): Single<OutgoerCommonResponse> {
        return storyRetrofitAPI.deleteStory(storyId)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }
}