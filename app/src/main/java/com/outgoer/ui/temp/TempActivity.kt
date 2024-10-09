@file:Suppress("DEPRECATION")

package com.outgoer.ui.temp

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import cn.jzvd.JZDataSource
import cn.jzvd.Jzvd
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityTempBinding
import com.outgoer.videoplayer.JzvdStdOutgoer
import io.reactivex.Observable
import java.util.concurrent.TimeUnit

class TempActivity : BaseActivity() {

    companion object {
        private const val PATH = "path"
        private const val THUMBNAIL_URL = "THUMBNAIL_URL"

        fun getIntent(context: Context, url: String, thumbnailUrl: String?): Intent {
            val intent = Intent(context, TempActivity::class.java)
            intent.putExtra(PATH, url)
            intent.putExtra(THUMBNAIL_URL, thumbnailUrl)
            return intent
        }
    }

    private lateinit var binding: ActivityTempBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTempBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val path = intent.extras?.getString(PATH, "")
        val thumbnailUrl = intent.extras?.getString(THUMBNAIL_URL, "")

        if (path != null) {
            Observable.timer(1000,TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                autoPlayVideo(path)
            }.autoDispose()
        }
        binding.progressImageProgressBar.isVisible = true
        binding.exoPlayer.posterImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.exoPlayer.posterImageView?.let {
            Glide.with(this@TempActivity)
                .load(thumbnailUrl).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .centerInside()
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding?.progressImageProgressBar?.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        binding?.progressImageProgressBar?.visibility = View.GONE
                        return false
                    }
                })
                .into(it)
        }
        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()
    }

    private fun autoPlayVideo(path: String) {

        val player: JzvdStdOutgoer = findViewById(R.id.exoPlayer)
        player.videoUrl = path.plus("?clientBandwidthHint=2.5")
        player.apply {
            val jzDataSource = JZDataSource(this.videoUrl)
            jzDataSource.looping = true
            this.setUp(
                jzDataSource,
                Jzvd.SCREEN_NORMAL,
            )
            startVideoAfterPreloading()
        }

        Jzvd.setVideoImageDisplayType(Jzvd.VIDEO_IMAGE_DISPLAY_TYPE_ADAPTER)
    }

    override fun onResume() {
        super.onResume()
        Jzvd.goOnPlayOnResume()

    }

    override fun onPause() {
        Jzvd.goOnPlayOnPause()
        super.onPause()
    }

    override fun onDestroy() {
        Jzvd.releaseAllVideos()
        super.onDestroy()
    }
}