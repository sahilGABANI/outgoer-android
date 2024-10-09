package com.outgoer.ui.sponty.location

import com.outgoer.ui.sponty.location.model.PlaceDetailsResponse
import com.outgoer.ui.sponty.location.model.PlaceSearchResponse
import retrofit2.Call
import retrofit2.http.*

interface PlaceSearch {
    @GET("api/place/textsearch/json")
    fun getPlacesLists(@Query("key") key: String, @Query("query") type: String, @Query("location") location: String, @Query("radius") radius: Int): Call<PlaceSearchResponse>

    @GET("api/place/autocomplete/json")
    fun getAutoCompleteLists(@Query("key") key: String, @Query("input") type: String,@Query("locationbias", encoded = true) locationbias :String): Call<PlaceSearchResponse>


    @GET("api/place/details/json")
    fun getPlaceDetails(@Query("key") key: String, @Query("place_id") type: String): Call<PlaceDetailsResponse>
}
