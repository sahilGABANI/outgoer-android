package com.outgoer.api.cloudflare.model

import com.google.gson.annotations.SerializedName

data class CloudFlareVideoDetails(
	@field:SerializedName("result")
	val result: CloudFlareVideoInfo
)

data class CloudFlareVideoInfo(
	@field:SerializedName("uid")
	val uid: String? = null,

	@field:SerializedName("thumbnail")
	val thumbnail: String? = null,

	@field:SerializedName("preview")
	val preview: String? = null,

	@field:SerializedName("playback")
	val playback: PlaybackRes? = null
)

data class PlaybackRes(
	@field:SerializedName("hls")
	val hls: String? = null
)