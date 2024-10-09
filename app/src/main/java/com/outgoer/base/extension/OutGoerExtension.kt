package com.outgoer.base.extension

import android.R.attr.x
import android.R.attr.y
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.outgoer.BuildConfig
import com.outgoer.R
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.group.model.GroupUserInfo
import timber.log.Timber
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow


const val GIF_API_KEY = "o8GPN9Cnyx6IZqb0HVmDB3G8V4tpzUqf"

const val BASE_DEV_URL = "https://dev.outgoerapp.com/"
const val BASE_QA_URL = "https://qa.outgoerapp.com/"
const val BASE_PROD_URL = "https://prod.outgoerapp.com/"
const val BASE_URL =  BASE_PROD_URL
fun getAPIBaseUrl(): String {

//    return "https://qa.outgoerapp.com/api/"
    return "${BASE_URL}api/"
}

fun getSocketBaseUrl(): String {
//    return "http://4.147.224.29:5000"
    return "http://4.195.130.27:5000"

//    return if (BuildConfig.DEBUG) {
//        "https://dev.outgoerapp.com:5000" //"http://44.198.229.6:5000"
//    } else {
//        "https://dev.outgoerapp.com:5000" //"http://44.198.229.6:5000"
//    }
}

const val cloudFlareImageUploadBaseUrl = "https://api.cloudflare.com/client/v4/accounts/%s/images/v1"
const val cloudFlareVideoUploadBaseUrl = "https://api.cloudflare.com/client/v4/accounts/%s/stream"
const val cloudFlareGetVideoUploadBaseUrl = "https://api.cloudflare.com/client/v4/accounts/%s/stream/%s"

fun Int.prettyCount(): String? {
    val suffix = charArrayOf(' ', 'k', 'M', 'B', 'T', 'P', 'E')
    val numValue: Long = this.toLong()
    val value = floor(log10(numValue.toDouble())).toInt()
    val base = value / 3
    return if (value >= 3 && base < suffix.size) {
        DecimalFormat("#0.00").format(numValue / 10.0.pow(base * 3.toDouble())) + suffix[base]
    } else {
        DecimalFormat().format(numValue)
    }
}

fun getCommonVideoFileName(userId: Int): String {
    return "${userId}_video_android_${System.currentTimeMillis()}"
}

fun getCommonPhotoFileName(userId: Int): String {
    return "${userId}_img_android_${System.currentTimeMillis()}"
}

val currentTime: Long
    get() = System.nanoTime() / 1000000

class DeactivatedAccountException(message: String?) : Exception(message)

fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
    return if (vectorDrawable != null) {
        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        BitmapDescriptorFactory.fromBitmap(bitmap)
    } else {
        null
    }
}

fun getChatMessageHeaderDateForGroup(dateString: String?): String {
    var convertTime = ""
    try {
        if (!dateString.isNullOrEmpty()) {
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            inputDateFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH)
            outputDateFormat.timeZone = TimeZone.getDefault()

            val parsedDate = inputDateFormat.parse(dateString)
            if (parsedDate != null) {
                val formattedDate = outputDateFormat.format(parsedDate)
                if (!formattedDate.isNullOrEmpty()) {
                    convertTime = formattedDate
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return convertTime
}

fun getFormattedDateForChatMessageHeader(context: Context, dateString: String?): String {
    var convertTime = getChatMessageHeaderDateForGroup(dateString)
    try {
        if (!dateString.isNullOrEmpty()) {
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            inputDateFormat.timeZone = TimeZone.getTimeZone("UTC")

            val currentDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            currentDateFormat.timeZone = TimeZone.getDefault()

            val parsedDate = inputDateFormat.parse(dateString)
            if (parsedDate != null) {
                val diffInMillis = TimeUnit.DAYS.convert(
                    currentDateFormat.calendar.timeInMillis - parsedDate.time,
                    TimeUnit.MILLISECONDS
                )
                when (diffInMillis) {
                    0L -> {
                        convertTime = context.resources.getString(R.string.today)
                    }
                    1L -> {
                        convertTime = context.resources.getString(R.string.yesterday)
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return convertTime
}

fun getFormattedTimeForChatMessage(dateString: String?): String {
    var convertTime = ""
    try {
        if (!dateString.isNullOrEmpty()) {
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            inputDateFormat.timeZone = TimeZone.getTimeZone("UTC")

            val outputDateFormat = SimpleDateFormat("hh:mm aa", Locale.ENGLISH)
            outputDateFormat.timeZone = TimeZone.getDefault()

            val parsedDate = inputDateFormat.parse(dateString)
            if (parsedDate != null) {
                val formattedDate = outputDateFormat.format(parsedDate)
                if (!formattedDate.isNullOrEmpty()) {
                    convertTime = formattedDate
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return convertTime
}

fun Bitmap.createBitmapWithBorder(context: Context, borderSize: Float, borderColor: Int, username: String, broadCastMessage: String? = null): Bitmap {
    val borderOffset = (borderSize * 2).toInt()
    val halfWidth = width / 2
    val halfHeight = height / 2
    val circleRadius = halfWidth.coerceAtMost(halfHeight).toFloat()
    val triangleHeight: Float = (borderSize * 2).toFloat()

    // Increase the drawable area width by a factor (e.g., 2 times)
    val drawableWidth = (width * 3).toInt()

    val newBitmap = Bitmap.createBitmap(
        drawableWidth + borderOffset,
        height + borderOffset + triangleHeight.toInt() + 140,
        Bitmap.Config.ARGB_8888
    )

    // Center coordinates of the widened image
    val centerX = (drawableWidth / 2).toFloat()
    val centerY = halfHeight + borderSize

    val paint = Paint()


    val canvas = Canvas(newBitmap).apply {
        // Set transparent initial area
        drawARGB(0, 0, 0, 0)
    }
    // Draw a circular mask
    val maskPaint = Paint().apply {
        isAntiAlias = true
        color = Color.WHITE
    }

    // Apply the circular mask to the original image
    val maskCanvas = Canvas(this)
    val maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val maskCanvasTemp = Canvas(maskBitmap)
    maskCanvasTemp.drawCircle(halfWidth.toFloat(), halfHeight.toFloat(), circleRadius, maskPaint)
    maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    maskCanvas.drawBitmap(maskBitmap, 0f, 0f, maskPaint)

    // Calculate the position to center the masked image
    val imageLeft = (drawableWidth - width) / 2f

    // Draw the masked image at the calculated position
    canvas.drawBitmap(this, imageLeft, borderSize, paint)

    // Draw the triangle
    val path = Path()
    path.moveTo(centerX - borderSize - 8, centerY + circleRadius)
    path.lineTo(centerX + borderSize + 8, centerY + circleRadius)
    path.lineTo(centerX , centerY + circleRadius + triangleHeight)
    path.close()

    paint.color = borderColor
    canvas.drawPath(path, paint)

    // Draw the border
    paint.xfermode = null
    paint.style = Paint.Style.STROKE
    paint.color = borderColor
    paint.strokeWidth = borderSize
    canvas.drawCircle(centerX, centerY, circleRadius, paint)

    // Add space between triangle and text
    val spaceHeight = 30 // Adjust this value as needed
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    textPaint.color = Color.WHITE
    textPaint.textSize = context.resources.getDimension(com.intuit.ssp.R.dimen._9ssp)
    textPaint.textAlign = Paint.Align.CENTER
    val textX = centerX
    val textY = centerY + circleRadius + triangleHeight + borderSize + spaceHeight // Adjust vertical position
    canvas.drawText(username, textX, textY, textPaint)

    return newBitmap
}

fun Bitmap.createBitmapWithBorder(borderSize: Float, borderColor: Int): Bitmap {
    val borderOffset = (borderSize * 2).toInt()
    val halfWidth = width / 2
    val halfHeight = height / 2
    val circleRadius = halfWidth.coerceAtMost(halfHeight).toFloat()
    val newBitmap = Bitmap.createBitmap(
        width + borderOffset,
        height + borderOffset,
        Bitmap.Config.ARGB_8888
    )

    // Center coordinates of the image
    val centerX = halfWidth + borderSize
    val centerY = halfHeight + borderSize

    val paint = Paint()
    val canvas = Canvas(newBitmap).apply {
        // Set transparent initial area
        drawARGB(0, 0, 0, 0)
    }

    // Draw the transparent initial area
    paint.isAntiAlias = true
    paint.style = Paint.Style.FILL
    canvas.drawCircle(centerX, centerY, circleRadius, paint)

    // Draw the image
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(this, borderSize, borderSize, paint)

    // Draw the createBitmapWithBorder
    paint.xfermode = null
    paint.style = Paint.Style.STROKE
    paint.color = borderColor
    paint.strokeWidth = borderSize
    canvas.drawCircle(centerX, centerY, circleRadius, paint)
    return newBitmap
}

fun Double.roundDoubleVal(decimals: Int = 2): String {
    val df = DecimalFormat("0.00")
    return  String.format("%.2f", this)
}

fun shareText(context: Context, textToShare: String) {
    ShareCompat.IntentBuilder(context)
        .setType("text/plain")
        .setChooserTitle(context.getString(R.string.app_name))
        .setText(textToShare)
        .startChooser()
}

fun getFormattedDateForEvent(eventDate: String?): String {
    var convertTime = ""
    try {
        if (!eventDate.isNullOrEmpty()) {
            val inputDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
            val outputDateFormat = SimpleDateFormat("dd MMM", Locale.ENGLISH)
            val parsedDate = inputDateFormat.parse(eventDate)
            if (parsedDate != null) {
                val formattedDate = outputDateFormat.format(parsedDate)
                if (!formattedDate.isNullOrEmpty()) {
                    convertTime = formattedDate
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return convertTime
}

fun openGoogleMapWithProvidedLatLng(context: Context, latitude: String?, longitude: String?) {
    if (!latitude.isNullOrEmpty() && !longitude.isNullOrEmpty()) {
        val data = "http://maps.google.com/maps?q=loc:$latitude,$longitude"
        val gmmIntentUri = Uri.parse(data)
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        }
    }
}

fun getSelectedTagUserIds(selectedTagUserInfo: MutableList<FollowUser>, commentString: String): String? {
    var selectedTagsUserIds: String? = null
    var selectedTags = commentString.split(" ")
    val tempTagsList = ArrayList<String>()
    for (element in selectedTags) {
        if (element.contains("@") && element.length > 1) {
            tempTagsList.addAll(element.split("@"))
        }
    }
    tempTagsList.removeAll {
        it.isEmpty() || it == "@"
    }
    selectedTags = tempTagsList
    if (!selectedTags.isNullOrEmpty()) {
        selectedTags = selectedTags.toMutableList()
        for (i in selectedTags.indices) {
            selectedTags[i] = selectedTags[i].trim()
        }
        val selectedSearchTagUserId: MutableList<Int> = mutableListOf()
        selectedTagUserInfo.forEach {
            if (it.username in selectedTags) {
                selectedSearchTagUserId.add(it.id)
            }
        }
        selectedTagsUserIds = selectedSearchTagUserId.joinToString(",")
    }
    Timber.tag("<><><><>").e(commentString.plus("\t").plus(selectedTagsUserIds))
    return selectedTagsUserIds
}


fun getSelectedTagMemberUserIds(selectedTagUserInfo: MutableList<GroupUserInfo>, commentString: String): String? {
    var selectedTagsUserIds: String? = null
    var selectedTags = commentString.split(" ")
    val tempTagsList = ArrayList<String>()
    for (element in selectedTags) {
        if (element.contains("@") && element.length > 1) {
            tempTagsList.addAll(element.split("@"))
        }
    }
    tempTagsList.removeAll {
        it.isEmpty() || it == "@"
    }
    selectedTags = tempTagsList
    if (!selectedTags.isNullOrEmpty()) {
        selectedTags = selectedTags.toMutableList()
        for (i in selectedTags.indices) {
            selectedTags[i] = selectedTags[i].trim()
        }
        val selectedSearchTagUserId: MutableList<Int> = mutableListOf()
        selectedTagUserInfo.forEach {
            if (it.username in selectedTags) {
                it.id?.let { userId ->
                    selectedSearchTagUserId.add(userId)
                }
            }
        }
        selectedTagsUserIds = selectedSearchTagUserId.joinToString(",")
    }
    Timber.tag("<><><><>").e(commentString.plus("\t").plus(selectedTagsUserIds))
    return selectedTagsUserIds
}