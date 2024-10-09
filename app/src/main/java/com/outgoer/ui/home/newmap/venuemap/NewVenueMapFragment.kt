package com.outgoer.ui.home.newmap.venuemap

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
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
import com.google.gson.Gson
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.ui.IconGenerator
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.makeramen.roundedimageview.RoundedImageView
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.GeoFenceResponse
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.VenueCategory
import com.outgoer.api.venue.model.VenueInfo
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.bitmapDescriptorFromVector
import com.outgoer.base.extension.createBitmapWithBorder
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.startActivityWithDefaultAnimation
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.FragmentNewVenueMapBinding
import com.outgoer.ui.home.map.venuemap.viewmodel.MapVenueViewModel
import com.outgoer.ui.home.map.venuemap.viewmodel.VenueCategoryViewState
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.CastMessagingBottomSheet
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.CheckInBottomSheet
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.VenueDetailsBottomsheet
import com.outgoer.ui.home.newmap.venuemap.view.NewVenueCategoryAdapter
import com.outgoer.ui.home.newmap.venuemap.view.VenueListAdapter
import com.outgoer.ui.map.GeofenceBroadcastReceiver
import com.outgoer.ui.map.createChannel
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import com.outgoer.ui.tag_venue.VenueTaggedActivity
import com.outgoer.ui.videorooms.VideoRoomsActivity
import com.outgoer.utils.SnackBarUtils
import com.outgoer.utils.UiUtils
import com.outgoer.utils.Utility.storyListUtil
import com.outgoer.utils.Utility.toggleSelectedStory
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates


private const val TAG = "NewVenueMapFragment"
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 20
private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

open class NewVenueMapFragment(
    private var latitude: Double = 0.toDouble(),
    private var longitude: Double = 0.toDouble(),
) : BaseFragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnCameraMoveListener,
    GoogleMap.OnCameraIdleListener {

    companion object {
        private const val SIGNIFICANT_DIFFERENCE_THRESHOLD = 0.005
    }

    private var checkInBottomSheet: CheckInBottomSheet? = null
    private var requestPermission: Boolean = true
    private var marketInfo: Marker? = null
    private var venueCategoryItem: Int? = null

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MapVenueViewModel>
    private lateinit var mapVenueViewModel: MapVenueViewModel
    private lateinit var geoClient: GeofencingClient

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserAvatar by Delegates.notNull<String>()

    private var _binding: FragmentNewVenueMapBinding? = null
    private val binding get() = _binding!!

    private var mMap: GoogleMap? = null
    private var clusterManager: ClusterManager<VenueInfo>? = null
    private var venueMapInfoList: ArrayList<VenueMapInfo> = arrayListOf()

    private var userProfileBorderRadius = 0f
    private var userProfileBorderColor = 0
    private var defaultPlaceHolderUser: BitmapDescriptor? = null

    private lateinit var venueCategoryAdapter: NewVenueCategoryAdapter
    private var venueCategoryId = 0
    private var currentMapLocationClicked: Boolean = false

    private var venueCategoryList: List<VenueCategory> = listOf()
    var categoryId: Int? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var currentLocation: Location? = null
    private var listOfVenues: ArrayList<VenueMapInfo> = arrayListOf()

    private lateinit var venueListAdapter: VenueListAdapter

    private val geofenceList = ArrayList<Geofence>()
    private var visibleInfo = ArrayList<Double>()
    val radius = 5000f
    private var isMapVenueByCategoryInfoCalled = false
    private var isPageResumed = true
    private var previousBounds: List<Double>? = null
    private var isApiCalledInit: Boolean = true
    private var previousLocation: Location? = null
    private var liveLocation: Location? = null
    private val LOCATION_CHANGE_THRESHOLD_METERS = 1000

    private val geofenceIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                requireContext(),
                0,
                intent,
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getBroadcast(
                requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }

    private var mLocationRequest: LocationRequest? = null
    var mCurrLocationMarker: Marker? = null
    private var userIsInteractingWithMap = false
    private var isSelected: Boolean = false
    private lateinit var venueMapContext: Context
    private val venueInfoSet = mutableSetOf<VenueInfo>()
    private lateinit var locationPermissions: LocationPermissions
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        mapVenueViewModel = getViewModelFromFactory(viewModelFactory)
        loggedInUserAvatar = loggedInUserCache.getLoggedInUser()?.loggedInUser?.avatar ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewVenueMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        venueMapContext = view.context
        geoClient = LocationServices.getGeofencingClient(requireContext())
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        fetchCurrentLocation()
        val supportMapFragment =
            (childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment)
        supportMapFragment.getMapAsync(this@NewVenueMapFragment)
        initUI()
        listenToViewModel()
        listenToViewEvents()
        initAdapter()
        createChannel(requireContext())

        RxBus.listen(RxEvent.DataReload::class.java).subscribeOnIoAndObserveOnMainThread({
            if (it.selectedTab == "Map") {
                if (ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    lifecycleScope.launch {
                        delay(1000)
                        mapVenueViewModel.getVenueCategoryList()
                        mapVenueViewModel.pullToRefresh(null, null)
                    }
                    (binding.rvVenueCategoryList.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
                }
            }
        }, {
            Timber.e(it)
        }).autoDispose()

        RxBus.listen(RxEvent.RefreshVenueList::class.java).subscribeAndObserveOnMainThread {
            if (::mapVenueViewModel.isInitialized) {
                Timber.tag("Geofence").d("RefreshVenueList")
            }
        }.autoDispose()
    }

    private fun initAdapter() {
        venueListAdapter = VenueListAdapter(requireContext()).apply {
            venueCategoryAllClick.subscribeAndObserveOnMainThread {
                if (it.storyCount == 1) {
                    listOfDataItems?.find { id -> id.userId == it.userId }?.storyCount = 0
                    venueListAdapter.listOfDataItems = listOfDataItems
                    toggleSelectedStory(
                        venueMapContext,
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
                    } else if ((it.postCount ?: 0) > 0 || (it.reelCount ?: 0) > 0 || (it.spontyCount
                            ?: 0) > 0 || (it.atVenueCount ?: 0) > 0
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
                        val venueDetailsBottomSheet = VenueDetailsBottomsheet.newInstance(it.id)
                        venueDetailsBottomSheet.show(
                            childFragmentManager, VenueDetailsBottomsheet.TAG
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

    private fun initUI() {
        binding.castingMessageAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val broadCastMessage = loggedInUserCache.getLoggedInUser()?.loggedInUser?.broadcastMessage
            val castMessagingBottomSheet =
                if (broadCastMessage.isNullOrEmpty()) CastMessagingBottomSheet.newInstance() else CastMessagingBottomSheet.newInstance(
                    broadCastMessage
                )
            castMessagingBottomSheet.apply {
                dismissClick.subscribeAndObserveOnMainThread {

                    if (marketInfo != null) {
                        marketInfo?.remove()
                    }

                    val latLngBuilder = LatLngBounds.Builder()
                    if (latitude != 0.0 && longitude != 0.0) {
                        addMyLocationMarkerBitmap(latLngBuilder)
                    }
                }
            }

            castMessagingBottomSheet.show(
                childFragmentManager,
                CastMessagingBottomSheet.Companion::class.java.name
            )
        }.autoDispose()

        binding.etSearch.editorActions().filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(venueMapContext)
            }.autoDispose()

        binding.etSearch.textChanges().doOnNext {
            if (it.isNullOrEmpty()) {
                binding.ivClear.visibility = View.INVISIBLE
            } else {
                binding.ivClear.visibility = View.VISIBLE
            }
        }.debounce(500, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                // val bottomSheetParent = requireActivity().findViewById<RelativeLayout>(R.id.bottom_sheet_parent)
                if (it.length > 2) {
                    removeGeofence()
                    val visibleRegion = mMap?.projection?.visibleRegion

                    visibleRegion?.let {
                        visibleInfo = arrayListOf(
                            visibleRegion.latLngBounds.southwest.latitude,
                            visibleRegion.latLngBounds.southwest.longitude,
                            visibleRegion.latLngBounds.northeast.latitude,
                            visibleRegion.latLngBounds.northeast.longitude
                        )
                        mapVenueViewModel.getMapVenueByCategoryInfo(
                            if (venueCategoryId == -1) 0 else venueCategoryId,
                            visibleInfo,
                            binding.etSearch.text.toString()
                        )
                    }
                } else if (it.isEmpty()) {
                    removeGeofence()
                    val visibleRegion = mMap?.projection?.visibleRegion
                    visibleRegion?.let {
                        visibleInfo = arrayListOf(
                            visibleRegion.latLngBounds.southwest.latitude,
                            visibleRegion.latLngBounds.southwest.longitude,
                            visibleRegion.latLngBounds.northeast.latitude,
                            visibleRegion.latLngBounds.northeast.longitude
                        )

                        mapVenueViewModel.getMapVenueByCategoryInfo(
                            if (venueCategoryId == -1) 0 else venueCategoryId,
                            visibleInfo,
                            binding.etSearch.text.toString()
                        )
                    }
                }
            }, {
                Timber.e(it)
            }).autoDispose()

        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(requireContext())
            binding.etSearch.setText("")
        }.autoDispose()

        binding.currentUsersAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            isSelected = !isSelected

            clusterManager?.clearItems()
            if(isSelected) {
                binding.currentUsersAppCompatImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        venueMapContext.resources,
                        R.drawable.ic_map_nearby_user,
                        null
                    )
                )
                val infoVenue = venueMapInfoList.filter { MapVenueUserType.VENUE_OWNER.type.equals(it.userType) }

                venueMapInfoList.filter { MapVenueUserType.USER.type.equals(it.userType) }?.forEach {
                    clusterManager?.removeItem(VenueInfo(
                        it.id,
                        it.latitude?.toDouble() ?: 0.0,
                        it.longitude?.toDouble() ?: 0.0,
                        if (it.userType == MapVenueUserType.VENUE_OWNER.type) it.name.toString() else it.username.toString(),
                        it.avatar ?: ""
                    ))
                }

                clusterManager?.clearItems()
                mMap?.clear()
                prepareVenueMarkerBitmap(infoVenue, true)
            } else {
                binding.currentUsersAppCompatImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        venueMapContext.resources,
                        R.drawable.ic_map_nearby_user_selected,
                        null
                    )
                )
                prepareVenueMarkerBitmap(venueMapInfoList)

            }
        }.autoDispose()
    }

    private fun listenToViewModel() {
        userProfileBorderRadius = resources.getDimension(com.intuit.sdp.R.dimen._2sdp)
        userProfileBorderColor = ContextCompat.getColor(venueMapContext, R.color.md_white)

        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_place_holder_user_map)
        defaultPlaceHolderUser = if (bitmap != null) {
            BitmapDescriptorFactory.fromBitmap(
                bitmap.createBitmapWithBorder(
                    userProfileBorderRadius, userProfileBorderColor
                )
            )
        } else {
            bitmapDescriptorFromVector(venueMapContext, R.drawable.ic_place_holder_user_map)
        }

        mapVenueViewModel.venueCategoryState.subscribeAndObserveOnMainThread { it ->
            when (it) {
                is VenueCategoryViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("VenueCategoryViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        SnackBarUtils.showTopSnackBar(requireView())
                    } else {
                        showToast(it.errorMessage)
                    }
                }

                is VenueCategoryViewState.VenueCategoryList -> {
                    venueCategoryList = it.venueCategoryList
                    venueCategoryAdapter.listOfDataItems = venueCategoryList
                }

                is VenueCategoryViewState.VenueMapInfoListByCategory -> {
//                    venueInfoSet.clear()
                    if (venueCategoryList.isNotEmpty())
                        clusterManager?.clearItems()

                    venueMapInfoList = it.venueMapInfoList as ArrayList<VenueMapInfo>

                    if (it.venueMapInfoList.isNotEmpty()) {
                        if (isSelected) {
                            prepareVenueMarkerBitmap(it.venueMapInfoList.filter { type -> MapVenueUserType.VENUE_OWNER.type == type.userType })
                        } else {
                            prepareVenueMarkerBitmap(it.venueMapInfoList)
                        }
                    }

                    val t: Thread = object : Thread() {
                        override fun run() {
                            if (isResumed && MapVenueUserType.USER.type.equals(loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType)) {
                                if (isAdded && ContextCompat.checkSelfPermission(
                                        requireActivity(),
                                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                                    )
                                    != PackageManager.PERMISSION_GRANTED
                                ) {
                                    if (requestPermission) {
                                        requestPermission = false
                                        // Permission not granted, request it
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            if (ContextCompat.checkSelfPermission(
                                                    requireContext(),
                                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                                ) != PackageManager.PERMISSION_GRANTED
                                            ) {

                                                if(!locationPermissions.isVisible) {
                                                    locationPermissions = LocationPermissions.newInstance()
                                                    locationPermissions.locationState.subscribeAndObserveOnMainThread {
                                                        ActivityCompat.requestPermissions(
                                                            activity ?: requireActivity(),
                                                            arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                                            LOCATION_PERMISSION_REQUEST_CODE
                                                        )
                                                    }
                                                    locationPermissions.show(childFragmentManager, LocationPermissions.Companion.javaClass.name)
                                                }
                                            }
                                        } else {
                                            if(!locationPermissions.isVisible) {
                                                locationPermissions = LocationPermissions.newInstance()
                                                locationPermissions.locationState.subscribeAndObserveOnMainThread {
                                                    ActivityCompat.requestPermissions(
                                                        activity ?: requireActivity(),
                                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                                                        LOCATION_PERMISSION_REQUEST_CODE
                                                    )
                                                }
                                                locationPermissions.show(childFragmentManager, LocationPermissions.Companion.javaClass.name)
                                            }
                                        }
                                    }
                                } else {
                                    // Permission already granted
                                    // You can proceed with using background location
                                    geofenceList.clear()
                                    it.venueMapInfoList.forEach { item ->
                                        if (item.userType == MapVenueUserType.VENUE_OWNER.type) {
                                            val gson = Gson()
                                            val geoFenceResponse = GeoFenceResponse(
                                                item.id,
                                                if ((item.name?.length ?: 0) > 18) item.name?.substring(
                                                    0,
                                                    18
                                                ) else item.name,
                                                item.venueCheckinStatus
                                            )
                                            val originalString = gson.toJson(geoFenceResponse)
                                            val encodedString = Base64.encodeToString(
                                                originalString.toByteArray(Charsets.UTF_8),
                                                Base64.NO_WRAP
                                            )
                                            geofenceList.add(
                                                Geofence.Builder().setRequestId("$encodedString")
                                                    .setCircularRegion(
                                                        (item.latitude?.toDouble() ?: 0) as Double,
                                                        (item.longitude?.toDouble() ?: 0) as Double,
                                                        radius
                                                    )
                                                    .setExpirationDuration(Geofence.GEOFENCE_TRANSITION_EXIT.toLong())
                                                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                                                    .build()
                                            )
                                        }
                                    }
                                    examinePermissionInitiateGeofence()
                                }
                            }
                        }
                    }
                    t.start()
                }

                is VenueCategoryViewState.OtherNearVenueInfoList -> {
                    listOfVenues = (it.listOfVenueMapInfo)
                    venueListAdapter.listOfDataItems = listOfVenues

                    listOfVenues.find { it.venueCheckinStatus == 1 }?.apply {
                        venueCheckinDate?.let {
                            val date1: Date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(it)
                            if (Date().after(date1)) {
                                if (checkInBottomSheet == null) {
                                    checkInBottomSheet = CheckInBottomSheet.newInstance(
                                        GeoFenceResponse(id, name),
                                        true
                                    ).apply {
                                        dismissClick.subscribeAndObserveOnMainThread {
                                            dismiss()
                                            checkInBottomSheet = null
                                        }.autoDispose()
                                    }
                                    checkInBottomSheet?.show(
                                        parentFragmentManager,
                                        CheckInBottomSheet.TAG
                                    )
                                }
                            }
                        }
                    }
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToViewEvents() {
        binding.currentLocationSAppCompatImageView.throttleClicks()
            .subscribeAndObserveOnMainThread {
                currentMapLocationClicked = !currentMapLocationClicked
                if (latitude != 0.0 && longitude != 0.0) {
                    mMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(latitude, longitude),
                            if (currentMapLocationClicked) 18f else 15.5f
                        )
                    )
                }
                userIsInteractingWithMap = false
            }

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        venueCategoryAdapter = NewVenueCategoryAdapter(requireContext())
        venueCategoryAdapter.apply {
            venueCategoryAllClick.subscribeAndObserveOnMainThread {
                if (latitude != 0.toDouble() && longitude != 0.toDouble()) {
                    isAllSelected = true
                    venueCategoryList.forEach { it.isSelected = false }
                    venueCategoryAdapter.listOfDataItems = venueCategoryList

                    venueCategoryId = 0

                    val visibleRegion = mMap?.projection?.visibleRegion

                    visibleRegion?.let {

                        mapVenueViewModel.getMapVenueByCategoryInfo(
                            venueCategoryId,
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
                categoryId = null
                mapVenueViewModel.pullToRefresh(null, null)
            }.autoDispose()
            venueCategoryClick.subscribeAndObserveOnMainThread { venueCategory ->
                if (latitude != 0.toDouble() && longitude != 0.toDouble()) {
                    isAllSelected = false
                    venueCategoryList.forEach { it.isSelected = false }
                    val mPos = venueCategoryList.indexOfFirst { it.id == venueCategory.id }
                    if (mPos != -1) {
                        venueCategoryList[mPos].isSelected = true
                    }
                    venueCategoryAdapter.listOfDataItems = venueCategoryList

                    categoryId = venueCategory.id
                    venueCategoryId = venueCategory.id

                    val visibleRegion = mMap?.projection?.visibleRegion

                    visibleRegion?.let {
                        mapVenueViewModel.getMapVenueByCategoryInfo(
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
                venueCategoryItem = venueCategory.id
                mapVenueViewModel.pullToRefresh(venueCategory.id, null)
            }.autoDispose()
        }
        binding.rvVenueCategoryList.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = venueCategoryAdapter
        }
        mapVenueViewModel.getVenueCategoryList()
    }

    private fun fetchCurrentLocation() {
        println("fetchCurrentLocation: ")
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissions = LocationPermissions.newInstance()
            locationPermissions.locationState.subscribeAndObserveOnMainThread {
                XXPermissions.with(requireContext())
                    .permission(Permission.ACCESS_FINE_LOCATION)
                    .permission(Permission.ACCESS_COARSE_LOCATION)
                    .request(object : OnPermissionCallback {

                        override fun onGranted(permissions: List<String>, all: Boolean) {
                            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                val task = fusedLocationProviderClient.lastLocation
                                task.addOnSuccessListener { location ->
                                    currentLocation = location
                                    liveLocation = location
                                    if (isResumed) {
                                        val supportMapFragment =
                                            (childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment)
                                        supportMapFragment.getMapAsync(this@NewVenueMapFragment)
                                    }
                                }
                            }
                        }

                        override fun onDenied(permissions: List<String>, never: Boolean) {
                            showToast(getString(R.string.msg_location_permission_required_for_venue))
                        }
                    })
            }

            locationPermissions.show(childFragmentManager, LocationPermissions.Companion.javaClass.name)

        }
    }

    override fun onMapReady(p0: GoogleMap) {
        mMap = p0
        p0.setOnMarkerClickListener(this)
        clusterManager = ClusterManager<VenueInfo>(venueMapContext, mMap)
        clusterManager?.renderer = OwnIconRendered(venueMapContext, p0, clusterManager!!)

        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            p0.isMyLocationEnabled = true
            p0.uiSettings.isMyLocationButtonEnabled = false
            p0.uiSettings.isMapToolbarEnabled = false
            updateCurrentLocation()
        }

        mMap?.setOnCameraMoveStartedListener(this)
        mMap?.setOnCameraMoveListener(this)

        mMap?.setOnCameraIdleListener(clusterManager)
        mMap?.setOnMarkerClickListener(clusterManager)

        try {
            // Customise the styling of the base map using a JSON object defined in a raw resource file.
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
            val latLngBuilder = LatLngBounds.Builder()
            if (latitude != 0.0 && longitude != 0.0) {
                addMyLocationMarkerBitmap(latLngBuilder)
            }
            if (!userIsInteractingWithMap)
                p0.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            currentLocation = null
        }

        RxBus.listen(RxEvent.VenueMapFragment::class.java)
            .subscribeOnIoAndObserveOnMainThread({
                if (isApiCalledInit) {
                    isApiCalledInit = false
                    val visibleInfo =  mMap?.projection?.visibleRegion?.let {
                        arrayListOf(it.latLngBounds.southwest.latitude, it.latLngBounds.southwest.longitude,
                            it.latLngBounds.northeast.latitude, it.latLngBounds.northeast.longitude)
                    }

                    if (visibleInfo != null) {
                        mapVenueViewModel.getMapVenueByCategoryInfo(
                            if (venueCategoryId == -1) 0 else venueCategoryId,
                            visibleInfo,
                            binding.etSearch.text.toString()
                        )
                    }
                }
                p0.setOnCameraIdleListener {
                    p0.projection.visibleRegion.latLngBounds.let { bounds ->
                        val currentBounds = arrayListOf(
                            bounds.southwest.latitude,
                            bounds.southwest.longitude,
                            bounds.northeast.latitude,
                            bounds.northeast.longitude
                        )
                        if (areBoundsDifferent(currentBounds)) {
                            mapVenueViewModel.getMapVenueByCategoryInfo(
                                if (venueCategoryId == -1) 0 else venueCategoryId,
                                currentBounds,
                                binding.etSearch.text.toString()
                            )
                        }
                        previousBounds = currentBounds
                    }
                }
            }, {
                Timber.e(it)
            }).autoDispose()
    }

    private fun areBoundsDifferent(currentBounds: List<Double>): Boolean {
        previousBounds?.let { prevBounds ->
            val latDiff1 = Math.abs(currentBounds[0] - prevBounds[0])
            val lngDiff1 = Math.abs(currentBounds[1] - prevBounds[1])
            val latDiff2 = Math.abs(currentBounds[2] - prevBounds[2])
            val lngDiff2 = Math.abs(currentBounds[3] - prevBounds[3])

            return (latDiff1 > SIGNIFICANT_DIFFERENCE_THRESHOLD ||
                    lngDiff1 > SIGNIFICANT_DIFFERENCE_THRESHOLD ||
                    latDiff2 > SIGNIFICANT_DIFFERENCE_THRESHOLD ||
                    lngDiff2 > SIGNIFICANT_DIFFERENCE_THRESHOLD)
        }
        return true
    }

    private fun prepareVenueMarkerBitmap(venueMapInfoList: List<VenueMapInfo>, removePrevious: Boolean = false) {
        for (venueMapInfo in venueMapInfoList) {

            println("User type: " + venueMapInfo.userType)
            val venueInfo = VenueInfo(
                venueMapInfo.id,
                venueMapInfo.latitude?.toDouble() ?: 0.0,
                venueMapInfo.longitude?.toDouble() ?: 0.0,
                if (venueMapInfo.userType == MapVenueUserType.VENUE_OWNER.type) venueMapInfo.name.toString() else venueMapInfo.username.toString(),
                venueMapInfo.avatar ?: ""
            )

            if (!venueInfoSet.contains(venueInfo)) {
                clusterManager?.addItem(venueInfo)
                venueInfoSet.add(venueInfo)
            }
        }
        clusterManager?.cluster()

        if (liveLocation != previousLocation || previousLocation == null || removePrevious) {
            val latLngBuilder = LatLngBounds.Builder()
            if (latitude != 0.0 && longitude != 0.0) {
                addMyLocationMarkerBitmap(latLngBuilder)
            }
            previousLocation = liveLocation
        }
    }

    private fun addMyLocationMarkerBitmap(latLngBuilder: LatLngBounds.Builder) {
        if (isResumed) {
            if (marketInfo != null) {
                marketInfo?.remove()
            }
            latLngBuilder.include(LatLng(latitude, longitude))

            val markerLayout: View = View.inflate(requireContext(), R.layout.layout_map_marker, null)
            val ivUserProfile = markerLayout.findViewById(R.id.myLocation) as AppCompatImageView
            val venueUserInfoFrameLayout = markerLayout.findViewById(R.id.venuePinFrameLayout) as FrameLayout
            val castingMessageFrameLayout = markerLayout.findViewById(R.id.castingMessageFrameLayout) as FrameLayout
            val castMessageAppCompatTextView = markerLayout.findViewById(R.id.castMessageAppCompatTextView) as AppCompatTextView
            val markerEdgeAppCompatImageView = markerLayout.findViewById(R.id.markerEdgeAppCompatImageView) as AppCompatImageView

            castingMessageFrameLayout.isVisible = (loggedInUserCache.getLoggedInUser()?.loggedInUser?.broadcastMessage != null)
            castMessageAppCompatTextView.text = (loggedInUserCache.getLoggedInUser()?.loggedInUser?.broadcastMessage ?: "")

            if (MapVenueUserType.VENUE_OWNER.type.equals(loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType)) {
                castMessageAppCompatTextView.background = ResourcesCompat.getDrawable(venueMapContext.resources, R.drawable.blue_gredient_color, null)
                markerEdgeAppCompatImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        venueMapContext.resources,
                        R.drawable.half_circle_blue,
                        null
                    )
                )
            } else {
                castMessageAppCompatTextView.background =
                    ResourcesCompat.getDrawable(venueMapContext.resources, R.drawable.purple_gradient_color, null)
                markerEdgeAppCompatImageView.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        venueMapContext.resources,
                        R.drawable.half_circle,
                        null
                    )
                )
            }
            ivUserProfile.visibility = View.VISIBLE
            castMessageAppCompatTextView.visibility = View.VISIBLE
            venueUserInfoFrameLayout.visibility = View.GONE

            Glide.with(venueMapContext)
                .asBitmap()
                .load(R.drawable.user_location)
                .placeholder(R.drawable.user_location)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
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
                        activity?.runOnUiThread {
                            if (resource != null) {
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
                                    val markerOptions = MarkerOptions()
                                        .position(LatLng(latitude, longitude))
                                        .draggable(false)
                                        .flat(false)
                                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
                                    marketInfo = mMap?.addMarker(markerOptions)

                                }

                                if (isPageResumed) {
                                    isPageResumed = false
                                    mMap?.animateCamera(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(latitude, longitude),
                                            11f
                                        ), null
                                    )
                                }
                            }
                        }
                        return false
                    }
                })
                .into(ivUserProfile)

        }
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        return true
    }

    private fun askPermission() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if(!locationPermissions.isVisible) {
                locationPermissions = LocationPermissions.newInstance()
                locationPermissions.locationState.subscribeAndObserveOnMainThread {
                    XXPermissions.with(this).permission(Permission.ACCESS_FINE_LOCATION)
                        .permission(Permission.ACCESS_COARSE_LOCATION)
                        .request(object : OnPermissionCallback {

                            override fun onGranted(permissions: List<String>, all: Boolean) {
                                validateGadgetAreaInitiateGeofence()
                            }

                            override fun onDenied(permissions: List<String>, never: Boolean) {
                                showToast(getString(R.string.msg_location_permission_required_for_venue))
                            }
                        })
                }

                locationPermissions.show(childFragmentManager, LocationPermissions.Companion.javaClass.name)
            }
        }

    }

    //specify the geofence to monitor and the initial trigger
    private fun seekGeofencing(): GeofencingRequest {
        return try {
            GeofencingRequest.Builder().apply {
                setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                addGeofences(geofenceList)
            }.build()
        } catch (e: Exception) {
            Timber.e(e, "Error creating GeofencingRequest")
            GeofencingRequest.Builder().build()
        }
    }

    //adding a geofence
    private fun addGeofence() {
        // Check for permission before adding geofences
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geoClient.addGeofences(seekGeofencing(), geofenceIntent).run {
                addOnSuccessListener {
                    Timber.tag("Geofence").d("Geofences added")
                }
                addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to add geofences", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    //removing a geofence
    private fun removeGeofence() {
        if (isVisible) {
            geoClient.removeGeofences(geofenceIntent).run {
                addOnSuccessListener {
                    Timber.tag("Geofence").d("Geofences removed")
                }
                addOnFailureListener {
                    if (isResumed) Toast.makeText(
                        requireContext(), "Failed to remove geofences", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun examinePermissionInitiateGeofence() {
        askPermission()
    }

    // check if background and foreground permissions are approved
    private fun validateGadgetAreaInitiateGeofence() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(requireContext())
        val locationResponses = client.checkLocationSettings(builder.build())
        locationResponses.addOnCompleteListener {
            if (geofenceList.size > 0)
                addGeofence()
        }
        locationResponses.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        requireActivity(), REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Timber.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Toast.makeText(requireContext(), "Enable your location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap?.isMyLocationEnabled = false
            mLocationRequest =
                LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(5000).setFastestInterval(5000);

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    for (location in locationResult.locations) {
                        val currentLatLng = LatLng(location.latitude, location.longitude)
                        if (!userIsInteractingWithMap) {
                            mCurrLocationMarker?.position = currentLatLng
                            latitude = location.latitude
                            longitude = location.longitude
                            liveLocation = location
                        }
                    }
                }
            }
            fusedLocationProviderClient.requestLocationUpdates(
                mLocationRequest!!, locationCallback, Looper.getMainLooper()
            )
        } else {
            if(!locationPermissions.isVisible) {
                locationPermissions = LocationPermissions.newInstance()
                locationPermissions.locationState.subscribeAndObserveOnMainThread {
                    ActivityCompat.requestPermissions(
                        requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
                    )
                }

                locationPermissions.show(childFragmentManager, LocationPermissions.Companion.javaClass.name)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val bounds = loggedInUserCache.getBounds()
        val visibleInfo = if (bounds != null) {
            arrayListOf(bounds.southwest.latitude, bounds.southwest.longitude,
                bounds.northeast.latitude, bounds.northeast.longitude)
        } else {
            mMap?.projection?.visibleRegion?.let {
                arrayListOf(it.latLngBounds.southwest.latitude, it.latLngBounds.southwest.longitude,
                    it.latLngBounds.northeast.latitude, it.latLngBounds.northeast.longitude)
            }
        }

        if (visibleInfo != null && !isMapVenueByCategoryInfoCalled) {
            isMapVenueByCategoryInfoCalled = true
            mapVenueViewModel.getMapVenueByCategoryInfo(
                if (venueCategoryId == -1) 0 else venueCategoryId,
                visibleInfo,
                binding.etSearch.text.toString()
            )
        }
    }

    override fun onCameraMoveStarted(p0: Int) {
        if (p0 == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
            userIsInteractingWithMap = true
        }
    }

    override fun onCameraMove() {}

    override fun onCameraIdle() {}

    override fun onStop() {
        removeGeofence()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mMap?.setOnCameraIdleListener(null)
    }

    inner class OwnIconRendered(
        private val context: Context,
        private val map: GoogleMap,
        clusterManager: ClusterManager<VenueInfo>
    ) : DefaultClusterRenderer<VenueInfo>(context, map, clusterManager) {

        private val MARKER_DIMENSION = 150
        private val CLUSTER_DIMENSION = 250
        private var iconGenerator: IconGenerator? = null
        private var clusterIconGenerator: IconGenerator? = null
        private var markerImageView: ImageView? = null
        private var clusterImageView: ImageView? = null
        private lateinit var clusterManager: ClusterManager<VenueInfo>
        private var googleMap: GoogleMap

        init {
            if (clusterManager != null) {
                this.clusterManager = clusterManager
            }
            this.googleMap = map
            iconGenerator = IconGenerator(context)
            markerImageView = ImageView(context)
            markerImageView!!.layoutParams = ViewGroup.LayoutParams(MARKER_DIMENSION, MARKER_DIMENSION)
            iconGenerator!!.setContentView(markerImageView)
            iconGenerator!!.setBackground(null)

            clusterIconGenerator = IconGenerator(context)
            clusterImageView = ImageView(context)
            clusterImageView!!.layoutParams = ViewGroup.LayoutParams(CLUSTER_DIMENSION, CLUSTER_DIMENSION)
            clusterIconGenerator!!.setContentView(clusterImageView)
            clusterIconGenerator!!.setBackground(null)

            clusterManager.setOnClusterClickListener { cluster ->
                handleClusterClick(cluster)
            }

            clusterManager.setOnClusterItemClickListener { item ->
                if(item != null) {
                    handleMarkerClick(item)
                }

                true
            }
        }


        override fun onBeforeClusterRendered(
            cluster: Cluster<VenueInfo>?,
            markerOptions: MarkerOptions?
        ) {
            if (cluster == null || cluster.size == 0 || cluster.size < 2) {
                return
            }

            val markerLayout: View = View.inflate(context, R.layout.layout_count_marker, null)

            Timber.tag("Cluster").d("onBeforeClusterRendered -> cluster.size: ${cluster.size}")
            val drawable = when {
                cluster.size < 10 -> R.drawable.purple_location_bg
                cluster.size in 10..99 -> R.drawable.cluster_bg
                cluster.size >= 100 -> R.drawable.red_location_bg
                else -> R.drawable.purple_location_bg
            }

            val backgroundImageView = markerLayout.findViewById<AppCompatImageView>(R.id.backgroundAppCompatImageView)

            val layoutParams = backgroundImageView.layoutParams
            layoutParams.width = 200
            layoutParams.height = 200
            backgroundImageView.layoutParams = layoutParams

            backgroundImageView.setImageDrawable(context.resources.getDrawable(drawable))
            markerLayout.findViewById<AppCompatTextView>(R.id.countTextAppCompatTextView).text =
                cluster.size.toString()
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
            var profilePicUrl : String? = null
            var userName : String? = null
            var userType: String? = null
            var broadCastMessage: String? = null
            venueMapInfoList.find { it.name.equals(item?.title) || it.username.equals(item?.title) && it.avatar.equals(item?.snippet) }?.let {
                profilePicUrl = it.avatar
                userName = it.username
                userType = it.userType
                broadCastMessage = it.broadcastMessage
            }

            markerOptions?.title(item?.title.toString())

            if(broadCastMessage.isNullOrEmpty()) {
                markerOptions?.icon(BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)))

                if (!profilePicUrl.isNullOrEmpty()) {

                    Glide.with(context)
                        .asBitmap()
                        .load(profilePicUrl)
                        .placeholder(if (userType.equals(MapVenueUserType.VENUE_OWNER.type)) resources.getDrawable(R.drawable.venue_placeholder, null) else resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                        .error(if (userType.equals(MapVenueUserType.VENUE_OWNER.type)) resources.getDrawable(R.drawable.venue_placeholder, null) else resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                        .override(150, 150)
                        .into(object : SimpleTarget<Bitmap>() {
                            override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                                markerOptions?.icon(BitmapDescriptorFactory.fromBitmap(
                                    bitmap.createBitmapWithBorder(
                                        context,
                                        context.resources.getDimension(com.intuit.sdp.R.dimen._2sdp),
                                        ContextCompat.getColor(context, R.color.white),
                                        userName ?: "",
                                        broadCastMessage = broadCastMessage
                                    )
                                ))
                                val marker = clusterManager?.markerCollection?.addMarker(markerOptions)
                                clusterManager?.addItem(item)
                                marker?.tag = item
                            }
                        })
                } else {
                    // No profile picture, use the default icon
                    markerImageView?.setImageResource(if (userType.equals(MapVenueUserType.VENUE_OWNER.type)) R.drawable.venue_placeholder else R.drawable.ic_chat_user_placeholder)
                    val icon = iconGenerator?.makeIcon()
                    val finalBitmap = icon?.let {
                        it.createBitmapWithBorder(
                            context,
                            context.resources.getDimension(com.intuit.sdp.R.dimen._2sdp),
                            ContextCompat.getColor(context, R.color.white),
                            userName ?: "",
                            broadCastMessage = broadCastMessage
                        )
                    }

                    markerOptions?.icon(finalBitmap?.let { BitmapDescriptorFactory.fromBitmap(it) })

                    val marker = clusterManager?.markerCollection?.addMarker(markerOptions)
                    clusterManager?.addItem(item)
                    marker?.tag = item
                }
            } else {
                val markerLayout: View = View.inflate(context, R.layout.layout_map_marker, null)
                markerLayout.findViewById<AppCompatTextView>(R.id.tvDistance).text = item?.title ?: ""


                Glide.with(context)
                    .load(profilePicUrl)
                    .thumbnail(0.5f)
                    .placeholder(if (userType.equals(MapVenueUserType.VENUE_OWNER.type)) resources.getDrawable(R.drawable.venue_placeholder, null) else resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                    .error(if (userType.equals(MapVenueUserType.VENUE_OWNER.type)) resources.getDrawable(R.drawable.venue_placeholder, null) else resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(markerLayout.findViewById<RoundedImageView>(R.id.ivUserProfile))

                markerLayout.findViewById<AppCompatTextView>(R.id.castMessageAppCompatTextView).isVisible = (broadCastMessage != null)
                markerLayout.findViewById<AppCompatTextView>(R.id.castMessageAppCompatTextView).text = broadCastMessage
                markerLayout.findViewById<FrameLayout>(R.id.castingMessageFrameLayout).isVisible = (broadCastMessage != null)

                if (userType?.equals(MapVenueUserType.USER.type) == true) {
                    markerLayout.findViewById<AppCompatTextView>(R.id.castMessageAppCompatTextView).background = ResourcesCompat.getDrawable(context.resources, R.drawable.blue_gredient_color, null)
                    markerLayout.findViewById<AppCompatImageView>(R.id.markerEdgeAppCompatImageView).setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.half_circle_blue, null))
                } else {
                    markerLayout.findViewById<AppCompatTextView>(R.id.castMessageAppCompatTextView).background = ResourcesCompat.getDrawable(context.resources,R.drawable.purple_gradient_color, null)
                    markerLayout.findViewById<AppCompatImageView>(R.id.markerEdgeAppCompatImageView).setImageDrawable(ResourcesCompat.getDrawable(context.resources, R.drawable.half_circle, null))
                }
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




//            markerOptions?.title(item?.title)
//            markerOptions?.snippet(item?.snippet)
        }

        private fun handleMarkerClick(marker: VenueInfo): Boolean {
            Timber.tag("HandleMarkerClick").d("handleMarkerClick -> marker: $marker")

            val tagVenue: VenueMapInfo? = venueMapInfoList.find {
                it.name == marker.title && it.avatar == marker.snippet
            } ?: venueMapInfoList.find {
                it.name == marker.title
            } ?: venueMapInfoList.find {
                it.username == marker.title
            }

            tagVenue?.let { tag ->
                when (tag.userType) {
                    MapVenueUserType.USER.type -> {
                        context.startActivity(NewOtherUserProfileActivity.getIntent(context, tag.userId ?: 0))
                    }
                    MapVenueUserType.VENUE_OWNER.type -> {
                        when {
                            (tag.isLive ?: 0) > 0 -> {
                                context.startActivity(VideoRoomsActivity.getIntentLive(context, tag.liveId))
                            }
                            tag.isTagged == 1 && (tag.atVenueCount ?: 0) > 0 -> {
                                context.startActivity(VenueTaggedActivity.getIntent(
                                    context,
                                    tag.id,
                                    tag.isReel ?: -1,
                                    tag.isPost ?: -1,
                                    tag.spontyCount ?: -1
                                ))
                            }
                            else -> {
                                context.startActivity(NewVenueDetailActivity.getIntent(
                                    context,
                                    tag.category?.firstOrNull()?.id ?: 0,
                                    tag.id
                                ))
                            }
                        }
                    }
                }
            }
            return true
        }

        private fun handleClusterClick(cluster: Cluster<VenueInfo>): Boolean {
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(cluster.position, map.cameraPosition.zoom + 2)
            map.animateCamera(cameraUpdate)
            return true
        }
    }
}