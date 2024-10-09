package com.outgoer.ui.venue

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.StrictMode
import android.provider.Settings
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
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
import com.makeramen.roundedimageview.RoundedImageView
import com.outgoer.BuildConfig
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityVenueInfoBinding
import com.outgoer.ui.postlocation.AddPostLocationActivity
import com.outgoer.ui.sponty.location.PlaceSearch
import com.outgoer.ui.sponty.location.model.PlaceDetailsResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class VenueInfoActivity : BaseActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private lateinit var binding: ActivityVenueInfoBinding
    private var registerVenueRequest: RegisterVenueRequest? = null

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationPermissionGranted = false

    private var latitude = 0.toDouble()
    private var longitude = 0.toDouble()
    private var location = ""
    private var vectorMarkerBitmap: BitmapDescriptor? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var marker: Marker? = null

    private var currentLocation: String? = null
    private lateinit var locationManager: LocationManager
    private val RC_GPS_SETTINGS = 100001

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        val INTENT_REGISTER_VENUE = "INTENT_REGISTER_VENUE"
        fun getIntent(context: Context, registerVenueRequest: RegisterVenueRequest): Intent {

            var intent = Intent(context, VenueInfoActivity::class.java)
            intent.putExtra(INTENT_REGISTER_VENUE, registerVenueRequest)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityVenueInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        initUI()
        checkLocationPermission()
        registerVenueRequest = loggedInUserCache.getVenueRequest()

        binding.descriptionAppCompatEditText.setText(registerVenueRequest?.description ?: "")
        binding.locationAppCompatTextView.setText(registerVenueRequest?.venueAddress ?: "")


        latitude = registerVenueRequest?.latitude?.toDouble() ?: 0.0
        longitude = registerVenueRequest?.longitude?.toDouble() ?: 0.0

    }

    private fun locationPermission() {
        XXPermissions.with(this).permission(Permission.ACCESS_COARSE_LOCATION).permission(Permission.ACCESS_FINE_LOCATION)
            .request(object : OnPermissionCallback {
                @SuppressLint("MissingPermission")
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    locationPermissionGranted = all
                    initMap()
                    fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            val lat = it.latitude
                            val longi = it.longitude
                            currentLocation = "${lat}, ${longi}"
                        }
                    }.addOnFailureListener {
                        Toast.makeText(
                            this@VenueInfoActivity, "Failed on getting current location", Toast.LENGTH_SHORT
                        ).show()
                    }
                }


                override fun onDenied(permissions: List<String>, never: Boolean) {
                    locationPermissionGranted = false
                    initMap()
                }
            })
    }

    private fun checkLocationPermission() {
        XXPermissions.with(this).permission(Permission.ACCESS_COARSE_LOCATION).permission(Permission.ACCESS_FINE_LOCATION)
            .permission(Permission.ACCESS_BACKGROUND_LOCATION).request(object : OnPermissionCallback {

                override fun onGranted(permissions: List<String>, all: Boolean) {
                    if (all) {
                        val isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                        if (isGPSEnabled) {
                            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                                location?.let {
                                    val lat = it.latitude
                                    val longi = it.longitude
                                    currentLocation = "${lat}, ${longi}"
                                }
                            }.addOnFailureListener {
                                Toast.makeText(
                                    this@VenueInfoActivity, "Failed on getting current location", Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            showGPSSettingsAlert()
                        }
                        locationPermissionGranted = all
                        initMap()

                    } else {

                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    locationPermissionGranted = false
                    initMap()
                    if (never) {
                        XXPermissions.startPermissionActivity(this@VenueInfoActivity, permissions)
                    }
                }
            })
    }

    private fun showGPSSettingsAlert() {
        val alertDialog = AlertDialog.Builder(ContextThemeWrapper(this, R.style.AlertDialogCustom))
        alertDialog.setTitle(getString(R.string.msg_gps_settings))
        alertDialog.setMessage(getString(R.string.msg_gps_settings_confirmation))
        alertDialog.setPositiveButton(getString(R.string.label_settings)) { _, _ ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(intent, RC_GPS_SETTINGS)
        }
        alertDialog.setNegativeButton(getString(R.string.label_cancel)) { dialog, _ ->
            dialog.cancel()
        }
        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initUI() {
        intent?.let {
            registerVenueRequest = it.getParcelableExtra<RegisterVenueRequest>(INTENT_REGISTER_VENUE)
            vectorMarkerBitmap = bitmapDescriptorFromVector(this, R.drawable.ic_place_holder_user_map)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        }

        binding.editAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            var spontyVenueLocationBottomSheet = SpontyVenueLocationBottomSheet.newInstance(true)

            spontyVenueLocationBottomSheet.venueLocationClick.subscribeAndObserveOnMainThread {
                binding.locationAppCompatTextView.text = it.formattedAddress
                setMarker(
                    LatLng(
                        it.geometry?.location?.lat ?: 0.0, it.geometry?.location?.lng ?: 0.0
                    )
                )
            }

            spontyVenueLocationBottomSheet.placeAvailableClick.subscribeAndObserveOnMainThread {
                binding.locationAppCompatTextView.text = it.description
                it.placeId?.let { it1 -> placeDetails(it1) }
            }

            spontyVenueLocationBottomSheet.show(
                supportFragmentManager, SpontyVenueLocationBottomSheet.javaClass.simpleName
            )
        }

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }

        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {

            if (binding.descriptionAppCompatEditText.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.label_enter_description))
            } else {
                registerVenueRequest = loggedInUserCache.getVenueRequest()
                registerVenueRequest?.let {
                    it.latitude = latitude.toString()
                    it.longitude = longitude.toString()
                    it.description = binding.descriptionAppCompatEditText.text.toString()
                    it.venueAddress = binding.locationAppCompatTextView.text.toString()

                    loggedInUserCache.setVenueRequest(it)
                    startActivity(VenueAvailabilityActivity.getIntent(this@VenueInfoActivity, it))
                }
            }
        }
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        p0.setOnMapClickListener(this)
        try {
            val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_json))
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
                fusedLocationClient.lastLocation.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        val lastKnownLocation = task.result
                        lastKnownLocation?.let {
                            val latLng = LatLng(it.latitude, it.longitude)

                            getAddress(latLng)
                            setMarker(latLng)
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    latLng, AddPostLocationActivity.DEFAULT_ZOOM
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun getAddress(latLng: LatLng) {
        val geocoder: Geocoder = Geocoder(this, Locale.getDefault())
        val addresses: MutableList<Address> = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) ?: return

        latitude = latLng.latitude
        longitude = latLng.longitude

        val address: String = addresses[0].getAddressLine(0)
        val city: String = addresses[0].getLocality()
        val state: String = addresses[0].getAdminArea()
        val country: String = addresses[0].getCountryName()
        val postalCode: String = addresses[0].getPostalCode()


        binding.locationAppCompatTextView.text = address
        binding.locationAppCompatEditText.setText(address)

    }

    override fun onMapClick(p0: LatLng) {
    }

    private fun setMarker(p0: LatLng) {
        latitude = p0.latitude
        longitude = p0.longitude

        mMap.clear()
        marker?.remove()
        if (vectorMarkerBitmap != null) {
            //marker = mMap.addMarker(MarkerOptions().position(p0).icon(vectorMarkerBitmap))
            prepareVenueMarkerBitmap(p0)
        } else {
            marker = mMap.addMarker(
                MarkerOptions().position(p0).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_post_location_marker))
            )

            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    p0, AddPostLocationActivity.DEFAULT_ZOOM
                )
            )
        }
    }

    private fun prepareVenueMarkerBitmap(p0: LatLng) {
        mMap.clear()
        mMap.let { gMap ->
            val latLngBuilder = LatLngBounds.Builder()
            val latitude = p0.latitude
            val longitude = p0.longitude
            latLngBuilder.include(LatLng(latitude.toDouble(), longitude.toDouble()))

            val markerLayout: View = View.inflate(this, R.layout.layout_map_marker, null)
            val ivUserProfile = markerLayout.findViewById(R.id.ivUserProfile) as RoundedImageView
            val ivUserProfile1 = markerLayout.findViewById(R.id.ivUserProfile1) as AppCompatImageView
            val ivPin = markerLayout.findViewById(R.id.ivPin) as AppCompatImageView
            val tvDistance = markerLayout.findViewById(R.id.tvDistance) as AppCompatTextView
            tvDistance.text = binding.locationAppCompatTextView.text.toString()

            ivUserProfile.visibility = View.VISIBLE
            ivUserProfile1.visibility = View.VISIBLE
            ivPin.visibility = View.GONE
            Glide.with(this).asBitmap().load(R.drawable.venue_placeholder).centerCrop().override(150).placeholder(R.drawable.venue_placeholder)
                .addListener(object : RequestListener<Bitmap> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean,
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean,
                    ): Boolean {
                        runOnUiThread {
                            if (resource != null) {
                                ivUserProfile.setImageBitmap(resource)
                                addVenueMarkerBitmap(
                                    gMap, markerLayout, latitude.toString(), longitude.toString()
                                )
                            }
                        }
                        return false
                    }
                }).into(ivUserProfile)
        }
    }

    private fun placeDetails(search: String) {
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

        retrofitService.getPlaceDetails("AIzaSyBxVTyN0YmOdGY4e92OObhtYCXC0VtxLB8", search)
            .enqueue(object :
                Callback<PlaceDetailsResponse> {
                override fun onResponse(
                    call: Call<PlaceDetailsResponse>,
                    response: Response<PlaceDetailsResponse>
                ) {
                    Log.e("Response %s", response.body()?.toString() ?: "")

                    response.body()?.result?.let {
                        val latLng = LatLng(
                            it.geometry?.location?.lat ?: 0.0, it.geometry?.location?.lng ?: 0.0
                        )
                        setMarker(latLng)

                        io.reactivex.Observable.timer(100,TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    latLng, AddPostLocationActivity.DEFAULT_ZOOM
                                )
                            )
                        }.autoDispose()
                    }
                }

                override fun onFailure(call: Call<PlaceDetailsResponse>, t: Throwable) {
                    Log.e("MainActivity", t.toString())
                }
            })
    }

    private fun addVenueMarkerBitmap(
        gMap: GoogleMap,
        markerLayout: View,
        latitude: String, longitude: String,
    ) {
        val widthHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        markerLayout.measure(widthHeight, widthHeight)
        markerLayout.layout(0, 0, markerLayout.measuredWidth, markerLayout.measuredHeight)
        val bitmap = Bitmap.createBitmap(
            markerLayout.measuredWidth, markerLayout.measuredHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        markerLayout.draw(canvas)
        if (bitmap != null) {
            val markerOptions = MarkerOptions().position(LatLng(latitude.toDouble(), longitude.toDouble())).draggable(false).flat(false)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))

            val mapMarker = gMap.addMarker(markerOptions)
            if (mapMarker != null) {
                // mapMarker.tag = venueMapInfo
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GPS_SETTINGS) {
            checkLocationPermission()
        }
    }

}