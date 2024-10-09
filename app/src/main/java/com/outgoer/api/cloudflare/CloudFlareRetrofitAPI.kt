package com.outgoer.api.cloudflare

import com.outgoer.api.cloudflare.model.*
import com.outgoer.base.network.model.OutgoerResponse
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.http.*

interface CloudFlareRetrofitAPI {
    @GET("users/api-token")
    fun getCloudFlareConfig(): Single<OutgoerResponse<CloudFlareConfig>>

    @Multipart
    @POST
    fun uploadImageToCloudFlare(
        @Url url: String?,
        @Header("Authorization") authToken: String?,
        @Part file: MultipartBody.Part,
    ): Single<UploadImageCloudFlareResponse>

    @Multipart
    @POST
    fun uploadVideoToCloudFlare(
        @Url url: String?,
        @Header("Authorization") authToken: String?,
        @Part file: MultipartBody.Part,
    ): Single<UploadVideoCloudFlareResponse>

    //    Authorization", "AWS4-HMAC-SHA256 Credential=undefined/20230211/us-east-1/execute-api/aws4_request, SignedHeaders=host;x-amz-date, Signature=899dfe576de355947b8fd64856b11018615a854ba98bcb96d2bd52f07e3a7b33"
    @Multipart
    @PUT
    fun uploadAudioToCloudFlare(
        @Url url: String?,
        @Part file: MultipartBody.Part,
        @Header("X-Amz-Date") date: String?,
        @Header("Authorization") authToken: String? = "AWS4-HMAC-SHA256 Credential=70cd13d4c19210c3d3b9eff883459da2/20230213/auto/s3/aws4_request, SignedHeaders=host;x-amz-content-sha256;x-amz-date, Signature=e36d433600c065f5c9886e236141aad91fe450ec8fbb81972285f51d01787480",
        @Header("x-amz-content-sha256") content: String? = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    ): Completable

    @GET
    fun getUploadVideoDetails(
        @Url url: String?,
        @Header("Authorization") authToken: String?,
    ): Single<CloudFlareVideoDetails>

    @GET
    fun getUploadVideoStatus(
        @Url url: String?,
        @Header("Authorization") authToken: String?,
    ): Single<CloudFlareVideoStatus>
}