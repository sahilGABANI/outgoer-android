package com.outgoer.mediapicker.models

import java.io.Serializable

class VideoModel : Serializable {
    var fileName: String? = null
    var filePath: String? = null
    var mimeType: String? = null
    var videoWidth = 0
    var videoHeight = 0
    var fileSize: Long = 0
    var duration = ""
    var dateModifiedInMillis: Long = 0
    var isSelected = false
}