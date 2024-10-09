package com.outgoer.cache

import android.net.Uri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.offline.HlsDownloader
import kotlinx.coroutines.*
import timber.log.Timber
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.outgoer.application.Outgoer.Companion.cache
import com.outgoer.application.Outgoer.Companion.cacheDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class VideoPrefetch(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "PREFETCH"
        private const val PRE_CACHE_SIZE_WIFI = 3 * 1024 * 1024L
        private const val PRE_CACHE_SIZE_MOBILE = 1024 * 1024L
    }

    fun prefetchHlsVideo(uri: Uri) {
        Timber.tag(TAG).d("prefetchHlsVideo() -> uri: $uri")
        val prefetchSize = if (isConnectedToWifi()) PRE_CACHE_SIZE_WIFI else PRE_CACHE_SIZE_MOBILE
        val prefetch = HlsPrefetch(uri, prefetchSize)
        coroutineScope.launch {
            try {
                prefetch.prefetchVideo()
            } catch (e: Exception) {
                Timber.tag(TAG).e("Exception in prefetchHlsVideo: $e")
            }
        }
    }

    private fun isConnectedToWifi(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private class HlsPrefetch(
        private val url: Uri,
        private val prefetchSize: Long
    ) {
        private val downloader: HlsDownloader =
            HlsDownloader(MediaItem.fromUri(url), cacheDataSourceFactory)

        suspend fun prefetchVideo() = withContext(Dispatchers.IO) {
            try {
                runCatching {
                    if (cache.isCached(url.toString(), 0, prefetchSize)) {
                        return@runCatching
                    }
                    downloader.download { _, bytesDownloaded, percentDownloaded ->
                        if (bytesDownloaded >= prefetchSize) {
                            downloader.cancel()
                        }
                        Timber.tag(TAG)
                            .d("bytesDownloaded: $bytesDownloaded, percentDownloaded: $percentDownloaded")
                    }
                    Timber.tag(TAG).d("Dispatching")
                }.onFailure {
                    if (it is CancellationException || it is InterruptedException) {
                        return@onFailure
                    }
                    Timber.tag(TAG).e("Error on $url: $it")
                }.onSuccess {
                    Timber.tag(TAG).d("Success")
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e("Exception in prefetchVideo: $e")
            }
        }
    }
}