package com.outgoer.application

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.AssetManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.outgoer.BuildConfig
import com.outgoer.base.ActivityManager
import com.outgoer.di.BaseAppComponent
import com.outgoer.di.BaseUiApp
import com.outgoer.socket.SocketDataManager
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import timber.log.Timber
import javax.inject.Inject

@SuppressLint("Registered")
open class OutgoerApplication : BaseUiApp() {

    @Inject
    lateinit var socketDataManager: SocketDataManager

    companion object {
        lateinit var component: BaseAppComponent
        var assetManager: AssetManager? = null


        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }

    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        context = this
        ActivityManager.getInstance().init(this)
        setupLog()
        initInstallTime()

    }

    private fun setupLog() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
    }

    private fun initInstallTime() {
        try {
            val context = createPackageContext("com.outgoer.app", 0)
            assetManager = context.assets
        } catch (e: PackageManager.NameNotFoundException) {
            Timber.e("AssetManager $e")
        }
    }

    override fun getAppComponent(): BaseAppComponent {
        return component
    }

    override fun setAppComponent(baseAppComponent: BaseAppComponent) {
        component = baseAppComponent
    }   

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
        EmojiManager.install(GoogleEmojiProvider())
    }
}