package com.outgoer.api.cloudflare.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CloudFlareConfig(
    @field:SerializedName("account_id")
    val accountId: String? = null,

    @field:SerializedName("api_token")
    val apiToken: String? = null,

    @field:SerializedName("image_upload_url")
    val imageUploadUrl: String? = null,

    @field:SerializedName("video_key")
    val videoKey: String? = null,
)

@Keep
data class UploadImageCloudFlareResponse(
    @field:SerializedName("result")
    val result: UploadImageCloudFlareResult? = null,

    @field:SerializedName("success")
    val success: Boolean,

    @field:SerializedName("errors")
    val errors: List<String>? = null,
)

@Keep
data class UploadImageCloudFlareResult(
    @field:SerializedName("variants")
    val variants: List<String>? = null,

    @field:SerializedName("preview")
    val preview: String? = null,
)

@Keep
data class UploadVideoCloudFlareResponse(
    @field:SerializedName("result")
    val result: UploadVideoCloudFlareResult? = null,

    @field:SerializedName("success")
    val success: Boolean,

    @field:SerializedName("errors")
    val errors: List<String>? = null,
)

@Keep
data class UploadVideoCloudFlareResult(
    @field:SerializedName("uid")
    val uid: String? = null,

    @field:SerializedName("thumbnail")
    val thumbnail: String? = null,

    @field:SerializedName("preview")
    val preview: String? = null,
)