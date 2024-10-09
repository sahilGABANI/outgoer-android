package com.outgoer.base

import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.venue.model.GeoFenceResponse
import com.outgoer.ui.home.view.OutgoerTabBarView
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

object RxBus {

    private val publisher = PublishSubject.create<Any>()

    fun publish(event: Any) {
        publisher.onNext(event)
    }

    // Listen should return an Observable and not the publisher
    // Using ofType we filter only events that match that class type
    fun <T> listen(eventType: Class<T>): Observable<T> = publisher.ofType(eventType)
}

class RxEvent {
    data class HomeTabChangeEvent(@OutgoerTabBarView.TabType val tabType: Int)
    object RefreshHomePagePost
    data class RefreshHomePagePostPlayVideo(val isVisible: Boolean)
    object RefreshMyProfile
    object RefreshOtherUserProfile
    data class UpdateNotificationBadge(val isVisible: Boolean)
    object RefreshMapPage
    object VenueMapFragment
    object OpenVenueUserProfile
    data class GeoFenceResEntry(val geoFenceResponse: GeoFenceResponse)
    data class GeoFenceResExit(val geoFenceResponse: GeoFenceResponse)
    data class StartVideo(val checkImage:Boolean)
    data class CheckHomeFragmentIsVisible(val checkImage:Boolean)
    data class CurrentPostionReels(val position : Int, val width : Int, val height : Int)
    data class SearchMusic(val searchString: String?, val musicCategoryId: Int)
    data class SearchStoryLocation(val searchString: String?)
    data class DataReload(val selectedTab: String)
    data class DataReloadReel(val selectedTab: String)
    object RefreshVenueList
}