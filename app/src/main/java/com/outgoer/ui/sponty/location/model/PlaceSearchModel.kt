package com.outgoer.ui.sponty.location.model

import com.google.gson.annotations.SerializedName

data class PlaceSearchResponse(
    @field:SerializedName("html_attributions")
    val htmlAttributions: ArrayList<String> = arrayListOf(),

    @field:SerializedName("next_page_token")
    val nextPageToken: String? = null,

    @field:SerializedName("results")
    val results: ArrayList<ResultResponse> = arrayListOf(),

    @field:SerializedName("status")
    val status: String? = null,

    @field:SerializedName("predictions")
    val predictions: ArrayList<Predictions>? = arrayListOf(),

)
data class Predictions(
    @field:SerializedName("reference")
    val reference: String? = null,

    @field:SerializedName("types")
    val types: List<String?>? = null,

    @field:SerializedName("matched_substrings")
    val matchedSubstrings: List<MatchedSubstringsItem?>? = null,


    @field:SerializedName("terms")
    val terms: List<TermsItem?>? = null,

    @field:SerializedName("structured_formatting")
    val structuredFormatting: StructuredFormatting? = null,

    @field:SerializedName("description")
    val description: String? = null,

    @field:SerializedName("place_id")
    val placeId: String? = null
)
data class TermsItem(

    @field:SerializedName("offset")
    val offset: Int? = null,

    @field:SerializedName("value")
    val value: String? = null
)

data class MatchedSubstringsItem(

    @field:SerializedName("offset")
    val offset: Int? = null,

    @field:SerializedName("length")
    val length: Int? = null
)

data class StructuredFormatting(

    @field:SerializedName("main_text_matched_substrings")
    val mainTextMatchedSubstrings: List<MainTextMatchedSubstringsItem?>? = null,

    @field:SerializedName("secondary_text")
    val secondaryText: String? = null,

    @field:SerializedName("main_text")
    val mainText: String? = null
)

data class MainTextMatchedSubstringsItem(

    @field:SerializedName("offset")
    val offset: Int? = null,

    @field:SerializedName("length")
    val length: Int? = null
)


data class ResultResponse(
    @field:SerializedName("business_status")
    val businessStatus: String? = null,

    @field:SerializedName("formatted_address")
    val formattedAddress: String? = null,

    @field:SerializedName("geometry")
    val geometry: GeoMetryResponse? = null,

    @field:SerializedName("icon")
    val icon: String? = null,

    @field:SerializedName("icon_background_color")
    val iconBackgroundColor: String? = null,

    @field:SerializedName("icon_mask_base_uri")
    val iconMaskBaseUri: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("photos")
    val photos: ArrayList<PhotoResponse> = arrayListOf(),

    @field:SerializedName("place_id")
    val placeId: String? = null,

    @field:SerializedName("plus_code")
    val plusCode: PlusCodeResponse? = null,

    @field:SerializedName("rating")
    val rating: Double = 0.0,

    @field:SerializedName("reference")
    val reference: String? = null,

    @field:SerializedName("types")
    val types: ArrayList<String> = arrayListOf(),

    @field:SerializedName("user_ratings_total")
    val userRatingsTotal: Int = 0
)

data class GeoMetryResponse(
    @field:SerializedName("location")
    val location: LocationResponse? = null,

    @field:SerializedName("viewport")
    val viewport: ViewPortResponse? = null,
)

data class ViewPortResponse(
    @field:SerializedName("northeast")
    val northeast: LocationResponse? = null,

    @field:SerializedName("southwest")
    val southwest: LocationResponse? = null,
)

data class LocationResponse(
    @field:SerializedName("lat")
    val lat: Double = 0.0,

    @field:SerializedName("lng")
    val lng: Double = 0.0,
)

data class PlusCodeResponse(
    @field:SerializedName("compound_code")
    val compoundCode: String? = null,

    @field:SerializedName("global_code")
    val globalCode: String? = null,
)


data class PhotoResponse(
    @field:SerializedName("height")
    val height: Int = 0,

    @field:SerializedName("html_attributions")
    val htmlAttributions: ArrayList<String> = arrayListOf(),

    @field:SerializedName("photo_reference")
    val photoReference: String?= null,

    @field:SerializedName("width")
    val width: Int = 0,
)


data class PlaceDetailsResponse(

    @field:SerializedName("result")
    val result: Result? = null,

    @field:SerializedName("html_attributions")
    val htmlAttributions: List<Any?>? = null,

    @field:SerializedName("status")
    val status: String? = null
)

data class Southwest(

    @field:SerializedName("lng")
    val lng: Double? = null,

    @field:SerializedName("lat")
    val lat: Double? = null
)

data class Location(

    @field:SerializedName("lng")
    val lng: Double? = null,

    @field:SerializedName("lat")
    val lat: Double? = null
)

data class AddressComponentsItem(

    @field:SerializedName("types")
    val types: List<String?>? = null,

    @field:SerializedName("short_name")
    val shortName: String? = null,

    @field:SerializedName("long_name")
    val longName: String? = null
)

data class Viewport(

    @field:SerializedName("southwest")
    val southwest: Southwest? = null,

    @field:SerializedName("northeast")
    val northeast: Northeast? = null
)

data class Result(

    @field:SerializedName("utc_offset")
    val utcOffset: Int? = null,

    @field:SerializedName("formatted_address")
    val formattedAddress: String? = null,

    @field:SerializedName("types")
    val types: List<String?>? = null,

    @field:SerializedName("icon")
    val icon: String? = null,

    @field:SerializedName("icon_background_color")
    val iconBackgroundColor: String? = null,

    @field:SerializedName("address_components")
    val addressComponents: List<AddressComponentsItem?>? = null,

    @field:SerializedName("photos")
    val photos: List<PhotosItem?>? = null,

    @field:SerializedName("url")
    val url: String? = null,

    @field:SerializedName("reference")
    val reference: String? = null,

    @field:SerializedName("name")
    val name: String? = null,

    @field:SerializedName("geometry")
    val geometry: Geometry? = null,

    @field:SerializedName("icon_mask_base_uri")
    val iconMaskBaseUri: String? = null,

    @field:SerializedName("vicinity")
    val vicinity: String? = null,

    @field:SerializedName("adr_address")
    val adrAddress: String? = null,

    @field:SerializedName("place_id")
    val placeId: String? = null
)

data class Northeast(

    @field:SerializedName("lng")
    val lng: Double? = null,

    @field:SerializedName("lat")
    val lat: Double? = null
)

data class Geometry(

    @field:SerializedName("viewport")
    val viewport: Viewport? = null,

    @field:SerializedName("location")
    val location: Location? = null
)

data class PhotosItem(

    @field:SerializedName("photo_reference")
    val photoReference: String? = null,

    @field:SerializedName("width")
    val width: Int? = null,

    @field:SerializedName("html_attributions")
    val htmlAttributions: List<String?>? = null,

    @field:SerializedName("height")
    val height: Int? = null
)
