package com.outgoer.api.music

import com.outgoer.api.music.model.MusicCategoryResponse
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class MusicRepository(private val musicRetrofitAPI: MusicRetrofitAPI) {

    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()
    fun getMusicCategory(): Single<OutgoerResponse<ArrayList<MusicCategoryResponse>>> {
        return musicRetrofitAPI.getMusicCategory()
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }


    fun getMusicList(page: Int, categoryId: Int, search: String? = null): Single<OutgoerResponse<ArrayList<MusicResponse>>> {
        return musicRetrofitAPI.getMusicList(page, categoryId, search)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }
}