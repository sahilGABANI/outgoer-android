package com.outgoer.application

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.exoplayer2.database.StandaloneDatabaseProvider
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.firebase.FirebaseApp
import com.outgoer.base.ActivityManager
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.cache.ExoCacheManager
import com.outgoer.di.DaggerOutgoerAppComponent
import com.outgoer.di.OutgoerAppComponent
import com.outgoer.di.OutgoerAppModule
import com.outgoer.ui.home.newmap.venuemap.cache.BitmapCache
import timber.log.Timber
import java.io.File


class Outgoer : OutgoerApplication() {
    companion object {
        private const val CACHE_SIZE = 300 * 1024 * 1024L
        private var cacheInstance: SimpleCache? = null
        private const val PRE_CACHE_SIZE = 512 * 1024L
        private const val TAG = "Outgoer"
        operator fun get(app: Application): Outgoer {
            return app as Outgoer
        }

        operator fun get(activity: Activity): Outgoer {
            return activity.application as Outgoer
        }

        lateinit var component: OutgoerAppComponent
            private set

        lateinit var cache: SimpleCache
            private set

        lateinit var mapCache: BitmapCache
            private set

        @SuppressLint("StaticFieldLeak")
        lateinit var upstreamDataSourceFactory: DefaultDataSourceFactory
            private set

        @SuppressLint("StaticFieldLeak")
        lateinit var cacheDataSourceFactory: CacheDataSource.Factory
            private set

        @SuppressLint("StaticFieldLeak")
        lateinit var exoCacheManager: ExoCacheManager
            private set
    }


    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            component = DaggerOutgoerAppComponent.builder()
                .outgoerAppModule(OutgoerAppModule(this))
                .build()
            component.inject(this)
            super.setAppComponent(component)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        observeSocket()

        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleEventObserver)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        ActivityManager.getInstance().init(this)
        cache = _cache
        exoCacheManager = _exoCacheManager
        upstreamDataSourceFactory = _upstreamDataSourceFactory
        cacheDataSourceFactory = _cacheDataSourceFactory
        mapCache = BitmapCache(BitmapCache.cacheSize)
    }

    private var lifecycleEventObserver = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                Timber.tag("AppLifecycleListener").e("App moved to background")
                socketDataManager.disconnect()
            }

            Lifecycle.Event.ON_START -> {
                Timber.tag("AppLifecycleListener").e("App moved to foreground")
                socketDataManager.connect()
            }

            Lifecycle.Event.ON_DESTROY -> {
                Timber.tag("AppLifecycleListener").e("App killed")
                socketDataManager.disconnect()
            }

            else -> {}
        }
    }

    private fun observeSocket() {
        socketDataManager.connectionEmitter().subscribeOnIoAndObserveOnMainThread({

        }, {
            Timber.e(it)
        })
        socketDataManager.connectionError().subscribeOnIoAndObserveOnMainThread({}, {
            Timber.tag("Socket Manager").e(it.message)
            Timber.tag("Socket Manager").e(it.localizedMessage)
        })
        socketDataManager.disconnectEmitter().subscribeOnIoAndObserveOnMainThread({}, {
            Timber.e(it)
        })
    }

    private val _cache by lazy {
        return@lazy cacheInstance ?: run {
            val exoCacheDir = File("${this.cacheDir.absolutePath}/exo")
            Timber.tag("Play").e("File ${exoCacheDir.path}")
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            SimpleCache(exoCacheDir, evictor, StandaloneDatabaseProvider(this)).also {
                cacheInstance = it
            }
        }
    }

    private val _exoCacheManager by lazy {
        ExoCacheManager()
    }

    private val _upstreamDataSourceFactory by lazy { DefaultDataSourceFactory(this) }

    private val _cacheDataSourceFactory by lazy {
        val cacheSink = CacheDataSink.Factory()
            .setCache(_cache)


        CacheDataSource.Factory()
            .setCache(_cache)
            .setCacheWriteDataSinkFactory(cacheSink)
            .setUpstreamDataSourceFactory(_upstreamDataSourceFactory)
            .setCacheReadDataSourceFactory(FileDataSource.Factory())
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setEventListener(object : CacheDataSource.EventListener {
                override fun onCachedBytesRead(cacheSizeBytes: Long, cachedBytesRead: Long) {

                }

                override fun onCacheIgnored(reason: Int) {
                    Timber.tag(TAG).e("onCacheIgnored. reason:$reason")
                }
            })
    }
}