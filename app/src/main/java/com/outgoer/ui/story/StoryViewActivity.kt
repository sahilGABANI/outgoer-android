package com.outgoer.ui.story

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentTransaction
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bolaware.viewstimerstory.Momentz
import com.bolaware.viewstimerstory.MomentzCallback
import com.bolaware.viewstimerstory.MomentzView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.outgoer.R
import com.outgoer.application.Outgoer
import com.outgoer.base.BaseActivity
import com.outgoer.databinding.ActivityStoryViewBinding
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.videoplayer.JzvdStdOutgoer
import timber.log.Timber
import toPixel


class StoryViewActivity : BaseActivity(), MomentzCallback, GestureDetector.OnGestureListener {

    private lateinit var binding: ActivityStoryViewBinding
    private var videoUri: String = ""
    private var videoDuration: Long = 0

    private lateinit var gestureDetector: GestureDetector
    private val swipeThreshold = 100
    private val swipeVelocityThreshold = 100
    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, StoryViewActivity::class.java)
        }
    }

    fun getMediaDuration(mContext: Context, uri: String): Long {
        val uri = Uri.parse(uri)
        var millis: Long = 0

        MediaPlayer.create(mContext, uri)?.also {
            millis = if(it != null && it.duration != null) it.duration.toLong() else 0.toLong()

            it.reset()
            it.release()
        }

        return millis/1000
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStoryViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        gestureDetector = GestureDetector(this)

//        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
//        ft.replace(R.id.storyFrameInfo, StoryListFragment.newInstance())
//        ft.commit()
        videoUri = "https://cloudflarestream.com/94245fd463bb7e914919d50433ce9966/manifest/video.m3u8"
        videoDuration = getMediaDuration(this@StoryViewActivity, videoUri)


//        getVideoDurationInMilliseconds(this, videoUri) { duration ->
//            // Handle the video duration here
//            Timber.tag("DurDur").d("getVideoDuration: $duration")
//            videoDuration = duration/1000
//        }
//        videoUri = "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"
        Outgoer.exoCacheManager.prepareCacheVideo(videoUri)
        initUI()
    }

    private fun initUI() {
// show a textview
        val textView = TextView(this)
        textView.text = "Hello, You can display TextViews"
        textView.textSize = 20f.toPixel(this).toFloat()
        textView.gravity = Gravity.CENTER
        textView.setTextColor(Color.parseColor("#ffffff"))

        //show a customView
        val customView = LayoutInflater.from(this).inflate(R.layout.custom_view, null)

        // show an imageview be loaded from file
        val locallyLoadedImageView = ImageView(this)
        locallyLoadedImageView.setImageDrawable(
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_new_logo
            )
        )

        //image to be loaded from the internet
        val internetLoadedImageView = ImageView(this)

        //video to be loaded from the internet
        val jzvdStdOutgoer = JzvdStdOutgoer(this)
        jzvdStdOutgoer.videoUrl = videoUri

        Timber.tag("DurDur").d("duration: $videoDuration")

        val listOfViews = listOf(
            MomentzView(textView, 10),
            MomentzView(customView, 5),
            MomentzView(locallyLoadedImageView, 10),
            MomentzView(internetLoadedImageView, 15),
            MomentzView(jzvdStdOutgoer, videoDuration.toInt())
        )

        Momentz(this, listOfViews, binding.storyContainer, this).start()
    }

    override fun done() {
        Toast.makeText(this@StoryViewActivity, "Finished!", Toast.LENGTH_LONG).show()
    }

    override fun onNextCalled(view: View, momentz: Momentz, index: Int) {
        if (view is JzvdStdOutgoer) {
            momentz.pause(true)
            playVideo(view, index, momentz)
        } else if ((view is ImageView) && (view.drawable == null)) {
            momentz.pause(true)

            Glide.with(this@StoryViewActivity)
                .load("https://i.pinimg.com/564x/14/90/af/1490afa115fe062b12925c594d93a96c.jpg")
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(view)
        }
    }

    private fun playVideo(videoView: JzvdStdOutgoer, index: Int, momentz: Momentz) {
        Timber.tag("DurDur").d("playVideo -> videoDuration: $videoDuration")
        momentz.editDurationAndResume(index, videoDuration.toInt())
        videoView.videoUrl = videoUri
        videoView.apply {
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = false
            this.setUp(
                jzDataSource, Jzvd.SCREEN_NORMAL, JZMediaExoKotlin::class.java
            )

            // Start playing the video after setup
            startVideoAfterPreloading()
        }
    }

    private fun getVideoDurationInMilliseconds(context: Context, videoUrl: String, callback: (Long) -> Unit) {
        try {
            val exoPlayer = ExoPlayer.Builder(context).build()

            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()

            exoPlayer.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        val durationInMilliseconds: Long = exoPlayer.duration
                        exoPlayer.removeListener(this)
                        exoPlayer.release()

                        callback(durationInMilliseconds)
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            callback(0L)
        }
    }

    // Override this method to recognize touch event
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (gestureDetector.onTouchEvent(event)) {
            true
        }
        else {
            super.onTouchEvent(event)
        }
    }

    override fun onDown(p0: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(p0: MotionEvent) {
        return
    }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    override fun onLongPress(p0: MotionEvent) {
        return
    }

    override fun onFling(p0: MotionEvent, p1: MotionEvent, velocityX: Float, velocityY: Float): Boolean {

        try {
            val diffY = p1.y - p0.y
            val diffX = p1.x - p0.x

            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > swipeThreshold && Math.abs(velocityX) > swipeVelocityThreshold) {
                    if (diffX > 0) {
                        Toast.makeText(this@StoryViewActivity, "Left to Right swipe gesture", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(this@StoryViewActivity, "Right to Left swipe gesture", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        catch (exception: Exception) {
            exception.printStackTrace()
        }
        return true
    }
}