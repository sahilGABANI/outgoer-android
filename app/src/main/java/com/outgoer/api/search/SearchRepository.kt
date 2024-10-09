package com.outgoer.api.search

import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.post.model.MyTagBookmarkInfo
import com.outgoer.api.search.model.SearchAccountRequest
import com.outgoer.api.search.model.SearchPlacesRequest
import com.outgoer.api.search.model.SearchTopPostReelRequest
import com.outgoer.api.venue.model.VenueListInfo
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject

class SearchRepository(
    private val searchRetrofitAPI: SearchRetrofitAPI
) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    private val searchStringSubject: BehaviorSubject<String> = BehaviorSubject.create()
    val searchString: Observable<String> = searchStringSubject.hide()

    fun getTopPostReel(pageNo: Int, searchText: String): Single<OutgoerResponse<List<MyTagBookmarkInfo>>?> {
        val request = SearchTopPostReelRequest(search = searchText)
        return searchRetrofitAPI.getTopPostReel(pageNo, request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun searchAccounts(pageNo: Int, searchText: String): Single<List<FollowUser>> {
        return searchRetrofitAPI.searchAccounts(pageNo, SearchAccountRequest(search = searchText)).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun searchPlaces(pageNo: Int, searchText: String): Single<List<VenueListInfo>> {
        return searchRetrofitAPI.searchPlaces(pageNo, SearchPlacesRequest(search = searchText)).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun searchString(searchString: String) {
        searchStringSubject.onNext(searchString)
    }
}