package com.outgoer.api.venue.model

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem


class VenueInfo(
    id: Int,
    lat: Double,
    lng: Double,
    title: String,
    snippet: String
) : ClusterItem {

    private val position: LatLng
    private val title: String
    private val snippet: String
    private val id: Int


    override fun getPosition(): LatLng {
        return position
    }

    override fun getTitle(): String {
        return title
    }

    override fun getSnippet(): String {
        return snippet
    }



    init {
        position = LatLng(lat, lng)
        this.title = title
        this.snippet = snippet
        this.id = id
    }
}
