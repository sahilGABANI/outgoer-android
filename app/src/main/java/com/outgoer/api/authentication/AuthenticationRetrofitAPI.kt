package com.outgoer.api.authentication

import com.outgoer.api.authentication.model.*
import com.outgoer.api.venue.model.GeoFenceResponse
import com.outgoer.base.network.model.NewOutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerCommonResponse
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthenticationRetrofitAPI {
    @POST("auth/signup")
    fun registerUser(@Body registerRequest: RegisterRequest): Single<OutgoerResponse<OutgoerUser>>

    @POST("auth/resend-code")
    fun resendCode(@Body resendCodeRequest: ResendCodeRequest): Single<OutgoerCommonResponse>

    @POST("auth/verify-email")
    fun verifyEmail(@Body verifyUserRequest: VerifyUserRequest): Single<OutgoerResponse<OutgoerUser>>

    @POST("auth/check-social-id")
    fun checkSocialId(@Body checkSocialIdExistRequest: CheckSocialIdExistRequest): Single<OutgoerResponse<CheckSocialIdExistResponse>>

    @POST("auth/sociallogin")
    fun socialLogin(@Body socialMediaLoginRequest: SocialMediaLoginRequest): Single<OutgoerResponse<OutgoerUser>>

    @POST("auth/login")
    fun login(@Body loginRequest: LoginRequest): Single<OutgoerResponse<OutgoerUser>>

    @POST("auth/forgot-password")
    fun forgotPassword(@Body forgotPasswordRequest: ForgotPasswordRequest): Single<OutgoerCommonResponse>

    @POST("auth/forgot-password-verify-code")
    fun forgotPasswordVerifyCode(@Body verifyUserRequest: VerifyUserRequest): Single<OutgoerCommonResponse>

    @POST("auth/reset-password")
    fun resetPassword(@Body resetPasswordRequest: ResetPasswordRequest): Single<OutgoerCommonResponse>

    @POST("users/device-token")
    fun updateNotificationToken(@Body updateNotificationTokenRequest: UpdateNotificationTokenRequest): Single<OutgoerResponse<UpdateNotificationToken>>

    @GET("get_check_in")
    fun getCheckIn(): Single<OutgoerResponse<GeoFenceResponse>>

    @GET("auth/logout")
    fun logout(@Query("device_id") deviceId: String): Single<OutgoerResponse<OutgoerUser>>

    @POST("auth/logout-all")
    fun logoutAll(@Query("device_id") deviceId: String): Single<NewOutgoerCommonResponse>

    @POST("auth/check-username")
    fun checkUsername(@Body request: ChekUsernameRequest): Single<OutgoerResponse<ChekUsernameResponse>>

    @GET("users/deactivate")
    fun deactivate(): Single<OutgoerCommonResponse>

    @POST("users/activation")
    fun activateAccount(@Body request: AccountActivationRequest): Single<OutgoerCommonResponse>
}