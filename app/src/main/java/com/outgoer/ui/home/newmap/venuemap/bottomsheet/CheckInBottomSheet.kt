package com.outgoer.ui.home.newmap.venuemap.bottomsheet

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.CheckInOutRequest
import com.outgoer.api.venue.model.GeoFenceResponse
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.roundDoubleVal
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.CheckInBottomsheetBinding
import com.outgoer.ui.home.map.venuemap.viewmodel.MapVenueViewModel
import com.outgoer.ui.home.map.venuemap.viewmodel.VenueCategoryViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class CheckInBottomSheet : BaseBottomSheetDialogFragment() {

    private var _binding: CheckInBottomsheetBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MapVenueViewModel>
    private lateinit var mapVenueViewModel: MapVenueViewModel

    @Inject
    lateinit var loggedInUserCache : LoggedInUserCache

    private var venueId: Int = -1

    companion object {
        val TAG: String = "RegisterBottomSheet"
        private val GEO_FENCE = "GEO_FENCE"
        private val IS_CHECK_IN = "IS_CHECK_IN"

        @JvmStatic
        fun newInstance(geoFence: GeoFenceResponse?, isCheckIn: Boolean): CheckInBottomSheet {
            val checkInBottomSheet = CheckInBottomSheet()
            val bundle = Bundle()
            bundle.putParcelable(GEO_FENCE, geoFence)
            bundle.putBoolean(IS_CHECK_IN, isCheckIn)
            checkInBottomSheet.arguments = bundle
            return checkInBottomSheet
        }
    }

    private var dismissClickSubscribe: PublishSubject<String> = PublishSubject.create()
    val dismissClick: Observable<String> = dismissClickSubscribe.hide()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        mapVenueViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CheckInBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        dialog?.apply {
            val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.peekHeight = 0
        }

        listenToViewModel()
        arguments?.let {
            val geoFence = it.getParcelable<GeoFenceResponse>(GEO_FENCE)

            venueId = geoFence?.id ?: -1

            if(it.getBoolean(IS_CHECK_IN)) {
                binding.checkinMaterialButton.text = resources.getString(R.string.check_out)
            } else {
                binding.checkinMaterialButton.text = resources.getString(R.string.check_in)
            }

            val list = mapVenueViewModel.getListOfVenue()
            list.find { it.id.equals(geoFence?.id) }?.apply {
                Glide.with(requireContext())
                    .load(avatar)
                    .error(R.drawable.venue_placeholder)
                    .into(binding.ivPlaceImage)

                binding.tvPlaceName.text = name
                binding.tvPlaceDescription.text = venueAddress
                binding.tvPlaceRatingCount.text = "${reviewAvg}"
                if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                    binding.distanceAppCompatTextView.text = distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_miles))
                } else {
                    binding.distanceAppCompatTextView.text = distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_kms))
                }
            }
        }

        binding.checkinMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if(!(-1).equals(venueId)) {
                if(arguments?.getBoolean(IS_CHECK_IN) ?: false) {
                    mapVenueViewModel.checkInOutVenue(CheckInOutRequest(venueId, 0))
                } else {
                    mapVenueViewModel.checkInOutVenue(CheckInOutRequest(venueId, 1))
                }
            }
        }

        binding.notNowMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            dismissClickSubscribe.onNext("")
        }
    }

    private fun listenToViewModel() {
        mapVenueViewModel.venueCategoryState.subscribeAndObserveOnMainThread {
            when(it) {
                is VenueCategoryViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                    dismissClickSubscribe.onNext("")
                }
                is VenueCategoryViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is VenueCategoryViewState.LoadingState -> {}
                else -> {}
            }
        }
    }

}