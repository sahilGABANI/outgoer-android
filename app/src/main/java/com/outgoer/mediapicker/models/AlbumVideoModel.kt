package com.outgoer.mediapicker.models

import java.io.Serializable

class AlbumVideoModel : Serializable {
    var albumName = ""
    var albumUrl = ""
    var videoModelArrayList = ArrayList<VideoModel>()
    var isSelected = false
}