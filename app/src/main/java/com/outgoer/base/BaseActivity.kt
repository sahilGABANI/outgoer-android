package com.outgoer.base

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.outgoer.api.venue.model.GeoFenceResponse
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.CheckInBottomSheet
import com.outgoer.ui.nointernet.NoInternetActivity
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import timber.log.Timber

abstract class BaseActivity : AppCompatActivity() {

    companion object {
        var PLACE_API_KEY = "AIzaSyCC_Nu3RvrGB8WId3Wazw_VWoD17u2eGI4"
        var MAX_RADIUS = 2000
        var isTransactionSafe = false
        var isNoInternetActivityOpened = true
    }

    private val compositeDisposable = CompositeDisposable()
    private var checkInBottomSheet: CheckInBottomSheet? = null
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private var isNetworkCallbackRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback = createNetworkCallback()


        if (!isNetworkCallbackRegistered) {
            val networkRequest = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
            isNetworkCallbackRegistered = true
        }

        if (!isConnectedToInternet()) {
            openNoInternetConnectionActivity()
        }
    }

    override fun onDestroy() {
        if (isNetworkCallbackRegistered) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            isNetworkCallbackRegistered = false
        }
        compositeDisposable.clear()
        super.onDestroy()
    }

    fun Disposable.autoDispose() {
        compositeDisposable.add(this)
    }

    override fun onPostResume() {
        super.onPostResume()
        isTransactionSafe = true
    }

    override fun onPause() {
        super.onPause()
        isTransactionSafe = false
    }

    fun openBottomSheet(geoFence: GeoFenceResponse?, isCheckIn: Boolean) {
        if (isTransactionSafe && checkInBottomSheet == null) {
            if(geoFence != null && !geoFence.name.isNullOrEmpty()) {
                checkInBottomSheet = CheckInBottomSheet.newInstance(geoFence, isCheckIn).apply {
                    dismissClick.subscribeAndObserveOnMainThread {
                        dismiss()
                        checkInBottomSheet = null
                    }.autoDispose()
                }
                checkInBottomSheet?.let {
                    val fragmentTransaction = supportFragmentManager.beginTransaction()
                    fragmentTransaction.add(it, "requestDialog")
                    fragmentTransaction.commit()
                }
            }
        }
    }

    private fun isConnectedToInternet(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun openNoInternetConnectionActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            Timber.tag("NoInternet").e("BaseActivity: isNoInternetActivityOpened: $isNoInternetActivityOpened")
            if (isNoInternetActivityOpened) {
                isNoInternetActivityOpened = false
                Timber.tag("NoInternet").e("No Internet")
                val intent = Intent(this, NoInternetActivity::class.java)
                startActivity(intent)
            }
        }, 2000)
    }

    private fun createNetworkCallback() = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            openNoInternetConnectionActivity()
        }
    }
}