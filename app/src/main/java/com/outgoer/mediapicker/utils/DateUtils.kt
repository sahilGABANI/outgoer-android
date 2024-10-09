package com.outgoer.mediapicker.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {


    fun getVideoDurationInHourMinSecFormat(date: String): String {
        val date1: Date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date)

        val format = SimpleDateFormat("E, MMM d, yyyy").format(date1)

        return format
    }

    fun getVideoDurationInHourMinFormat(date: String): String {
        val date1: Date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(date)

        val format = SimpleDateFormat("HH:mm a").format(date1)

        return format
    }


    fun getVideoDurationInHourMinSecFormat(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        val mFormatter = Formatter()
        return if (hours > 0) {
            mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString()
        } else {
            mFormatter.format("%02d:%02d", minutes, seconds).toString()
        }
    }

}