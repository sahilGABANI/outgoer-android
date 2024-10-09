package com.outgoer.ui.map

import android.Manifest
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CircleOptions
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.databinding.ActivityGeoMapsBinding

private const val TAG = "MapActivity"
private lateinit var geoClient: GeofencingClient
private  val REQUEST_TURN_DEVICE_LOCATION_ON =20
private val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 3
private val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 4
private val REQUEST_LOCATION_PERMISSION = 10

class GeoMapsActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityGeoMapsBinding
    private val geofenceList =ArrayList<Geofence>()

    private val gadgetQ = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

    private val geofenceIntent: PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityGeoMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createChannel(this)

        geoClient = LocationServices.getGeofencingClient(this)


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val geomapFragment = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment)
        geomapFragment.getMapAsync(this)

        val latitude = 21.2366
        val longitude = 72.8845
        val radius = 100f

        geofenceList.add(Geofence.Builder()
            .setRequestId("entry.key")
            .setCircularRegion(latitude,longitude,radius)
            .setExpirationDuration(Geofence.GEOFENCE_TRANSITION_DWELL.toLong())
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build())

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val latlng = LatLng(21.2366,
            72.8845,)

        val circleOptions = CircleOptions()
            .center(latlng)
            .radius(10.0)
            .fillColor(0x40ff0000)
            .strokeColor(Color.BLUE)
            .strokeWidth(2f)

        val zoomLevel = 18f

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, zoomLevel))
        map.addMarker(MarkerOptions().position(latlng))
        map.addCircle(circleOptions)

        startLocation()
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) === PackageManager.PERMISSION_GRANTED
    }

    private fun startLocation() {
        if (isPermissionGranted()) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            map.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    //specify the geofence to monitor and the initial trigger
    private fun seekGeofencing(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    //adding a geofence
    private fun addGeofence(){
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        geoClient?.addGeofences(seekGeofencing(), geofenceIntent)?.run {
            addOnSuccessListener {
                //Toast.makeText(this@GeoMapsActivity, "Geofences added", Toast.LENGTH_SHORT).show()
            }
            addOnFailureListener {
                Toast.makeText(this@GeoMapsActivity, "Failed to add geofences", Toast.LENGTH_SHORT).show()

            }
        }
    }

    //removing a geofence
    private fun removeGeofence(){
        geoClient?.removeGeofences(geofenceIntent)?.run {
            addOnSuccessListener {
               // Toast.makeText(this@GeoMapsActivity, "Geofences removed", Toast.LENGTH_SHORT).show()

            }
            addOnFailureListener {
                Toast.makeText(this@GeoMapsActivity, "Failed to remove geofences", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun examinePermisionAndinitiatGeofence() {
        if (authorizedLocation()) {
            validateGadgetAreaInitiateGeofence()
        } else {
            askLocationPermission()
        }
    }

    // check if background and foreground permissions are approved
    @TargetApi(29)
    private fun authorizedLocation(): Boolean {
        val formalizeForeground = (
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ))
        val formalizeBackground =
            if (gadgetQ) {
                PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
            } else {
                true
            }
        return formalizeForeground && formalizeBackground
    }

    //requesting background and foreground permissions
    @TargetApi(29)
    private fun askLocationPermission() {
        if (authorizedLocation())
            return
        var grantingPermission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val customResult = when {
            gadgetQ -> {
                grantingPermission += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "askLocationPermission: ")
        ActivityCompat.requestPermissions(
            this,
            grantingPermission,
            customResult
        )

    }

    private fun validateGadgetAreaInitiateGeofence(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val locationResponses =
            client.checkLocationSettings(builder.build())

        locationResponses.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        this,
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error geting location settings resolution: " + sendEx.message)
                }
            } else {
                Toast.makeText(this, "Enable your location", Toast.LENGTH_SHORT).show()
            }
        }
        locationResponses.addOnCompleteListener {
            if (it.isSuccessful) {
                addGeofence()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size > 0 && (grantResults[0] == PackageManager.PERMISSION_GRANTED))
                startLocation()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        validateGadgetAreaInitiateGeofence(false)
    }

    override fun onStart() {
        super.onStart()
        examinePermisionAndinitiatGeofence()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeGeofence()
    }
}