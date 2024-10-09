package com.outgoer.mediapicker.models

import android.net.Uri
import java.io.Serializable

class PhotoModel : Serializable {
    var uri: Uri? = null
    var name: String? = null
    var path: String? = null
    var type: String? = null
    var width = 0
    var height = 0
    var orientation = 0
    var size: Long = 0
    var duration: Long = 0
    var time: Long = 0
    var selected = false
}