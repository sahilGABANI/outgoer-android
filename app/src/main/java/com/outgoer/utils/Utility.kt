package com.outgoer.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.outgoer.R
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.application.OutgoerApplication.Companion.context
import com.outgoer.ui.story.StoryInfoActivity
import com.outgoer.videoplayer.JzvdStdOutgoer
import timber.log.Timber
import java.io.File
import java.util.ArrayList
import java.util.Random

object Utility {
    val ringGradientColor = ContextCompat.getDrawable(context, R.drawable.ring_gredient_color)
    val ringBlueGradientColor =
        ContextCompat.getDrawable(context, R.drawable.ring_blue_gredient_color)
    var isSpontyOpen: Boolean = false
    val storyListUtil: ArrayList<StoryListResponse> = arrayListOf()
    val prefetchedUrls = mutableSetOf<String>()
    @SuppressLint("StaticFieldLeak")
    var player: JzvdStdOutgoer ?= null
    var previousFeedViewPosition = 1
    var firstVisiblePosition: Int = 0

    interface TabItemClickListener {
        fun onTabItemClicked(tabType: String)
    }

    fun getImageDimensions(
        context: Context,
        imageUrl: String,
        callback: (width: Int, height: Int) -> Unit
    ) {
        val imageLoader = ImageLoader.Builder(context)
            .build()

        val request = ImageRequest.Builder(context)
            .data(imageUrl)
            .target { drawable ->
                val width = drawable.intrinsicWidth
                val height = drawable.intrinsicHeight
                callback(width, height)
            }
            .build()
        imageLoader.enqueue(request)
    }

    fun generateRandomString(length: Int): String {
        val charPool: List<Char> = ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { _ -> Random().nextInt(charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    fun getConvertedFile(directoryPath: String, fileName: String): File {
        val directory = File(directoryPath)
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File(directory, fileName)
    }

    fun toggleSelectedStory(
        context: Context,
        storyListUtil: ArrayList<StoryListResponse>,
        userId: Int
    ) {
        storyListUtil.forEachIndexed { index, storyList ->
            Timber.tag("ToggleSelectedStory")
                .d("storyList index: $index, isSelected: ${storyList.isSelected}")
            storyList.stories.forEach { story ->
                Timber.tag("ToggleSelectedStory").d("story userId: ${story.userId}")
            }
        }

        val index = storyListUtil.indexOfFirst { storyList ->
            storyList.stories.any { story -> story.userId == userId }
        }

        Timber.tag("ToggleSelectedStory").d("index: $index")
        if (index != -1) {
            storyListUtil.forEach { storyList ->
                storyList.isSelected = false
            }
            storyListUtil[index].isSelected = !storyListUtil[index].isSelected
            context.startActivity(
                StoryInfoActivity.getIntent(
                    context,
                    storyListUtil
                )
            )
        } else {
            Timber.tag("ToggleSelectedStory").e("No matching userId found in storyListUtil.")
        }
    }
}