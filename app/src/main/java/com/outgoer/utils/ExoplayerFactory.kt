package com.outgoer.utils

import android.content.Context
import android.net.Uri
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.offline.StreamKey
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.SimpleCache


class ExoplayerFactory(var context: Context) {

    fun build(playListener: Player.Listener): ExoPlayer {
        val exoPlayer = ExoPlayer.Builder(context).build()
        exoPlayer.addListener(playListener)
        exoPlayer.seekTo(0)
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE
        return exoPlayer
    }

}

class VideoMediaSourceFactory(var dataSourceFactory: DataSource.Factory) {

    fun createMediaSource(url: Uri): BaseMediaSource {
        val mediaSource: BaseMediaSource = HlsMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(url))
        return mediaSource
    }
}

class ExoPlayerItem(
    var exoPlayer: ExoPlayer,
    var position: Int
)