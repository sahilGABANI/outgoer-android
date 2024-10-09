package com.outgoer.api.authentication

import com.outgoer.api.authentication.model.*
import com.outgoer.api.venue.model.GeoFenceResponse
import com.outgoer.base.network.OutgoerResponseConverter
import com.outgoer.base.network.model.NewOutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single

class AuthenticationRepository(
    private val authenticationRetrofitAPI: AuthenticationRetrofitAPI,
    private val loggedInUserCache: LoggedInUserCache
) {

    private val outgoerResponseConverter: OutgoerResponseConverter = OutgoerResponseConverter()

    fun register(registerRequest: RegisterRequest): Single<OutgoerResponse<OutgoerUser>> {
        return authenticationRetrofitAPI.registerUser(registerRequest).flatMap {
            outgoerResponseConverter.convertToSingleWithFullResponse(it)
        }
    }

    fun resendCode(resendCodeRequest: ResendCodeRequest): Single<OutgoerCommonResponse> {
        return authenticationRetrofitAPI.resendCode(resendCodeRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun verifyEmail(request: VerifyUserRequest): Single<OutgoerResponse<OutgoerUser>> {
        return authenticationRetrofitAPI.verifyEmail(request)
            .doAfterSuccess { outgoerResponse ->
                if (outgoerResponse.data?.emailVerified == 1) {
                    loggedInUserCache.setLoggedInUserToken(outgoerResponse.accessToken)
                    loggedInUserCache.setLoggedInUserSocketToken(outgoerResponse.socketToken)
                    loggedInUserCache.setLoggedInUser(outgoerResponse.data)
                }
            }.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun checkSocialId(checkSocialIdExistRequest: CheckSocialIdExistRequest): Single<OutgoerResponse<CheckSocialIdExistResponse>> {
        return authenticationRetrofitAPI.checkSocialId(checkSocialIdExistRequest)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun socialLogin(socialMediaLoginRequest: SocialMediaLoginRequest): Single<OutgoerResponse<OutgoerUser>> {
        return authenticationRetrofitAPI.socialLogin(socialMediaLoginRequest)
            .doAfterSuccess { outgoerResponse ->
                loggedInUserCache.setLoggedInUserToken(outgoerResponse.accessToken)
                loggedInUserCache.setLoggedInUserSocketToken(outgoerResponse.socketToken)
                loggedInUserCache.setLoggedInUser(outgoerResponse.data)
            }.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun login(loginRequest: LoginRequest): Single<OutgoerResponse<OutgoerUser>> {
        return authenticationRetrofitAPI.login(loginRequest)
            .doAfterSuccess { outgoerResponse ->
                if (outgoerResponse.data?.emailVerified == 1) {
                    loggedInUserCache.setLoggedInUserToken(outgoerResponse.accessToken)
                    loggedInUserCache.setLoggedInUserSocketToken(outgoerResponse.socketToken)
                    loggedInUserCache.setLoggedInUser(outgoerResponse.data)
                }
            }.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun forgotPassword(forgotPasswordRequest: ForgotPasswordRequest): Single<OutgoerCommonResponse> {
        return authenticationRetrofitAPI.forgotPassword(forgotPasswordRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun forgotPasswordVerifyCode(verifyUserRequest: VerifyUserRequest): Single<OutgoerCommonResponse> {
        return authenticationRetrofitAPI.forgotPasswordVerifyCode(verifyUserRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun resetPassword(resetPasswordRequest: ResetPasswordRequest): Single<OutgoerCommonResponse> {
        return authenticationRetrofitAPI.resetPassword(resetPasswordRequest)
            .flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun updateNotificationToken(updateNotificationTokenRequest: UpdateNotificationTokenRequest): Single<OutgoerResponse<UpdateNotificationToken>> {
        return authenticationRetrofitAPI.updateNotificationToken(updateNotificationTokenRequest)
            .doAfterSuccess {}.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun logout(deviceId: String): Single<OutgoerResponse<OutgoerUser>> {
        return authenticationRetrofitAPI.logout(deviceId)
            .doAfterSuccess { outgoerResponse ->
                if(!outgoerResponse.accessToken.isNullOrEmpty()) {
                    loggedInUserCache.setLoggedInUserToken(outgoerResponse.accessToken)
                    loggedInUserCache.setLoggedInUserSocketToken(outgoerResponse.socketToken)
                    loggedInUserCache.setLoggedInUser(outgoerResponse.data)
                } else {
                    loggedInUserCache.clearLoggedInUserLocalPrefs()
                }
            }.flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun logoutAllUsers(deviceId: String): Single<NewOutgoerCommonResponse> {
        return authenticationRetrofitAPI.logoutAll(deviceId)
            .doAfterSuccess {
                loggedInUserCache.clearLoggedInUserLocalPrefs()
            }.flatMap { outgoerResponseConverter.convertNewCommonResponse(it) }
    }

    fun checkUsername(request: ChekUsernameRequest): Single<OutgoerResponse<ChekUsernameResponse>> {
        return authenticationRetrofitAPI.checkUsername(request)
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun getCheckIn(): Single<OutgoerResponse<GeoFenceResponse>> {
        return authenticationRetrofitAPI.getCheckIn()
            .flatMap { outgoerResponseConverter.convertToSingleWithFullResponse(it) }
    }

    fun deactivate(): Single<OutgoerCommonResponse> {
        return authenticationRetrofitAPI.deactivate()
            .doAfterSuccess {
                loggedInUserCache.clearLoggedInUserLocalPrefs()
            }.flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }

    fun activateAccount(request: AccountActivationRequest): Single<OutgoerCommonResponse> {
        return authenticationRetrofitAPI.activateAccount(request)
            .doAfterSuccess {}.flatMap { outgoerResponseConverter.convertCommonResponse(it) }
    }
}