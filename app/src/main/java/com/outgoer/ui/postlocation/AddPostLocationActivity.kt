package com.outgoer.ui.postlocation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.BuildConfig
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityAddPostLocationBinding
import com.outgoer.ui.postlocation.view.AddLocationAdapter
import com.outgoer.ui.postlocation.view.PlacesAdapter
import com.outgoer.ui.sponty.location.PlaceSearch
import com.outgoer.ui.sponty.location.model.PlaceSearchResponse
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
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit


class AddPostLocationActivity : BaseActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    companion object {
        const val DEFAULT_ZOOM = 10f
        const val INTENT_EXTRA_LATITUDE = "INTENT_EXTRA_LATITUDE"
        const val INTENT_EXTRA_LONGITUDE = "INTENT_EXTRA_LONGITUDE"
        const val INTENT_EXTRA_LOCATION = "INTENT_EXTRA_LOCATION"
        const val INTENT_PLACE_ID = "INTENT_PLACE_ID"
        fun launchActivity(context: Context): Intent {
            return Intent(context, AddPostLocationActivity::class.java)
        }
    }

    private lateinit var binding: ActivityAddPostLocationBinding

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationPermissionGranted = false

    private var latitude = 0.toDouble()
    private var longitude = 0.toDouble()
    private var location = ""
    private var vectorMarkerBitmap: BitmapDescriptor? = null

    private var currentLocation: String? = null

    private lateinit var placesAdapter: PlacesAdapter
    private lateinit var addLocationAdapter: AddLocationAdapter

    private var placeId: String? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddPostLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        listenToViewEvents()
        locationPermission()
        searchLocationPlaces()
    }

    private fun listenToViewEvents() {

        vectorMarkerBitmap =
            bitmapDescriptorFromVector(this, R.drawable.ic_post_location_marker_vector)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.btnConfirmLocation.throttleClicks().subscribeAndObserveOnMainThread {
            val intent = Intent()
            intent.putExtra(INTENT_EXTRA_LATITUDE, latitude)
            intent.putExtra(INTENT_EXTRA_LONGITUDE, longitude)
            intent.putExtra(INTENT_EXTRA_LOCATION, location)

            placeId?.let {
                intent.putExtra(INTENT_PLACE_ID, it)
            }
            setResult(Activity.RESULT_OK, intent)
            finish()
        }.autoDispose()

        placesAdapter = PlacesAdapter(this).apply {
            placesActionState.subscribeAndObserveOnMainThread {

                binding.locationSearchRecyclerView.visibility = View.GONE
                it.geometry?.location?.let { loc ->
                    val latLng = LatLng(loc.lat, loc.lat)
                    latitude = loc.lat
                    longitude = loc.lng
                    location = it.formattedAddress ?: ""

                    binding.llFooter.visibility = View.VISIBLE
                    binding.tvMainLocation.text = location

                }
            }
        }

        addLocationAdapter = AddLocationAdapter(this).apply {
            placesActionState.subscribeAndObserveOnMainThread {
                binding.locationSearchRecyclerView.visibility = View.GONE

                location = it.formattedAddress ?: ""
                placeId = it.placeId

                mMap.clear()

                it.geometry?.location?.let { loc ->
                    val latLng = LatLng(loc.lat, loc.lng)
                    setMarker(latLng)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                }

                binding.llFooter.visibility = View.VISIBLE
                binding.tvMainLocation.text = location

            }
        }

        binding.locationSearchRecyclerView.apply {
            adapter = placesAdapter
        }

        binding.nearbyLocationSearchRecyclerView.apply {
            adapter = addLocationAdapter
        }
    }

    private fun locationPermission() {
        XXPermissions.with(this)
            .permission(Permission.ACCESS_COARSE_LOCATION)
            .permission(Permission.ACCESS_FINE_LOCATION)
            .request(object : OnPermissionCallback {
                @SuppressLint("MissingPermission")
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    locationPermissionGranted = all
                    initMap()
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
                    locationPermissionGranted = false
                    initMap()
                }
            })
    }

    private fun initMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        p0.setOnMapClickListener(this)
        try {
            val success =
                mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
            if (!success) {
                Timber.tag("<><>").e("Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            Timber.tag("<><>").e("Can't find style. Error: ".plus(e))
        }

        try {
            if (locationPermissionGranted) {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                mMap.uiSettings.isMapToolbarEnabled = false

                val locationResult = fusedLocationClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        val lastKnownLocation = task.result
                        lastKnownLocation?.let {
                            val latLng = LatLng(it.latitude, it.longitude)
                            setMarker(latLng)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM))
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onMapClick(p0: LatLng) {
//        mMap.clear()
//        setMarker(p0)
    }

    private fun setMarker(p0: LatLng) {
        latitude = p0.latitude
        longitude = p0.longitude
        if (vectorMarkerBitmap != null) {
            mMap.addMarker(MarkerOptions().position(p0).icon(vectorMarkerBitmap))
        } else {
            mMap.addMarker(
                MarkerOptions().position(p0)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_post_location_marker))
            )
        }
//        getAddressFromLocation()
    }

    private fun searchLocationPlaces() {
        binding.searchAppCompatEditText.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(this)
            }.autoDispose()

        binding.searchAppCompatEditText.textChanges()
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
                    binding.locationSearchRecyclerView.visibility = View.VISIBLE
                    placeSearch(it.toString())
                } else if (it.isEmpty()) {
                    placesAdapter.listOfPlaces = arrayListOf()
                }
                UiUtils.hideKeyboard(this)
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(this)
            binding.searchAppCompatEditText.setText("")
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
//        api/place/textsearch/json?key=${PLACE_API_KEY}&type=cafe
        val retrofit = Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/maps/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okhttpBuilder.build())
            .build()

        val retrofitService = retrofit.create(PlaceSearch::class.java)

        retrofitService.getPlacesLists(PLACE_API_KEY, search, currentLocation ?: "", MAX_RADIUS)
            .enqueue(object :
                Callback<PlaceSearchResponse> {
                override fun onResponse(
                    call: Call<PlaceSearchResponse>,
                    response: Response<PlaceSearchResponse>
                ) {
                    Log.e("Response %s", response.body()?.toString() ?: "")
                    response.body()?.results?.let {
//                        placesAdapter.listOfPlaces = it
                        hideKeyboard()
                        addLocationAdapter.listOfPlaces = it
                    }
                }

                override fun onFailure(call: Call<PlaceSearchResponse>, t: Throwable) {
                    Log.e("MainActivity", t.toString())
                }
            })
    }

    private fun getAddressFromLocation() {
        try {
            if (latitude != 0.toDouble() && longitude != 0.toDouble()) {
                Handler().post {
                    val geocoder = Geocoder(this, Locale.ENGLISH)
                    try {
                        val addressList = geocoder.getFromLocation(latitude, longitude, 1)
                        runOnUiThread {
                            if (!addressList.isNullOrEmpty()) {
                                val address = addressList.firstOrNull()
                                if (address != null) {
                                    var mainAddress = ""

                                    val subLocality = address.subLocality
                                    val locality = address.locality
                                    val subAdminArea = address.subAdminArea
                                    val adminArea = address.adminArea
                                    val countryName = address.countryName

                                    if (!subLocality.isNullOrEmpty()) {
                                        mainAddress = subLocality.plus(" ")
                                    } else if (!locality.isNullOrEmpty()) {
                                        mainAddress = mainAddress.plus(locality).plus(" ")
                                    } else if (!subAdminArea.isNullOrEmpty()) {
                                        mainAddress = mainAddress.plus(subAdminArea).plus(" ")
                                    } else if (!adminArea.isNullOrEmpty()) {
                                        mainAddress = mainAddress.plus(adminArea).plus(" ")
                                    } else if (!countryName.isNullOrEmpty()) {
                                        mainAddress = mainAddress.plus(countryName).plus(" ")
                                    }

                                    if (mainAddress.isNotEmpty()) {
                                        location = address.getAddressLine(0)
                                        binding.tvMainLocation.text = address.getAddressLine(0)
                                        binding.llFooter.visibility = View.VISIBLE
                                    } else {
                                        val maxAddressLineIndex = address.maxAddressLineIndex
                                        if (maxAddressLineIndex >= 0) {
                                            val fullAddress = address.getAddressLine(0)
                                            if (!fullAddress.isNullOrEmpty()) {
                                                location = fullAddress
                                                binding.tvMainLocation.text = fullAddress
                                                binding.llFooter.visibility = View.VISIBLE
                                            } else {
                                                location = ""
                                                binding.tvMainLocation.text =
                                                    getString(R.string.address_not_found)
                                                binding.llFooter.visibility = View.GONE
                                            }
                                        } else {
                                            location = ""
                                            binding.tvMainLocation.text =
                                                getString(R.string.address_not_found)
                                            binding.llFooter.visibility = View.GONE
                                        }
                                    }
                                } else {
                                    location = ""
                                    binding.tvMainLocation.text =
                                        getString(R.string.address_not_found)
                                    binding.llFooter.visibility = View.GONE
                                }
                            } else {
                                location = ""
                                binding.tvMainLocation.text = getString(R.string.address_not_found)
                                binding.llFooter.visibility = View.GONE
                            }
                        }
                    } catch (e: IOException) {

                    }
                }
            } else {
                location = ""
                binding.tvMainLocation.text = getString(R.string.address_not_found)
                binding.llFooter.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}