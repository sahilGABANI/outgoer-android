package com.outgoer.ui.chat

import android.app.ActionBar.LayoutParams
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityDisplayBinding
import com.outgoer.videoplayer.JZMediaExoKotlin

class DisplayActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_MEDIA_URL = "INTENT_EXTRA_MEDIA_URL"
        private const val INTENT_EXTRA_MEDIA_TYPE = "INTENT_EXTRA_MEDIA_TYPE"
        private const val INTENT_EXTRA_THUMB_URL = "INTENT_EXTRA_THUMB_URL"
        const val INTENT_EXTRA_MEDIA_IMAGE = "INTENT_EXTRA_MEDIA_IMAGE"
        const val INTENT_EXTRA_MEDIA_VIDEO = "INTENT_EXTRA_MEDIA_VIDEO"
        fun launchActivity(context: Context, mediaUrl: String?, mediaType: String?, thumbnailUrl: String? = null): Intent {
            val intent = Intent(context, DisplayActivity::class.java)
            intent.putExtra(INTENT_EXTRA_MEDIA_URL, mediaUrl)
            intent.putExtra(INTENT_EXTRA_MEDIA_TYPE, mediaType)
            intent.putExtra(INTENT_EXTRA_THUMB_URL, thumbnailUrl)
            return intent
        }
    }

    private lateinit var binding: ActivityDisplayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listenToViewEvent()
    }

    private fun listenToViewEvent() {
        binding.ivClose.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.ivMutePlayer.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.outgoerVideoPlayer.isVideMute) {
                binding.outgoerVideoPlayer.isVideMute = false
                binding.outgoerVideoPlayer.unMute()
                binding.ivMutePlayer.setImageResource(R.drawable.ic_post_unmute)
            } else {
                binding.outgoerVideoPlayer.isVideMute = true
                binding.outgoerVideoPlayer.mute()
                binding.ivMutePlayer.setImageResource(R.drawable.ic_post_mute)
            }
        }

        val mediaUrl = intent.getStringExtra(INTENT_EXTRA_MEDIA_URL)
        val mediaType = intent.getStringExtra(INTENT_EXTRA_MEDIA_TYPE)
        val thumbnailUrl = intent.getStringExtra(INTENT_EXTRA_THUMB_URL)

        if (!mediaUrl.isNullOrEmpty() && !mediaType.isNullOrEmpty()) {
            if (mediaType == INTENT_EXTRA_MEDIA_IMAGE) {
                binding.ivPreview.visibility = View.VISIBLE
                binding.outgoerVideoPlayer.visibility = View.GONE
                binding.ivMutePlayer.visibility = View.GONE

                Glide.with(this)
                    .load(mediaUrl)
                    .placeholder(R.color.color08163C)
                    .into(binding.ivPreview)

            } else if (mediaType == INTENT_EXTRA_MEDIA_VIDEO) {
                binding.ivPreview.visibility = View.GONE
                binding.outgoerVideoPlayer.visibility = View.VISIBLE
                binding.ivMutePlayer.visibility = View.VISIBLE

                binding.outgoerVideoPlayer.apply {
                    binding.ivMutePlayer.setImageResource(R.drawable.ic_post_unmute)

                    if (!thumbnailUrl.isNullOrEmpty()) {
                        Glide.with(this@DisplayActivity)
                            .load(thumbnailUrl)
                            .placeholder(R.color.color08163C)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                            .into(posterImageView)
                    }

                    val jzDataSource = JZDataSource(mediaUrl.replace("webp", "mp4"))
                    jzDataSource.looping = true
                    this.setUp(
                        jzDataSource,
                        Jzvd.SCREEN_NORMAL,
                        JZMediaExoKotlin::class.java
                    )
                    if (mediaUrl.contains(".m3u8")) {
                        Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE)
                    } else {
                        Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER)
                    }
                    startVideoAfterPreloading()
                }
            }
        } else {
            onBackPressed()
        }
    }

    override fun onPause() {
        Jzvd.goOnPlayOnPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        Jzvd.goOnPlayOnResume()
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        super.onDestroy()
    }
}