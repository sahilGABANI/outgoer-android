package com.outgoer.ui.fullscreenimage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.MediaController
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.MimeTypes
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityFullScreenImageBinding
import com.outgoer.videoplayer.JZMediaExoKotlin
import com.outgoer.videoplayer.JzvdStdOutgoer


class FullScreenImageActivity : BaseActivity() {

    lateinit var binding: ActivityFullScreenImageBinding

    companion object {
        private const val PATH = "path"

        fun getIntent(context: Context, url: String): Intent {
            val intent = Intent(context, FullScreenImageActivity::class.java)
            intent.putExtra(PATH, url)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFullScreenImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val path = intent.extras!!.getString(PATH, "")
        val uri = Uri.parse(path + "?clientBandwidthHint=2.5")

        binding.playerView.setVideoURI(uri)

        // creating object of
        // media controller class
        val mediaController = MediaController(this)

        // sets the anchor view
        // anchor view for the videoView
        mediaController.setAnchorView(binding.playerView)

        // sets the media player to the videoView
        mediaController.setMediaPlayer(binding.playerView)

        // sets the media controller to the videoView
        binding.playerView.setMediaController(mediaController);

        // starts the video
        binding.playerView.start();

        Log.e("FullScreenImageActivity path", path)

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
    }

    override fun onPause() {
        super.onPause()
        Jzvd.goOnPlayOnPause()
        binding.playerView.pause()
    }

    override fun onDestroy() {
        Jzvd.goOnPlayOnPause()
        binding.playerView.stopPlayback()
        super.onDestroy()
    }
}