package com.outgoer.ui.venue.update

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
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
import com.outgoer.R
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityVenueInfoUpdateBinding
import com.outgoer.ui.addvenuemedia.viewmodel.AddVenueMediaViewModel
import com.outgoer.ui.postlocation.AddPostLocationActivity
import com.outgoer.ui.venue.SpontyVenueLocationBottomSheet
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class VenueInfoUpdateActivity : BaseActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    private lateinit var binding: ActivityVenueInfoUpdateBinding
    private var registerVenueRequest: RegisterVenueRequest? = null

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationPermissionGranted = false

    private var latitude = 0.toDouble()
    private var longitude = 0.toDouble()
    private var location = ""
    private var vectorMarkerBitmap: BitmapDescriptor? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<AddVenueMediaViewModel>
    private lateinit var addVenueMediaViewModel: AddVenueMediaViewModel

    private var currentLocation: String? = null
    private var marker: Marker? = null

    companion object {
        val INTENT_REGISTER_VENUE = "INTENT_REGISTER_VENUE"
        fun getIntent(context: Context, registerVenueRequest: RegisterVenueRequest): Intent {

            var intent = Intent(context, VenueInfoUpdateActivity::class.java)
            intent.putExtra(INTENT_REGISTER_VENUE, registerVenueRequest)

            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        addVenueMediaViewModel = getViewModelFromFactory(viewModelFactory)
        binding = ActivityVenueInfoUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        initUI()
        locationPermission()
        listenToViewModel()

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
                        }
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    locationPermissionGranted = false
                    initMap()
                }
            })
    }

    private fun listenToViewModel() {
        addVenueMediaViewModel.addVenueMediaState.subscribeAndObserveOnMainThread {
            when (it) {
                is AddVenueMediaViewModel.AddVenueMediaViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is AddVenueMediaViewModel.AddVenueMediaViewState.UpdateVenueSuccess -> {
                    finish()
                }
                else -> {

                }
            }
        }.autoDispose()
    }

    private fun initMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun initUI() {
        intent?.let {
            registerVenueRequest =
                it.getParcelableExtra<RegisterVenueRequest>(INTENT_REGISTER_VENUE)

            vectorMarkerBitmap =
                bitmapDescriptorFromVector(this, R.drawable.ic_search_place_location_pin)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        }

        binding.editAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {

            var spontyVenueLocationBottomSheet = SpontyVenueLocationBottomSheet.newInstance()
            spontyVenueLocationBottomSheet.venueLocationClick.subscribeAndObserveOnMainThread {
                binding.locationAppCompatTextView.text = it.formattedAddress
                setMarker(LatLng(it.geometry?.location?.lat ?: 0.0, it.geometry?.location?.lng ?: 0.0))
            }
            spontyVenueLocationBottomSheet.show(supportFragmentManager, SpontyVenueLocationBottomSheet.javaClass.simpleName)
        }.autoDispose()

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()

        binding.continueMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (binding.locationAppCompatTextView.text.toString().isNullOrEmpty()) {
                showToast(resources.getString(R.string.label_enter_description))
            } else {
                val registerVenueRequest = RegisterVenueRequest(
                    latitude = latitude.toString(),
                    longitude = longitude.toString(),
                    venueAddress = binding.locationAppCompatTextView.text.toString(),
                    name = this.registerVenueRequest?.name
                )
                addVenueMediaViewModel.updateVenue(registerVenueRequest)
            }
        }.autoDispose()
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0

        registerVenueRequest?.let {
            it?.latitude?.toDouble()
                ?.let { it1 -> it?.longitude?.toDouble()?.let { it2 -> LatLng(it1, it2) } }
                ?.let { it2 -> setMarker(it2) }

            binding.locationAppCompatTextView.text = it.venueAddress
        }

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

                            getAddress(latLng)
                            setMarker(latLng)
                            mMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    latLng,
                                    AddPostLocationActivity.DEFAULT_ZOOM
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
        val addresses: List<Address> =
            geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)?:return

        latitude = latLng.latitude
        longitude = latLng.longitude

        val address: String = addresses[0].getAddressLine(0)
        val city: String = addresses[0].getLocality()
        val state: String = addresses[0].getAdminArea()
        val country: String = addresses[0].getCountryName()
        val postalCode: String = addresses[0].getPostalCode()

        binding.locationAppCompatTextView.text =
            "${address}, ${city} - ${postalCode}, ${state}, ${country}"
        binding.locationAppCompatEditText.setText("${address}, ${city} - ${postalCode}, ${state}, ${country}")

    }

    override fun onMapClick(p0: LatLng) {
    }

    private fun setMarker(p0: LatLng) {
        latitude = p0.latitude
        longitude = p0.longitude
        mMap.clear()
        marker?.remove()
        if (vectorMarkerBitmap != null) {
            marker = mMap.addMarker(MarkerOptions().position(p0).icon(vectorMarkerBitmap))
        } else {
            marker = mMap.addMarker(
                MarkerOptions().position(p0)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_post_location_marker))
            )
        }
    }
}