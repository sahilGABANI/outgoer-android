package com.outgoer.ui.venue

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.StrictMode
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.jakewharton.rxbinding3.widget.editorActions
import com.jakewharton.rxbinding3.widget.textChanges
import com.outgoer.BuildConfig
import com.outgoer.R
import com.outgoer.api.event.model.GooglePlaces
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.subscribeOnIoAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.BottomSheetVenueAvailabilityBinding
import com.outgoer.ui.createevent.viewmodel.CreateEventsViewModel
import com.outgoer.ui.sponty.location.PlaceSearch
import com.outgoer.ui.sponty.location.model.PlaceSearchResponse
import com.outgoer.ui.sponty.location.model.Predictions
import com.outgoer.ui.sponty.location.model.ResultResponse
import com.outgoer.ui.venue.view.VenueLocationAdapter
import com.outgoer.utils.SnackBarUtils.showTopSnackBar
import com.outgoer.utils.UiUtils
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SpontyVenueLocationBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: BottomSheetVenueAvailabilityBinding? = null
    private val binding get() = _binding!!

    private lateinit var venueLocationAdapter: VenueLocationAdapter
    private var currentLocation: String? = null
    private var fromVenueRegister: Boolean? = false

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val venueLocationClickSubject: PublishSubject<ResultResponse> = PublishSubject.create()
    val venueLocationClick: Observable<ResultResponse> = venueLocationClickSubject.hide()


    private val googlePlacesClickSubject: PublishSubject<GooglePlaces> = PublishSubject.create()
    val googlePlacesClick: Observable<GooglePlaces> = googlePlacesClickSubject.hide()

    private val placeAvailableClickSubject: PublishSubject<Predictions> = PublishSubject.create()
    val placeAvailableClick: Observable<Predictions> = placeAvailableClickSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<CreateEventsViewModel>
    private lateinit var createEventsViewModel: CreateEventsViewModel

    companion object {
        private val VENUE_AVAILABILITY = "VENUE_AVAILABILITY"
        private val FROM_VENUE_REGISTER = "FROM_VENUE_REGISTER"

        @JvmStatic
        fun newInstance(): SpontyVenueLocationBottomSheet {
            return SpontyVenueLocationBottomSheet()
        }


        fun newInstance(fromVenueRegister :Boolean): SpontyVenueLocationBottomSheet {
            val checkInBottomSheet = SpontyVenueLocationBottomSheet()
            val bundle = Bundle()
            bundle.putBoolean(FROM_VENUE_REGISTER, fromVenueRegister)
            checkInBottomSheet.arguments = bundle
            return checkInBottomSheet
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        OutgoerApplication.component.inject(this)

        createEventsViewModel = getViewModelFromFactory(viewModelFactory)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetVenueAvailabilityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            }
        }
        fromVenueRegister = arguments?.getBoolean(FROM_VENUE_REGISTER)
        locationPermission()
        listenToViewEvents()
        listenToViewModel()
    }

    private fun listenToViewModel() {

        createEventsViewModel.eventsViewState.subscribeAndObserveOnMainThread {
            when(it) {
                is com.outgoer.ui.createevent.viewmodel.EventViewState.LoadingState -> {}
                is com.outgoer.ui.createevent.viewmodel.EventViewState.ErrorMessage -> {
                    Timber.tag("ErrorMessage").e("EventViewState -> it.errorMessage: ${it.errorMessage}")
                    if (it.errorMessage.startsWith("Unable to resolve host")) {
                        showTopSnackBar(requireView())
                    } else {
                        showToast(it.errorMessage)
                    }
                }
                is com.outgoer.ui.createevent.viewmodel.EventViewState.ListOfGoogleMap -> {
                    venueLocationAdapter.listofGooglePlaces = it.event
                }
                else -> {}
            }
        }
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback =
        object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                   dismiss()
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        }
    private fun listenToViewEvents() {
        binding.llSearch.visibility = View.VISIBLE
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        if (fromVenueRegister == false || fromVenueRegister == null) {
            createEventsViewModel.getNearGooglePlaces()
        } else {
            addCompleteQuerySearch("Ab")
        }
        venueLocationAdapter = VenueLocationAdapter(requireContext()).apply {
            googlePlacesClick.subscribeAndObserveOnMainThread {
                googlePlacesClickSubject.onNext(it)
                dismissBottomSheet()
            }
        }
        binding.dataRecyclerView.apply {
            adapter = venueLocationAdapter
        }

        binding.etSearch.editorActions()
            .filter { action -> action == EditorInfo.IME_ACTION_SEARCH }
            .subscribeAndObserveOnMainThread {
                UiUtils.hideKeyboard(requireContext())
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
            .debounce(100, TimeUnit.MILLISECONDS, Schedulers.io())
            .subscribeOnIoAndObserveOnMainThread({
                if (it.length > 2) {
                    if (fromVenueRegister == false || fromVenueRegister == null) {
                        createEventsViewModel.getNearGooglePlaces(it.toString())
                    } else {
                        addCompleteQuerySearch(it.toString())
                    }
                }
                UiUtils.hideKeyboard(requireContext())
            }, {
                Timber.e(it)
            }).autoDispose()


        binding.ivClear.throttleClicks().subscribeAndObserveOnMainThread {
            UiUtils.hideKeyboard(requireContext())
            binding.etSearch.setText("")
            binding.tvNoLocation.isVisible = false
        }.autoDispose()
    }

    private fun addCompleteQuerySearch(search: String) {
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
        val rectangleCoordinates = "13.0013560,75.1743990%7C13.3436680,80.2720550"


        retrofitService.getAutoCompleteLists("AIzaSyBxVTyN0YmOdGY4e92OObhtYCXC0VtxLB8", search,"rectangle:$rectangleCoordinates")
            .enqueue(object :
                Callback<PlaceSearchResponse> {
                override fun onResponse(
                    call: Call<PlaceSearchResponse>,
                    response: Response<PlaceSearchResponse>
                ) {
                    Log.e("Response %s", response.body()?.toString() ?: "")
                    response.body()?.predictions?.let {
                        venueLocationAdapter.listofPredictions = it as ArrayList<Predictions>
                        if(search != "") {
                            binding.tvNoLocation.isVisible = venueLocationAdapter.listofPredictions.isNullOrEmpty()
                        }
                        if (isVisible) {
                            UiUtils.hideKeyboard(requireContext())
                        }
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

                            latitude = lat
                            longitude = longi
                            currentLocation = "${lat}, ${longi}"
//                            placeSearch("cafe,bar,restaurants,hotel")

                            if (fromVenueRegister == false || fromVenueRegister == null) {
                                createEventsViewModel.getNearGooglePlaces()
                            } else {
                                addCompleteQuerySearch("ab")
                            }
                        }
                    }
                }

                override fun onDenied(permissions: List<String>, never: Boolean) {
                    val locationPermissionGranted = false
                }
            })
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}