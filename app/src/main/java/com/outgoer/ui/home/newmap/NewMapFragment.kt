package com.outgoer.ui.home.newmap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import cn.jzvd.Jzvd
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.profile.model.LocationUpdateRequest
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.extension.showLongToast
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.databinding.FragmentNewMapBinding
import com.outgoer.ui.home.newmap.venueevents.VenueEventsFragment
import com.outgoer.ui.home.newmap.venuemap.NewVenueMapFragment
import timber.log.Timber
import javax.inject.Inject

class NewMapFragment : BaseFragment() {
    companion object {
        private var selectedTab: String = "Map"
        @JvmStatic
        fun newInstance() = NewMapFragment()
    }

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var _binding: FragmentNewMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapTabAdapter: MapTabAdapter
    private var latitude = 0.toDouble()
    private var longitude = 0.toDouble()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNewMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OutgoerApplication.component.inject(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        checkLocationPermission()
        listenToViewEvents()
        RxBus.listen(RxEvent.RefreshMapPage::class.java).subscribeOnIoAndObserveOnMainThread({
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            checkLocationPermission()
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    fun refreshPages() {
        Jzvd.goOnPlayOnPause()
        RxBus.publish(RxEvent.DataReload(selectedTab))
    }

    private fun listenToViewEvents() {
        val lat = loggedInUserCache.getLocationLatitude()
        val lng = loggedInUserCache.getLocationLongitude()
        if (lat.isEmpty() && lng.isNotEmpty()) {
            if (lat.toDouble() != 0.toDouble() && lng.toDouble() != 0.toDouble()) {
                this.latitude = lat.toDouble()
                this.longitude = lng.toDouble()
            }
        }

        Jzvd.goOnPlayOnPause()
        val fragmentList = mutableListOf<Fragment>()
        fragmentList.add(NewVenueMapFragment(latitude, longitude))
        fragmentList.add(VenueEventsFragment.newInstance())

        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 2
        mapTabAdapter = MapTabAdapter(requireActivity())
        mapTabAdapter.addFragment(fragmentList)
        binding.viewPager.adapter = mapTabAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.label_map)
                }
                1 -> {
                    tab.text = getString(R.string.label_events)
                }
            }}.attach()


        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedTab = tab?.text.toString()
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }
        })

    }

    private fun checkLocationPermission() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        Timber.tag("<><>").e(location.latitude.toString().plus(", ").plus(location.longitude.toString()))
                        latitude = location.latitude
                        longitude = location.longitude

                        loggedInUserCache.setLocation(
                            LocationUpdateRequest(
                                latitude = location.latitude.toString(),
                                longitude = location.longitude.toString()
                            )
                        )

                        val southwest = LatLng(latitude, longitude)
                        val northeast = calculateNortheastBound(latitude, longitude)
                        val bounds = LatLngBounds(southwest, northeast)
                        loggedInUserCache.setLocationBounds(bounds)
                        listenToViewEvents()
                    }
                }.addOnFailureListener { exception ->
                    exception.localizedMessage?.let {
                        showLongToast(it)
                    }
                }
            }
        } catch (e: Exception) {
            e.localizedMessage?.let {
                showLongToast(it)
            }
        }
    }

    private fun calculateNortheastBound(latitude: Double, longitude: Double): LatLng {
        val offset = 0.01
        val newLatitude = latitude + offset
        val newLongitude = longitude + offset
        return LatLng(newLatitude, newLongitude)
    }

    override fun onResume() {
        super.onResume()
        if(isVisible && isAdded) {
            RxBus.publish(RxEvent.VenueMapFragment)
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}