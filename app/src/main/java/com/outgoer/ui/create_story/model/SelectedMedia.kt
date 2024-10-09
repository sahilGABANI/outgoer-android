package com.outgoer.ui.create_story.model

import android.os.Parcelable
import com.outgoer.api.event.model.GooglePlaces
import com.outgoer.api.music.model.MusicResponse
import com.outgoer.api.venue.model.VenueMapInfo
import kotlinx.parcelize.Parcelize
import java.net.URLConnection

@Parcelize
data class SelectedMedia(
    var filePath: String,
    var isSelected: Boolean = false,
    var isTrimMusic: Boolean? = false,
    var fileName: String? = "",
    var musicFileName: String? = "",
    var trimMusicFileName: String? = "",
    var location: VenueMapInfo? = null,
    var googleLocation: GooglePlaces? = null,
    var musicResponse: MusicResponse? = null,
    var durationSet: Boolean? = false,
    var mergeAudioVideoPath: String? = "",
    var mergeAudioImagePath: String? = "",
    var isCompress: Boolean? = false,
    var compressFilePath: String? = "",
    var isMergeAudioVideo: Boolean = false,
    var uid: String? = null,
    var imageFromCloudFlare: String? = null,
    var imageIsUploaded: Boolean? = false,
    var videoIsUploaded: Boolean? = false,
    var retryOption: Boolean? = false
) : Parcelable {
   fun isVideo(): Boolean {
        val mimeType: String = URLConnection.guessContentTypeFromName(filePath)
        return mimeType.startsWith("video") || !mergeAudioImagePath.isNullOrEmpty()
    }


    fun isCheckVideo(): Boolean {
        val mimeType: String = URLConnection.guessContentTypeFromName(filePath)
        return mimeType.startsWith("video")
    }

    fun isImageVideoCheck(): Boolean {
        return !mergeAudioImagePath.isNullOrEmpty()
    }
}