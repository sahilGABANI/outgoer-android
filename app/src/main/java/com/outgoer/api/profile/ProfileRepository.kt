package com.outgoer.api.profile

import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.post.model.PeopleForTag
import com.outgoer.api.profile.model.*
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import com.outgoer.ui.block.BlockProfileActivity
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody

class ProfileRepository(
    private val profileRetrofitAPI: ProfileRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache,
) {
    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun myProfile(): Single<OutgoerResponse<OutgoerUser>> {
        return profileRetrofitAPI.myProfile()
            .doAfterSuccess { outgoerResponse ->
                loggedInUserCache.setLoggedInUser(outgoerResponse.data)
            }.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getDeviceAccount(request: DeviceAccountRequest): Single<OutgoerResponse<ArrayList<PeopleForTag>>> {
        return profileRetrofitAPI.getDeviceAccount(request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun switchAccount(request: SwitchDeviceAccountRequest): Single<OutgoerResponse<OutgoerUser>> {
        return profileRetrofitAPI.switchAccount(request)
            .doAfterSuccess { outgoerResponse ->
                println("Switch account response")
                loggedInUserCache.setLoggedInUserToken(outgoerResponse.accessToken)
                loggedInUserCache.setLoggedInUserSocketToken(outgoerResponse.socketToken)
                loggedInUserCache.setLoggedInUser(outgoerResponse.data)
            }.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun setVisibility(request: SetVisibilityRequest): Single<OutgoerResponse<OutgoerUser>> {
        return profileRetrofitAPI.setVisibility(request)
            .doAfterSuccess { outgoerResponse ->
                loggedInUserCache.setLoggedInUser(outgoerResponse.data)
            }.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun uploadProfile(request: UpdateProfileRequest): Single<OutgoerResponse<OutgoerUser>> {
        return profileRetrofitAPI.uploadProfile(request)
            .doAfterSuccess { outgoerResponse ->
                loggedInUserCache.setLoggedInUser(outgoerResponse.data)
            }.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun updateLocation(locationUpdateRequest: LocationUpdateRequest): Single<OutgoerCommonResponse> {
        return profileRetrofitAPI.updateLocation(locationUpdateRequest).doAfterSuccess {
            loggedInUserCache.setLocation(locationUpdateRequest)
        }
    }

    fun searchUser(searchUserListRequest: SearchUserListRequest): Single<List<OutgoerUser>> {
        return profileRetrofitAPI.searchUser(searchUserListRequest).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun getSuggestedUsersList(pageNo: Int, perPage: Int,search :String): Single<List<OutgoerUser>> {
        return profileRetrofitAPI.getSuggestedUsersList(pageNo, perPage,search).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun getNearByUsersList(locationInfo: LocationUpdateRequest): Single<List<NearByUserResponse>> {
        return profileRetrofitAPI.getNearByUsersList(locationInfo).flatMap {
            outgoerResponseConverter.convertToSingle(it)
        }
    }

    fun getUserProfile(userId: Int?): Single<OutgoerResponse<OutgoerUser>> {
        val request = GetUserProfileRequest(userId = userId)
        return profileRetrofitAPI.getUserProfile(request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun blockUserProfile(blockUserRequest: BlockUserRequest): Single<OutgoerCommonResponse> {
        return profileRetrofitAPI.blockUserProfile(blockUserRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun reportUserVenue(reportUserRequest: ReportUserRequest): Single<OutgoerCommonResponse> {
        return profileRetrofitAPI.reportUserVenue(reportUserRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun editVenue(request: UpdateVenueRequest): Single<OutgoerResponse<OutgoerUser>> {
        return profileRetrofitAPI.editVenue(request)
            .doAfterSuccess { outgoerResponse ->
                loggedInUserCache.setLoggedInUser(outgoerResponse.data)
            }.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun sendVerificationRequest(request: GetUserProfileRequest): Single<OutgoerCommonResponse> {
        return profileRetrofitAPI.sendVerificationRequest(request)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }


    fun blockAccounts(blockUserListRequest: BlockUserListRequest): Single<OutgoerResponse<ArrayList<BlockUserResponse>>> {
        return profileRetrofitAPI.blockAccounts(blockUserListRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }
}