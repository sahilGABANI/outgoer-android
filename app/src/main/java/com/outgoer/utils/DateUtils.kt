package com.outgoer.utils

import java.text.SimpleDateFormat
import java.util.*

fun Date.formatTo(
    timeZone: TimeZone,
    formatter: SimpleDateFormat
): String {
    formatter.timeZone = timeZone
    return formatter.format(this)
}

