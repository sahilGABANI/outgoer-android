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
import com.outgoer.api.venue.model.BroadcastMessageRequest
import com.outgoer.api.venue.model.CheckInOutRequest
import com.outgoer.api.venue.model.GeoFenceResponse
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.hideKeyboard
import com.outgoer.base.extension.roundDoubleVal
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.CastMessageBottomsheetBinding
import com.outgoer.databinding.CheckInBottomsheetBinding
import com.outgoer.ui.home.map.venuemap.viewmodel.MapVenueViewModel
import com.outgoer.ui.home.map.venuemap.viewmodel.VenueCategoryViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class CastMessagingBottomSheet: BaseBottomSheetDialogFragment() {

    private var _binding: CastMessageBottomsheetBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MapVenueViewModel>
    private lateinit var mapVenueViewModel: MapVenueViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private var venueId: Int = -1

    companion object {
        val BROADCAST_MESSAGE = "BROADCAST_MESSAGE"
        @JvmStatic
        fun newInstance(broadCastMessage: String? = null): CastMessagingBottomSheet {
            var castMessagingBottomSheet = CastMessagingBottomSheet()

            if(broadCastMessage != null) {
                var bundle = Bundle()
                bundle.putString(BROADCAST_MESSAGE, broadCastMessage)
                castMessagingBottomSheet.arguments = bundle
            }

            return castMessagingBottomSheet
        }
    }

    private var dismissClickSubscribe: PublishSubject<String> = PublishSubject.create()
    val dismissClick: Observable<String> = dismissClickSubscribe.hide()

    var broadCastMessage: String? = null


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
        _binding = CastMessageBottomsheetBinding.inflate(inflater, container, false)
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

        listenToViewEvent()
        listenToViewModel()
    }

    private fun listenToViewEvent() {
        arguments?.let {
            broadCastMessage = it.getString(BROADCAST_MESSAGE, null)
        }

        if(broadCastMessage.isNullOrEmpty()) {
            binding.saveMaterialButton.text = resources.getString(R.string.send)
            binding.deleteMaterialButton.visibility = View.GONE
        } else {
            binding.castMessageAppCompatEditText.setText(broadCastMessage)
            binding.saveMaterialButton.text = resources.getString(R.string.label_save)
            binding.deleteMaterialButton.visibility = View.VISIBLE
        }
        binding.saveMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            if (!binding.castMessageAppCompatEditText.text.toString().isNullOrEmpty()) {
                val outgoer = loggedInUserCache.getLoggedInUser()?.loggedInUser
                outgoer?.broadcastMessage = binding.castMessageAppCompatEditText.text.toString()
                loggedInUserCache.setLoggedInUser(outgoer)
                mapVenueViewModel.broadcastMessage(BroadcastMessageRequest(binding.castMessageAppCompatEditText.text.toString()))
                requireContext().hideKeyboard()
                dismissClickSubscribe.onNext(binding.castMessageAppCompatEditText.text.toString())
                binding.castMessageAppCompatEditText.text?.clear()
            }
        }.autoDispose()
        binding.deleteMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            val outgoer = loggedInUserCache.getLoggedInUser()?.loggedInUser
            outgoer?.broadcastMessage = null
            loggedInUserCache.setLoggedInUser(outgoer)

            binding.castMessageAppCompatEditText.text?.clear()
            mapVenueViewModel.broadcastMessage(BroadcastMessageRequest(""))
            requireContext().hideKeyboard()
            dismissClickSubscribe.onNext("")
        }.autoDispose()
    }

    private fun listenToViewModel() {
        mapVenueViewModel.venueCategoryState.subscribeAndObserveOnMainThread {
            when(it) {
                is VenueCategoryViewState.SuccessMessage -> {
//                    showToast(it.successMessage)
                    dismiss()
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