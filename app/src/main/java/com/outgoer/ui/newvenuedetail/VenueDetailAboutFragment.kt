package com.outgoer.ui.newvenuedetail

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.makeramen.roundedimageview.RoundedImageView
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.extension.roundDoubleVal
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentVenueDetailAboutBinding
import com.outgoer.ui.venue.update.VenueInfoUpdateActivity
import com.outgoer.ui.venue.update.VenueTimingUpdateActivity
import com.outgoer.ui.venue.update.VenueUpdateActivity
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

class VenueDetailAboutFragment : BaseFragment(), OnMapReadyCallback, GoogleMap.OnMapClickListener {

    companion object {
        const val DEFAULT_ZOOM = 10f
        private const val VENUE_DETAILS = "venueDetail"

        @JvmStatic
        fun newInstance() = VenueDetailAboutFragment()

        @JvmStatic
        fun newInstanceWithData(venueDetail: VenueDetail): VenueDetailAboutFragment {
            val venueDetailAboutFragment = VenueDetailAboutFragment()
            val bundle = Bundle()
            bundle.putParcelable(VENUE_DETAILS, venueDetail)
            venueDetailAboutFragment.arguments = bundle
            return venueDetailAboutFragment
        }
    }

    private var _binding: FragmentVenueDetailAboutBinding? = null
    private val binding get() = _binding!!

    private var venueDetail: VenueDetail? = null
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationPermissionGranted = false

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var latitude = 0.toDouble()
    private var longitude = 0.toDouble()
    private var vectorMarkerBitmap: BitmapDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentVenueDetailAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initMap()
        initUI()
    }

    private fun initUI() {
        arguments?.let {
            venueDetail = it.getParcelable(VENUE_DETAILS)

            val venue = venueDetail?.availibility?.find { data -> data.dayName.equals(getCurrentDay()) }
            if (venueDetail?.phoneCode != null) {
                binding.ivPhone.text = venueDetail?.phoneCode?.plus(" ")?.plus(venueDetail?.phone)
            } else {
                binding.ivPhone.text = venueDetail?.phone
            }

            if (!venue?.openAt.isNullOrEmpty() && !venue?.closeAt.isNullOrEmpty() && venue?.status == 1) {
                val sb = StringBuilder()
                sb.append(venue.dayName)
                sb.append(" <font color='green'><b>")
                sb.append("Open!")
                sb.append("</b></font>")

                binding.timeAppCompatTextView.text = Html.fromHtml(sb.toString())
            } else {
                val sb = StringBuilder()
                sb.append(venue?.dayName)
                sb.append(" <font color='red'><b>")
                sb.append("Closed!")
                sb.append("</b></font>")

                binding.timeAppCompatTextView.text = Html.fromHtml(sb.toString())
            }
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                binding.distanceAppCompatTextView.text = if (venueDetail?.distance != null) {
                    venueDetail?.distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_miles))
                } else {
                    "0 ".plus(getString(R.string.label_miles))
                }
            } else {
                binding.distanceAppCompatTextView.text = if (venueDetail?.distance != null) {
                    venueDetail?.distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_kms))
                } else {
                    "0 ".plus(getString(R.string.label_kms))
                }
            }
            binding.distanceAppCompatTextView.isVisible = loggedInUserCache.getUserId() != venueDetail?.id
            binding.tvAddress.text = "${venueDetail?.venueAddress}"
        }


        binding.tvOpen.throttleClicks().subscribeAndObserveOnMainThread {
            venueDetail?.availibility?.let {
                val venueAvailabilityBottomSheet = VenueAvailabilityBottomSheet.newInstanceWithData(it)
                venueAvailabilityBottomSheet.show(childFragmentManager, VenueAvailabilityBottomSheet.Companion::class.java.simpleName)
            }
        }.autoDispose()
        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.id == venueDetail?.id) {
            binding.timeEditAppCompatImageView.isVisible = true
            binding.numberEditAppCompatImageView.isVisible = true
            binding.addressEditAppCompatImageView.isVisible = true
        }
        binding.timeEditAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val registerVenueRequest = RegisterVenueRequest(
                venueDetail?.name,
                venueDetail?.username,
                venueDetail?.email,
                venueDetail?.phone,
                null,
                venueDetail?.description,
                venueDetail?.venueAddress,
                venueDetail?.latitude,
                venueDetail?.longitude,
                venueDetail?.avatar,
                null,
                venueDetail?.availibility ?: arrayListOf(),
                arrayListOf(),
            )
            startActivity(VenueTimingUpdateActivity.getIntent(requireContext(), registerVenueRequest))
        }.autoDispose()
        binding.numberEditAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val registerVenueRequest = RegisterVenueRequest(
                venueDetail?.name,
                venueDetail?.username,
                venueDetail?.email,
                venueDetail?.phone,
                null,
                venueDetail?.description,
                venueDetail?.venueAddress,
                venueDetail?.latitude,
                venueDetail?.longitude,
                venueDetail?.avatar,
                null,
                venueDetail?.availibility ?: arrayListOf(),
                arrayListOf(),
                phoneCode = venueDetail?.phoneCode
            )
            startActivity(VenueUpdateActivity.getIntent(requireContext(), registerVenueRequest))
        }.autoDispose()

        binding.addressEditAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val registerVenueRequest = RegisterVenueRequest(
                venueDetail?.name,
                venueDetail?.username,
                venueDetail?.email,
                venueDetail?.phone,
                null,
                venueDetail?.description,
                venueDetail?.venueAddress,
                venueDetail?.latitude,
                venueDetail?.longitude,
                venueDetail?.avatar,
                null,
                venueDetail?.availibility ?: arrayListOf(),
                arrayListOf()
            )
            startActivity(VenueInfoUpdateActivity.getIntent(requireContext(), registerVenueRequest))
        }.autoDispose()
    }

    private fun initMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun getCurrentDay(): String {
        val daysArray = arrayOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
        val calendar = Calendar.getInstance()
        val day = calendar[Calendar.DAY_OF_WEEK]
        return daysArray[day - 1]
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0

        p0.setOnMapClickListener(this)
        try {
            val success = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.style_json))
            if (!success) {
                Timber.tag("<><>").e("Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
            Timber.tag("<><>").e("Can't find style. Error: ".plus(e))
        }

        val latLong = LatLng(venueDetail?.latitude?.toDouble() ?: 0.0, venueDetail?.longitude?.toDouble() ?: 0.0)
        setMarker(latLong)
        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLong, DEFAULT_ZOOM
            )
        )

        try {
            if (locationPermissionGranted) {
                mMap.isMyLocationEnabled = false
                mMap.uiSettings.isMyLocationButtonEnabled = false
                mMap.uiSettings.isMapToolbarEnabled = false

                val locationResult = fusedLocationClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        val latLongVal = LatLng(venueDetail?.latitude?.toDouble() ?: 0.0, venueDetail?.longitude?.toDouble() ?: 0.0)
                        setMarker(latLongVal)

                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Timber.tag("<><>").e("Error: ".plus(e))
        }
    }

    override fun onMapClick(p0: LatLng) {
    }

    private fun setMarker(p0: LatLng) {
        latitude = p0.latitude
        longitude = p0.longitude
        if (vectorMarkerBitmap != null) {
            mMap.addMarker(MarkerOptions().position(p0).icon(vectorMarkerBitmap))
        } else {
            val markerLayout: View = View.inflate(requireContext(), R.layout.layout_map_marker, null)

            val ivUserProfile = markerLayout.findViewById(R.id.ivUserProfile) as RoundedImageView
            val tvDistance = markerLayout.findViewById(R.id.tvDistance) as AppCompatTextView

            tvDistance.text = venueDetail?.venueAddress.toString()

            Glide.with(requireContext()).asBitmap().load(venueDetail?.avatar).centerInside().override(150)
                .placeholder(R.drawable.ic_chat_user_placeholder).error(R.drawable.ic_chat_user_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).addListener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean,
                    ): Boolean {
                        ivUserProfile.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.map_logo_image, null))
                        addVenueMarkerBitmap(
                            venueDetail, mMap, markerLayout, p0.latitude.toString(), p0.longitude.toString()
                        )
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean,
                    ): Boolean {
                        if(isVisible && isResumed) {
                            requireActivity().runOnUiThread {
                                if (resource != null) {
                                    ivUserProfile.setImageBitmap(resource)
                                    addVenueMarkerBitmap(
                                        venueDetail,
                                        mMap,
                                        markerLayout,
                                        p0.latitude.toString(),
                                        p0.longitude.toString()
                                    )
                                }
                            }
                            Timber.tag("onResourceReady")
                                .i("resource is null : ${venueDetail?.avatar}")
                        }
                        return false
                    }
                }).into(ivUserProfile)
        }
    }

    private fun addVenueMarkerBitmap(
        venueMapInfo: VenueDetail? = null,
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
            val markerOptions = MarkerOptions()
                .position(LatLng(latitude.toDouble(), longitude.toDouble()))
                .draggable(false)
                .flat(false)
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))

            val mapMarker = gMap.addMarker(markerOptions)
            if (mapMarker != null) {
                mapMarker.tag = venueMapInfo
            }
        }
    }
}