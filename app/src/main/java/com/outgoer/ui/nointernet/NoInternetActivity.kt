package com.outgoer.ui.nointernet

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.outgoer.base.BaseActivity.Companion.isNoInternetActivityOpened
import com.outgoer.databinding.ActivityNoInternetBinding
import com.outgoer.utils.SnackBarUtils.showTopSnackBar
import timber.log.Timber

class NoInternetActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoInternetBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isNoInternetActivityOpened = false
        initUI()
    }

    private fun initUI() {
        binding.mbTryAgain.setOnClickListener {
            if (isConnectedToInternet()) {
                onBackPressedDispatcher.onBackPressed()
            } else {
                showTopSnackBar(findViewById(android.R.id.content))
            }
        }
    }

    private fun isConnectedToInternet(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    override fun onBackPressed() {
        if (isConnectedToInternet()) {
            onBackPressedDispatcher.onBackPressed()
        } else {
            showTopSnackBar(findViewById(android.R.id.content))
        }
    }

    override fun onDestroy() {
        isNoInternetActivityOpened = true
        Timber.tag("NoInternet").e("NoInternetActivity: isNoInternetActivityOpened: $isNoInternetActivityOpened")
        super.onDestroy()
    }
}