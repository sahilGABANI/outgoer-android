package com.outgoer.ui.add_location

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.BuildConfig
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityAddLocationBinding
import com.outgoer.ui.postlocation.AddPostLocationActivity
import com.outgoer.ui.sponty.location.PlaceSearch
import com.outgoer.ui.sponty.location.model.PlaceSearchResponse
import com.outgoer.ui.venue.view.VenueLocationAdapter
import com.outgoer.utils.UiUtils
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AddLocationActivity : BaseActivity() {

    private lateinit var venueLocationAdapter: VenueLocationAdapter
    private var currentLocation: String? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private lateinit var binding:ActivityAddLocationBinding

    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, AddLocationActivity::class.java)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@AddLocationActivity)

        locationPermission()
        listenToViewEvents()
    }


    private fun listenToViewEvents() {
        binding.llSearch.visibility = View.VISIBLE
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this@AddLocationActivity)

        placeSearch("")
        venueLocationAdapter = VenueLocationAdapter(this@AddLocationActivity).apply {
            venueAvailableClick.subscribeAndObserveOnMainThread {
                var intent = Intent()
                intent.putExtra(AddPostLocationActivity.INTENT_EXTRA_LATITUDE, it.geometry?.location?.lat)
                intent.putExtra(AddPostLocationActivity.INTENT_EXTRA_LONGITUDE, it.geometry?.location?.lng)
                intent.putExtra(AddPostLocationActivity.INTENT_EXTRA_LOCATION, it.formattedAddress)
                intent.putExtra(AddPostLocationActivity.INTENT_PLACE_ID, it.placeId)

                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
        binding.dataRecyclerView.apply {
            adapter = venueLocationAdapter
        }

        binding.etSearch.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this@AddLocationActivity)
            }.autoDispose()

        binding.etSearch.textChanges()
            .skipInitialValue()
            .doOnNext {
                if (it.isNullOrEmpty()) {
                    binding.ivClear.visibility = View.INVISIBLE
                } else {
                    binding.ivClear.visibility = View.VISIBLE
                }
            }
            .debounce(300, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.length > 2) {
                    placeSearch(it.toString())
                }
//                UiUtils.hideKeyboard(this@AddLocationActivity)
            }, {
                Timber.e(it)
            }).autoDispose()


        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this@AddLocationActivity)
            binding.etSearch.setText("")
            binding.tvNoLocation.isVisible = false
        }.autoDispose()
    }

    private fun placeSearch(search: String) {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val okhttpBuilder = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(240, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
            okhttpBuilder.addInterceptor(loggingInterceptor)
        }
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okhttpBuilder.build())
            .build()

        val retrofitService = retrofit.create(PlaceSearch::class.java)

        retrofitService.getPlacesLists("AIzaSyBxVTyN0YmOdGY4e92OObhtYCXC0VtxLB8", search, currentLocation ?: "", 2000)
            .enqueue(object :
                Callback<PlaceSearchResponse> {
                override fun onResponse(
                    call: Call<PlaceSearchResponse>,
                    response: Response<PlaceSearchResponse>
                ) {
                    Log.e("Response %s", response.body()?.toString() ?: "")
                    response.body()?.results?.let {
                        venueLocationAdapter.listoflocation = it
                        if(search != "") {
                            binding.tvNoLocation.isVisible = venueLocationAdapter.listoflocation.isNullOrEmpty()
                        }

                        UiUtils.hideKeyboard(this@AddLocationActivity)
                    }
                }

                override fun onFailure(call: Call<PlaceSearchResponse>, t: Throwable) {
                    Log.e("MainActivity", t.toString())
                }
            })
    }

    private fun locationPermission() {
        XXPermissions.with(this)
            .permission(Permission.ACCESS_COARSE_LOCATION)
            .permission(Permission.ACCESS_FINE_LOCATION)
            .request(object : OnPermissionCallback {
                @SuppressLint("MissingPermission")
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    val task = fusedLocationProviderClient.lastLocation
                    task.addOnSuccessListener { location ->
                        location?.let {
                            val lat = it.latitude
                            val longi = it.longitude
                            currentLocation = "${lat}, ${longi}"

                            placeSearch("cafe,bar,restaurants,hotel")
                        }
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    val locationPermissionGranted = false
                }
            })
    }
}