package com.outgoer.api.profile

import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.api.profile.model.*
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import com.outgoer.ui.block.BlockProfileActivity
import io.reactivex.Single
import retrofit2.http.*

interface ProfileRetrofitAPI {
    @GET("users/my-profile")
    fun myProfile(): Single<OutgoerResponse<OutgoerUser>>

    @POST("users/get-device-account")
    fun getDeviceAccount(@Body request: DeviceAccountRequest): Single<OutgoerResponse<ArrayList<PeopleForTag>>>

    @POST("users/switch-account")
    fun switchAccount(@Body request: SwitchDeviceAccountRequest): Single<OutgoerResponse<OutgoerUser>>

    @POST("users/set-visibility")
    fun setVisibility(@Body request: SetVisibilityRequest): Single<OutgoerResponse<OutgoerUser>>

    @POST("users/update-profile")
    fun uploadProfile(@Body request: UpdateProfileRequest): Single<OutgoerResponse<OutgoerUser>>

    @POST("users/update-location")
    fun updateLocation(@Body locationUpdateRequest: LocationUpdateRequest): Single<OutgoerCommonResponse>

    @POST("users/seacrh-user")
    fun searchUser(@Body searchUserListRequest: SearchUserListRequest): Single<OutgoerResponse<List<OutgoerUser>>>

    @GET("users/suggested_users")
    fun getSuggestedUsersList(@Query("page") pageNo: Int, @Query("per_page") perPage: Int,@Query("search") search:String): Single<OutgoerResponse<List<OutgoerUser>>>

    @POST("users/nearby_users")
    fun getNearByUsersList(@Body locationInfo: LocationUpdateRequest): Single<OutgoerResponse<List<NearByUserResponse>>>

    @POST("users/get-user")
    fun getUserProfile(@Body request: GetUserProfileRequest): Single<OutgoerResponse<OutgoerUser>>

    @POST("users/update")
    fun editVenue(@Body request: UpdateVenueRequest): Single<OutgoerResponse<OutgoerUser>>

    @POST("users/badge-request")
    fun sendVerificationRequest(@Body request: GetUserProfileRequest): Single<OutgoerCommonResponse>

    @POST("block")
    fun blockAccounts(@Body blockUserListRequest: BlockUserListRequest): Single<OutgoerResponse<ArrayList<BlockUserResponse>>>

    @POST("block/add")
    fun blockUserProfile(@Body blockUserRequest: BlockUserRequest): Single<OutgoerCommonResponse>

    @POST("report/user-venue")
    fun reportUserVenue(@Body reportUserRequest: ReportUserRequest): Single<OutgoerCommonResponse>

}