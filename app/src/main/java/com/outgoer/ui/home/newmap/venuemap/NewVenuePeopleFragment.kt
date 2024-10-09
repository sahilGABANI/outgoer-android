package com.outgoer.ui.home.newmap.venuemap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.makeramen.roundedimageview.RoundedImageView
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.VenueInfo
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.bitmapDescriptorFromVector
import com.outgoer.base.extension.createBitmapWithBorder
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentNewVenuePeopleBinding
import com.outgoer.ui.home.map.venuemap.viewmodel.MapVenueViewModel
import com.outgoer.ui.home.map.venuemap.viewmodel.VenueCategoryViewState
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.CastMessagingBottomSheet
import com.outgoer.ui.home.newmap.venuemap.view.VenueListAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.tag_venue.VenueTaggedActivity
import com.outgoer.ui.videorooms.VideoRoomsActivity
import com.outgoer.utils.UiUtils
import com.outgoer.utils.Utility
import com.outgoer.utils.Utility.storyListUtil
import com.outgoer.utils.Utility.toggleSelectedStory
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

private const val TAG = "NewVenuePeopleFragment"
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 20

class NewVenuePeopleFragment(
    private var latitude: Double = 0.toDouble(),
    private var longitude: Double = 0.toDouble(),
) : BaseFragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraMoveListener,
    GoogleMap.OnCameraIdleListener {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MapVenueViewModel>
    private lateinit var mapVenueViewModel: MapVenueViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserAvatar by Delegates.notNull<String>()

    private var _binding: FragmentNewVenuePeopleBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null

    private var userProfileBorderRadius = 0f
    private var userProfileBorderColor = 0
    private var defaultPlaceHolderUser: BitmapDescriptor? = null
    private var currentMapLocationClicked: Boolean = false

    private var venueCategoryId = 0
    private var isInfoMarkerClicked = false
    var categoryid: Int? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var currentLocation: Location? = null
    private lateinit var venueListAdapter: VenueListAdapter

    private var clusterManager: ClusterManager<VenueInfo>? = null
    private var venueMapInfoList: ArrayList<VenueMapInfo> = arrayListOf()

    var mLocationRequest: LocationRequest? = null
    var mCurrLocationMarker: Marker? = null
    private var userIsInteractingWithMap = false
    private var marketInfo: Marker? = null
    private lateinit var newVenueContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        mapVenueViewModel = getViewModelFromFactory(viewModelFactory)
        loggedInUserAvatar = loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewVenuePeopleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        newVenueContext = view.context
        initUI()
        listenToViewModel()
        listenToViewEvents()


        initAdapter()
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        fetchCurrentLocation()
    }

    private fun initAdapter() {
        venueListAdapter = VenueListAdapter(requireContext()).apply {
            venueCategoryAllClick.subscribeAndObserveOnMainThread {
                if (it.storyCount == 1) {
                    listOfDataItems?.find { id -> id.userId == it.userId }?.storyCount = 0
                    venueListAdapter.listOfDataItems = listOfDataItems
                    toggleSelectedStory(
                        newVenueContext,
                        storyListUtil,
                        it.userId as Int
                    )
                } else if (it.userType == MapVenueUserType.USER.type) {
                    startActivityWithDefaultAnimation(
                        NewOtherUserProfileActivity.getIntent(
                            requireContext(), it.userId ?: 0
                        )
                    )

                } else if (it.userType == MapVenueUserType.VENUE_OWNER.type) {

                    if ((it.isLive ?: 0) > 0) {
                        startActivity(VideoRoomsActivity.getIntentLive(requireContext(), it.liveId))
                    } else if (((it.postCount ?: 0) > 0) || ((it.reelCount
                            ?: 0) > 0) || ((it.spontyCount ?: 0) > 0) || ((it.atVenueCount
                            ?: 0) > 0)
                    ) {
                        startActivityWithDefaultAnimation(
                            VenueTaggedActivity.getIntent(
                                requireContext(),
                                it.id,
                                it.reelCount ?: -1,
                                it.postCount ?: -1,
                                it.spontyCount ?: -1
                            )
                        )
                    } else {
//                        val venueDetailsBottomsheet = VenueDetailsBottomsheet.newInstance(it.id)
//                        venueDetailsBottomsheet.show(
//                            childFragmentManager, VenueDetailsBottomsheet.TAG
//                        )

                        startActivityWithDefaultAnimation(
                            NewOtherUserProfileActivity.getIntent(
                                requireContext(), it.userId ?: 0
                            )
                        )
                    }
                }
            }

            venueFavoriteClick.subscribeAndObserveOnMainThread { item ->
                val list = venueListAdapter.listOfDataItems
                list?.find { it.id == item.id }?.apply {
                    venueFavouriteStatus = if (0.equals(venueFavouriteStatus)) 1 else 0
                }

                venueListAdapter.listOfDataItems = list
                mapVenueViewModel.addRemoveFavouriteVenue(item.id)
            }
        }
    }

    private fun listenToViewModel() {
        userProfileBorderRadius = resources.getDimension(com.intuit.sdp.R.dimen._2sdp)
        userProfileBorderColor = ContextCompat.getColor(requireContext(), R.color.md_white)

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_place_holder_user_map)
        defaultPlaceHolderUser = if (bitmap != null) {
            BitmapDescriptorFactory.fromBitmap(
                bitmap.createBitmapWithBorder(
                    userProfileBorderRadius, userProfileBorderColor
                )
            )
        } else {
            bitmapDescriptorFromVector(requireContext(), R.drawable.ic_place_holder_user_map)
        }

        mapVenueViewModel.venueCategoryState.subscribeAndObserveOnMainThread {
            when (it) {
                is VenueCategoryViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }

                is VenueCategoryViewState.FriendsVenueInfoList -> {
                    clusterManager?.clearItems()

                    venueMapInfoList = it.listOfFriendMapInfo as ArrayList<VenueMapInfo>
                    prepareVenueMarkerBitmap(it.listOfFriendMapInfo)
                    venueListAdapter.listOfDataItems = it.listOfFriendMapInfo
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun initUI() {
        binding.castingMessageAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            var broadCastMessage =
                loggedInUserCache.getLoggedInUser()?.loggedInUser?.broadcastMessage

            var castMessagingBottomSheet =
                if (broadCastMessage.isNullOrEmpty()) CastMessagingBottomSheet.newInstance() else CastMessagingBottomSheet.newInstance(
                    broadCastMessage
                )
            castMessagingBottomSheet.apply {
                dismissClick.subscribeAndObserveOnMainThread {
                    var latLngBuilder = LatLngBounds.builder()
                    addMyLocationMarkerBitmap(latLngBuilder)
                }
            }

            castMessagingBottomSheet.show(
                childFragmentManager, CastMessagingBottomSheet.javaClass.name
            )
        }.autoDispose()

        binding.etSearch.editorActions().filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(requireContext())
            }.autoDispose()

        binding.etSearch.textChanges().doOnNext {
            if (it.isNullOrEmpty()) {
                binding.ivClear.visibility = View.INVISIBLE
            } else {
                binding.ivClear.visibility = View.VISIBLE
            }
        }.debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({

                if (it.length > 2) {
                    val visibleRegion = mMap?.projection?.visibleRegion

                    visibleRegion?.let {
                        mapVenueViewModel.getMapPeopleByCategoryIn(
                            categoryid ?: 0,
                            arrayListOf(
                                visibleRegion.latLngBounds.southwest.latitude,
                                visibleRegion.latLngBounds.southwest.longitude,
                                visibleRegion.latLngBounds.northeast.latitude,
                                visibleRegion.latLngBounds.northeast.longitude
                            ),
                            binding.etSearch.text.toString(),
                        )
                    }
                } else if (it.isEmpty()) {

                    val visibleRegion = mMap?.projection?.visibleRegion

                    visibleRegion?.let {
                        mapVenueViewModel.getMapPeopleByCategoryIn(
                            venueCategoryId ?: 0,
                            arrayListOf(
                                visibleRegion.latLngBounds.southwest.latitude,
                                visibleRegion.latLngBounds.southwest.longitude,
                                visibleRegion.latLngBounds.northeast.latitude,
                                visibleRegion.latLngBounds.northeast.longitude
                            ),
                            binding.etSearch.text.toString(),
                        )
                    }
                }
//            UiUtils.hideKeyboard(requireContext())
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(requireContext())
            binding.etSearch.setText("")
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.currentLocationSAppCompatImageView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                userIsInteractingWithMap = false
                currentMapLocationClicked = !currentMapLocationClicked

                mMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(latitude, longitude), if (currentMapLocationClicked) 18f else 15.5f
                    )
                )
            }
    }

    private fun fetchCurrentLocation() {
        XXPermissions.with(this).permission(Permission.ACCESS_COARSE_LOCATION)
            .request(object : OnPermissionCallback {

                override fun onGranted(permissions: List<String>, all: Boolean) {
                    if (all) {
                        val task = fusedLocationProviderClient.lastLocation
                        task.addOnSuccessListener { location ->
                            currentLocation = location
                            if (isResumed) {
                                val supportMapFragment =
                                    (childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment)
                                supportMapFragment.getMapAsync(this@NewVenuePeopleFragment)
                            }
                        }
                    } else {
                        showToast(getString(R.string.msg_location_permission_required_for_venue))
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    showToast(getString(R.string.msg_location_permission_required_for_venue))
                 /*   if (never) {
                        XXPermissions.startPermissionActivity(
                            this@NewVenuePeopleFragment, permissions
                        )
                    }*/
                }
            })
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        p0.setOnMarkerClickListener(this)

        val visibleRegion = mMap?.projection?.visibleRegion

        visibleRegion?.let {
            mapVenueViewModel.getMapPeopleByCategoryIn(
                0,
                arrayListOf(
                    visibleRegion.latLngBounds.southwest.latitude,
                    visibleRegion.latLngBounds.southwest.longitude,
                    visibleRegion.latLngBounds.northeast.latitude,
                    visibleRegion.latLngBounds.northeast.longitude
                ),
                binding.etSearch.text.toString(),
            )
        }

        clusterManager = ClusterManager<VenueInfo>(requireContext(), mMap)
        clusterManager?.renderer = OwnIconRendered(requireContext(), p0, clusterManager!!)


        clusterManager?.setOnClusterItemClickListener { tagI ->
            var tagVenue: VenueMapInfo? =
                venueMapInfoList.find { it.name.equals(tagI.title) && it.avatar.equals(tagI.snippet) }
            tagVenue?.let { tag ->
                if (tag.userType == MapVenueUserType.USER.type) {
                    startActivityWithDefaultAnimation(
                        NewOtherUserProfileActivity.getIntent(
                            requireContext(), tag.userId ?: 0
                        )
                    )
                } else if (tag.userType == MapVenueUserType.VENUE_OWNER.type) {
                    if (tag.isLive ?: 0 > 0) {
                        startActivity(
                            VideoRoomsActivity.getIntentLive(
                                requireContext(), tag.liveId
                            )
                        )
                    } else if (tag.isTagged?.equals(1) == true && tag.atVenueCount ?: 0 > 0) {
                        startActivityWithDefaultAnimation(
                            VenueTaggedActivity.getIntent(
                                requireContext(),
                                tag.id,
                                tag.isReel ?: -1,
                                tag.isPost ?: -1,
                                tag.spontyCount ?: -1
                            )
                        )
                    } else {
                        startActivityWithDefaultAnimation(
                            NewVenueDetailActivity.getIntent(
                                requireContext(), tag.category?.firstOrNull()?.id ?: 0, tag.id
                            )
                        )
                    }
                }
            }
            true
        }

        clusterManager?.setOnClusterClickListener {

            val currentLatLng = LatLng(latitude, longitude)

            mMap?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    currentLatLng, 20f
                )
            )
            userIsInteractingWithMap = true

            true
        }


        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            p0.isMyLocationEnabled = false
            p0.uiSettings.isMyLocationButtonEnabled = false
            p0.uiSettings.isMapToolbarEnabled = false
//            p0.animateCamera(CameraUpdateFactory.zoomTo(10f))
            updateCurrentLocation()
        }
        mMap?.setOnCameraMoveStartedListener(this)
        mMap?.setOnCameraMoveListener(this)


        mMap?.setOnCameraIdleListener(clusterManager)
        mMap?.setOnMarkerClickListener(clusterManager)
        try {
            val success = p0.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(), R.raw.style_json
                )
            )
            if (!success) {
                Timber.tag("<><>").e("Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            e.printStackTrace()
        }

        currentLocation?.let {
            val latLng = LatLng(it.latitude, it.longitude)
//            p0.animateCamera(CameraUpdateFactory.newLatLng(latLng))
//            p0.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))

            currentLocation = null
        }

    }

    private fun prepareVenueMarkerBitmap(peopleMapInfoList: List<VenueMapInfo>) {
        mMap?.clear()
        mMap?.let { gMap ->
            for (i in peopleMapInfoList.indices) {
                val venueMapInfo = peopleMapInfoList[i]
                clusterManager?.addItem(
                    VenueInfo(
                        venueMapInfo.id,
                        venueMapInfo?.latitude?.toDouble() ?: 0.0,
                        venueMapInfo?.longitude?.toDouble() ?: 0.0,
                        venueMapInfo.name ?: "",
                        venueMapInfo.avatar ?: ""
                    )
                )
            }

            clusterManager?.cluster()

            Handler(Looper.getMainLooper()).postDelayed({
                var latLngBuilder = LatLngBounds.builder()
                addMyLocationMarkerBitmap(latLngBuilder)
            }, 50)
        }
    }

    private fun addMyLocationMarkerBitmap(latLngBuilder: LatLngBounds.Builder) {
        if (isResumed) {

            if (marketInfo != null) {
                marketInfo?.remove()
            }

            latLngBuilder.include(LatLng(latitude, longitude))

            val markerLayout: View =
                View.inflate(requireContext(), R.layout.layout_user_marker, null)
            val ivUserProfile = markerLayout.findViewById(R.id.myLocation) as AppCompatImageView
            val venueUserInfoFrameLayout =
                markerLayout.findViewById(R.id.venueUserInfoFrameLayout) as FrameLayout
            val castingMessageFrameLayout =
                markerLayout.findViewById(R.id.castingMessageFrameLayout) as FrameLayout
            val castMessageAppCompatTextView =
                markerLayout.findViewById(R.id.castMessageAppCompatTextView) as AppCompatTextView
            val markerEdgeAppCompatImageView =
                markerLayout.findViewById(R.id.markerEdgeAppCompatImageView) as AppCompatImageView

            castingMessageFrameLayout.isVisible =
                (loggedInUserCache.getLoggedInUser()?.loggedInUser?.broadcastMessage != null)
            castMessageAppCompatTextView.text =
                (loggedInUserCache.getLoggedInUser()?.loggedInUser?.broadcastMessage ?: "")
            ivUserProfile.visibility = View.VISIBLE
            venueUserInfoFrameLayout.visibility = View.GONE

            if (MapVenueUserType.VENUE_OWNER.type.equals(loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType)) {
                castMessageAppCompatTextView.background =
                    resources.getDrawable(R.drawable.blue_gredient_color, null)
                markerEdgeAppCompatImageView.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.half_circle_blue, null
                    )
                )
            } else {
                castMessageAppCompatTextView.background =
                    resources.getDrawable(R.drawable.purple_gradient_color, null)
                markerEdgeAppCompatImageView.setImageDrawable(
                    resources.getDrawable(
                        R.drawable.half_circle, null
                    )
                )
            }

            Glide.with(requireContext()).asBitmap().load(R.drawable.user_location).centerInside()
                .placeholder(R.drawable.user_location)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
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
                        isFirstResource: Boolean,
                    ): Boolean {
                        requireActivity().runOnUiThread {
                            if (resource != null) {
                                ivUserProfile.setImageBitmap(resource)

                                val widthHeight = View.MeasureSpec.makeMeasureSpec(
                                    0, View.MeasureSpec.UNSPECIFIED
                                )
                                markerLayout.measure(widthHeight, widthHeight)
                                markerLayout.layout(
                                    0, 0, markerLayout.measuredWidth, markerLayout.measuredHeight
                                )
                                val bitmap = Bitmap.createBitmap(
                                    markerLayout.measuredWidth,
                                    markerLayout.measuredHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = Canvas(bitmap)
                                markerLayout.draw(canvas)

                                if (bitmap != null) {
                                    val markerOptions =
                                        MarkerOptions().position(LatLng(latitude, longitude))
                                            .draggable(false).flat(false)
                                            .icon(BitmapDescriptorFactory.fromBitmap(bitmap))

                                    marketInfo = mMap?.addMarker(markerOptions)
                                    mMap?.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(
                                                latitude, longitude
                                            ), 15f
                                        ), null
                                    );

                                }
                            }
                        }
                        return false
                    }
                }).into(ivUserProfile)
        }
    }

    private fun addVenueMarkerBitmap(
        venueMapInfo: VenueMapInfo,
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
            val markerOptions =
                MarkerOptions().position(LatLng(latitude.toDouble(), longitude.toDouble()))
                    .draggable(false).flat(false).icon(BitmapDescriptorFactory.fromBitmap(bitmap))

            val mapMarker = gMap.addMarker(markerOptions)
            if (mapMarker != null) {
                mapMarker.tag = venueMapInfo
            }
        }
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return true
    }


    private fun updateCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap?.isMyLocationEnabled = false
            mLocationRequest =
                LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    //.setInterval(1000)
                    .setFastestInterval(500);
            var locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        // Update the marker's position
                        val currentLatLng = LatLng(location.latitude, location.longitude)

                        if (!userIsInteractingWithMap) {
                            mCurrLocationMarker?.position = currentLatLng

                            var latLngBuilder = LatLngBounds.builder()
                            addMyLocationMarkerBitmap(latLngBuilder)
                            mMap?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    currentLatLng, if (currentMapLocationClicked) 18f else 15.5f
                                )
                            )
                        }

                    }
                }
            }
            fusedLocationProviderClient.requestLocationUpdates(
                mLocationRequest!!, locationCallback, Looper.getMainLooper()
            )

        } else {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
        }
    }


    override fun onCameraMoveStarted(p0: Int) {
        if (p0 == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            userIsInteractingWithMap = true
        }
    }

    override fun onCameraMove() {
    }

    override fun onCameraIdle() {
    }

    inner class OwnIconRendered(
        private val context: Context,
        private val map: GoogleMap,
        private val clusterManager: ClusterManager<VenueInfo>
    ) : DefaultClusterRenderer<VenueInfo>(context, map, clusterManager) {

        override fun onBeforeClusterRendered(
            cluster: Cluster<VenueInfo>?, markerOptions: MarkerOptions?
        ) {
            val markerLayout: View = View.inflate(context, R.layout.layout_count_marker, null)

            var drawable = if (cluster?.size ?: 0 >= 100) {
                R.drawable.red_location_bg
            } else if (cluster?.size ?: 0 < 100 && cluster?.size ?: 0 >= 10) {
                R.drawable.cluster_bg
            } else {
                R.drawable.purple_location_bg
            }
            markerLayout.findViewById<AppCompatImageView>(R.id.backgroundAppCompatImageView)
                .setImageDrawable(context.getResources().getDrawable(drawable))
            markerLayout.findViewById<AppCompatTextView>(R.id.countTextAppCompatTextView)
                .setText(cluster?.size.toString())
            markerLayout.findViewById<AppCompatTextView>(R.id.countTextAppCompatTextView).visibility =
                View.INVISIBLE

            val widthHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            markerLayout.measure(widthHeight, widthHeight)
            markerLayout.layout(0, 0, markerLayout.measuredWidth, markerLayout.measuredHeight)
            val bitmap = Bitmap.createBitmap(
                markerLayout.measuredWidth, markerLayout.measuredHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            markerLayout.draw(canvas)
            if (bitmap != null) {
                markerOptions?.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            }
        }

        override fun onBeforeClusterItemRendered(item: VenueInfo?, markerOptions: MarkerOptions?) {
            val markerLayout: View = View.inflate(context, R.layout.layout_user_marker, null)

            markerLayout.findViewById<AppCompatImageView>(R.id.myLocation).visibility = View.GONE

            markerLayout.findViewById<RoundedImageView>(R.id.ivUserProfile).visibility =
                View.VISIBLE
            venueMapInfoList.find { it.name.equals(item?.title) && it.avatar.equals(item?.snippet) }
                ?.apply {

                    markerLayout.findViewById<FrameLayout>(R.id.castingMessageFrameLayout).isVisible =
                        (broadcastMessage != null)
                    markerLayout.findViewById<AppCompatTextView>(R.id.castMessageAppCompatTextView).isVisible =
                        (broadcastMessage != null)
                    markerLayout.findViewById<AppCompatTextView>(R.id.castMessageAppCompatTextView).text =
                        broadcastMessage
                    markerLayout.findViewById<AppCompatTextView>(R.id.userNameAppCompatTextView).text =
                        username ?: name
                }

            Glide.with(context).load(item?.snippet).placeholder(R.drawable.ic_chat_user_placeholder)
                .error(R.drawable.ic_chat_user_placeholder)
                .into(markerLayout.findViewById(R.id.ivUserProfile))

//            markerOptions?.icon(item.getMarker().getIcon());

            val widthHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            markerLayout.measure(widthHeight, widthHeight)
            markerLayout.layout(0, 0, markerLayout.measuredWidth, markerLayout.measuredHeight)
            val bitmap = Bitmap.createBitmap(
                markerLayout.measuredWidth, markerLayout.measuredHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            markerLayout.draw(canvas)
            if (bitmap != null) {
                markerOptions?.icon(BitmapDescriptorFactory.fromBitmap(bitmap))
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val visibleRegion = mMap?.projection?.visibleRegion

        visibleRegion?.let {
            mapVenueViewModel.getMapPeopleByCategoryIn(
                0,
                arrayListOf(
                    visibleRegion.latLngBounds.southwest.latitude,
                    visibleRegion.latLngBounds.southwest.longitude,
                    visibleRegion.latLngBounds.northeast.latitude,
                    visibleRegion.latLngBounds.northeast.longitude
                ),
                binding.etSearch.text.toString(),
            )
        }
    }
}