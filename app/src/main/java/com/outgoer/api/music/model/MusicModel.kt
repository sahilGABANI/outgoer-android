package com.outgoer.api.music.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize


@Parcelize
data class MusicCategoryResponse(

    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("category_name")
    val categoryName: String? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

):Parcelable


@Parcelize
data class MusicResponse(
    @field:SerializedName("id")
    val id: Int,

    @field:SerializedName("song_title")
    val songTitle: String? = null,

    @field:SerializedName("song_subtitle")
    val songSubtitle: String? = null,

    @field:SerializedName("song_file")
    val songFile: String? = null,

    @field:SerializedName("song_image")
    val songImage: String? = null,

    @field:SerializedName("user_id")
    val userId: Int? = null,

    @field:SerializedName("category_id")
    val categoryId: Int? = null,

    @field:SerializedName("created_at")
    val createdAt: String? = null,

    @field:SerializedName("artists")
    val artists: String? = null,

    @field:SerializedName("updated_at")
    val updatedAt: String? = null,

    @field:SerializedName("deleted_at")
    val deletedAt: String? = null,

    @field:SerializedName("username")
    val username: String? = null,

    @field:SerializedName("profile_verified")
    val profileVerified: String? = null,

    @field:SerializedName("category_name")
    val categoryName: String? = null,

    var isPlaying: Boolean = false
): Parcelable