package com.outgoer.mediapicker.models

import java.io.Serializable

class AlbumPhotoModel : Serializable {
    var albumName = ""
    var albumUrl = ""
    var photoModelArrayList = ArrayList<PhotoModel>()
    var isSelected = false
}