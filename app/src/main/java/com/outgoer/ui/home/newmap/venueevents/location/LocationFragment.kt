package com.outgoer.ui.home.newmap.venueevents.location

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.makeramen.roundedimageview.RoundedImageView
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.event.model.EventData
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.extension.*
import com.outgoer.databinding.FragmentLocationBinding
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import timber.log.Timber
import java.math.RoundingMode
import javax.inject.Inject


class LocationFragment : BaseFragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!
    private lateinit var mMap: GoogleMap
    private var locationPermissionGranted = false
    private var vectorMarkerBitmap: BitmapDescriptor? = null

    private var eventData: EventData? = null
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        @JvmStatic
        fun newInstance() = LocationFragment()

        private val EVENT_INFO = "EVENT_INFO"

        @JvmStatic
        fun newInstanceWithData(eventData: EventData): LocationFragment {
            var locationFragment = LocationFragment()

            val args = Bundle()
            args.putParcelable(EVENT_INFO, eventData)
            locationFragment.arguments = args

            return locationFragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
    }

    override fun onResume() {
        super.onResume()
//        binding.root.requestLayout()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        arguments?.let {
            eventData = it.getParcelable(EVENT_INFO)
            binding.tvPlaceName.text = eventData?.venueDetail?.name
            binding.tvPlaceDescription.text = eventData?.venueDetail?.venueAddress
            binding.tvPlaceRatingCount.text = eventData?.reviewAvg.toString()
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                binding.distanceAppCompatTextView.text = if (eventData?.distance != 0.00) {
                    eventData?.distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_miles))
                } else {
                    "0 ".plus(getString(R.string.label_miles))
                }
            } else {
                binding.distanceAppCompatTextView.text = if (eventData?.distance != 0.00) {
                    eventData?.distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_kms))
                } else {
                    "0 ".plus(getString(R.string.label_kms))
                }
            }
//            binding.distanceAppCompatTextView.text =
//                eventData?.distance?.toBigDecimal()?.setScale(1, RoundingMode.UP)?.toDouble().toString().plus(" miles")
            Glide.with(requireContext()).load(eventData?.venueDetail?.avatar).placeholder(R.drawable.ic_chat_user_placeholder).into(binding.ivPlaceImage)
        }
        vectorMarkerBitmap = bitmapDescriptorFromVector(requireContext(), R.drawable.ic_post_location_marker_vector)

        locationPermission()
        binding.cardContainer.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(requireContext(), 0, venueId = eventData?.venueDetail?.id ?: 0))
        }.autoDispose()
    }

    private fun locationPermission() {
        XXPermissions.with(this).permission(Permission.ACCESS_COARSE_LOCATION).permission(Permission.ACCESS_FINE_LOCATION)
            .request(object : OnPermissionCallback {
                @SuppressLint("MissingPermission")
                override fun onGranted(permissions: List<String>, all: Boolean) {
                    locationPermissionGranted = all
                    initMap()
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    locationPermissionGranted = false
                    initMap()
                }
            })
    }

    private fun initMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

//    override fun onMapReady(p0: GoogleMap) {
//
//    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        try {
            val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.style_json))
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

                arguments?.let {
                    var eventInfo = it.getParcelable<EventData>(EVENT_INFO)

                    eventInfo?.let { event ->

                        mMap?.let { gMap ->

                            val latitude = event.latitude
                            val longitude = event.longitude
                            if (!latitude.isNullOrEmpty() && !longitude.isNullOrEmpty()) {
                                val markerLayout: View = View.inflate(requireContext(), R.layout.layout_map_marker, null)

                                val ivUserProfile = markerLayout.findViewById(R.id.ivUserProfile) as RoundedImageView
                                val ivUserProfile1 = markerLayout.findViewById(R.id.ivUserProfile1) as AppCompatImageView
                                val ivPin = markerLayout.findViewById(R.id.ivPin) as AppCompatImageView
                                val tvDistance = markerLayout.findViewById(R.id.tvDistance) as AppCompatTextView

                                tvDistance.visibility = View.GONE
                                tvDistance.text = ""

                                ivUserProfile.visibility = View.VISIBLE
                                ivUserProfile1.visibility = View.VISIBLE
                                ivPin.visibility = View.GONE

                                Glide.with(requireContext()).asBitmap().load(event.user?.avatar).centerCrop().override(150)
                                    .placeholder(R.drawable.ic_logo_placeholder).diskCacheStrategy(
                                        DiskCacheStrategy.AUTOMATIC)
                                    .addListener(object : RequestListener<Bitmap> {

                                        override fun onLoadFailed(
                                            e: GlideException?,
                                            model: Any?,
                                            target: Target<Bitmap>?,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            return false
                                        }

                                        override fun onResourceReady(
                                            resource: Bitmap?,
                                            model: Any?,
                                            target: Target<Bitmap>?,
                                            dataSource: DataSource?,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            requireActivity()?.runOnUiThread {
                                                if (resource != null) {
                                                    ivUserProfile.setImageBitmap(resource)
                                                    addVenueMarkerBitmap(
                                                        event, gMap, markerLayout, latitude, longitude
                                                    )
                                                }
                                            }
                                            return false
                                        }
                                    }).into(ivUserProfile)
                            }

                            gMap.animateCamera(CameraUpdateFactory.newLatLng(LatLng(event.latitude.toDouble(), event.longitude.toDouble())))

                        }

                        val latLng = LatLng(event.latitude.toDouble(), event.longitude.toDouble())
//                        setMarker(latLng)
                        mMap.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                latLng, 15f
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
//
    private fun addVenueMarkerBitmap(
        venueMapInfo: EventData,
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
                mapMarker.tag = venueMapInfo
            }
        }
    }
//
    private fun setMarker(p0: LatLng) {
        if (vectorMarkerBitmap != null) {
            mMap.addMarker(MarkerOptions().position(p0).icon(vectorMarkerBitmap))
        } else {
            mMap.addMarker(
                MarkerOptions().position(p0).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_post_location_marker))
            )
        }
    }

}