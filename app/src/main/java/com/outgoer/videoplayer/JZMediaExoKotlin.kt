package com.outgoer.videoplayer

import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import cn.jzvd.JZMediaInterface
import cn.jzvd.Jzvd
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultAllocator
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.video.VideoSize
import com.outgoer.application.Outgoer
import timber.log.Timber

class JZMediaExoKotlin(jzvd: Jzvd) : JZMediaInterface(jzvd), Player.Listener {

    private var exoPlayer: ExoPlayer? = null
    private var callback: Runnable? = null
    private var previousSeek: Long = 0

    override fun start() {
        exoPlayer?.playWhenReady = true
    }

    override fun prepare() {
        val context = jzvd.context
        release()
        mMediaHandlerThread = HandlerThread("JZVD")
        mMediaHandlerThread.start()
        mMediaHandler = Handler(context.mainLooper) //主线程还是非主线程，就在这里
        handler = Handler()
        mMediaHandler.post {
            val trackSelector: TrackSelector =
                DefaultTrackSelector(context, AdaptiveTrackSelection.Factory())
            val loadControl: DefaultLoadControl.Builder = DefaultLoadControl.Builder()
                .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
                .setBufferDurationsMs(3000, 60000, 1000, 3000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .setTargetBufferBytes(C.LENGTH_UNSET)
            val bandwidthMeter: BandwidthMeter = DefaultBandwidthMeter.Builder(context).build()

            val renderersFactory: RenderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
            if (exoPlayer == null) {
                exoPlayer = ExoPlayer.Builder(context, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadControl.build())
                    .setBandwidthMeter(bandwidthMeter)
                    .build()
                val isLoop = jzvd.jzDataSource.looping
                exoPlayer?.repeatMode = if (isLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                exoPlayer?.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                jzvd.textureView?.surfaceTexture?.let {
                    exoPlayer?.setVideoSurface(Surface(it))
                }
                exoPlayer?.addListener(this)
                exoPlayer?.playWhenReady = true
            }
            val currUrl = jzvd.jzDataSource.currentUrl.toString()

            val mediaSource = if (currUrl.contains(".m3u8")) {
                HlsMediaSource.Factory(Outgoer.cacheDataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(
                        MediaItem.Builder()
                            .setUri(currUrl)
                            .build()
                    )
            } else {
                ProgressiveMediaSource.Factory(Outgoer.cacheDataSourceFactory)
                    .createMediaSource(MediaItem.Builder()
                        .setUri(currUrl)
                        .build())
            }
            Timber.i("URL Link = %s", currUrl)
            exoPlayer?.setMediaSource(mediaSource)
            exoPlayer?.prepare()
            callback = OnBufferingUpdate()
        }
    }

    override fun pause() {
        exoPlayer?.playWhenReady = false
    }

    override fun isPlaying(): Boolean {
        return exoPlayer?.playWhenReady ?: false
    }

    override fun seekTo(time: Long) {
        if (exoPlayer == null) {
            return
        }
        if (time != previousSeek) {
            if (time >= (exoPlayer?.bufferedPosition ?: 0)) {
                jzvd.onStatePreparingPlaying()
            }
            exoPlayer?.seekTo(time)
            previousSeek = time
            jzvd.seekToInAdvance = time
        }
    }

    override fun release() {
        if (mMediaHandler != null && mMediaHandlerThread != null && exoPlayer != null) { //不知道有没有妖孽
            val tmpHandlerThread = mMediaHandlerThread
            val tmpMediaPlayer: ExoPlayer = exoPlayer ?: return
            SAVED_SURFACE = null
            mMediaHandler.post {
                tmpMediaPlayer.release() //release就不能放到主线程里，界面会卡顿
                tmpHandlerThread.quit()
            }
            exoPlayer = null
        }
    }

    override fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0
    }

    override fun getDuration(): Long {
        return exoPlayer?.duration ?: 0
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        exoPlayer?.volume = leftVolume
        exoPlayer?.volume = rightVolume
    }

    override fun setSpeed(speed: Float) {
        val playbackParameters = PlaybackParameters(speed, 1.0f)
        exoPlayer?.playbackParameters = playbackParameters
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        handler.post {
            when (playbackState) {
                Player.STATE_IDLE -> {
                    Timber.tag("VideoPlayer").e("STATE_IDLE")
                }
                Player.STATE_BUFFERING -> {
                    Timber.tag("VideoPlayer").w("STATE_BUFFERING")
                    jzvd.onStatePreparingPlaying()
                    handler.post(callback!!)
                }
                Player.STATE_READY -> {
                    Timber.tag("VideoPlayer").i("STATE_READY")
                    if (playWhenReady) {
                        jzvd.onStatePlaying()
                        jzvd.alpha = 1f
                    }
                }
                Player.STATE_ENDED -> {
                    Timber.tag("VideoPlayer").e("STATE_ENDED")
                    jzvd.onCompletion()
                }
            }
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Timber.tag("VideoPlayer").e("error: $error")
        handler.post { jzvd.onError(1000, 1000) }
    }

    override fun onPositionDiscontinuity(
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int
    ) {
        when(reason) {
            Player.DISCONTINUITY_REASON_SKIP -> {
                handler.post { jzvd.onSeekComplete() }
            }
            else -> {

            }
        }
    }

    override fun setSurface(surface: Surface) {
        if (exoPlayer != null) {
            exoPlayer?.setVideoSurface(surface)
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        if (SAVED_SURFACE == null) {
            SAVED_SURFACE = surface
            prepare()
        } else {
            jzvd.textureView.setSurfaceTexture(SAVED_SURFACE)
        }
    }

    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

    }

    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
        Timber.tag("VideoPlayer").e("onSurfaceTextureDestroyed")
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        Timber.tag("VideoPlayer").w("onSurfaceTextureUpdated")
    }

    private inner class OnBufferingUpdate : Runnable {
        override fun run() {
            if (exoPlayer != null) {
                val percent = exoPlayer?.bufferedPercentage ?: 0
                handler.post { jzvd.setBufferProgress(percent) }
                if (percent < 100) {
                    handler.postDelayed(callback!!, 300)
                } else {
                    handler.removeCallbacks(callback!!)
                }
            }
        }
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        handler.post { jzvd.onVideoSizeChanged(videoSize.width, videoSize.height) }
    }
}