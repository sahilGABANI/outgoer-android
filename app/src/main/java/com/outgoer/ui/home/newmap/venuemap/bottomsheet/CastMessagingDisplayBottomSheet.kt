package com.outgoer.ui.home.newmap.venuemap.bottomsheet

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.profile.model.NearByUserResponse
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
import com.outgoer.databinding.CastMessageDisplayBottomsheetBinding
import com.outgoer.databinding.CheckInBottomsheetBinding
import com.outgoer.ui.home.map.venuemap.viewmodel.MapVenueViewModel
import com.outgoer.ui.home.map.venuemap.viewmodel.VenueCategoryViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class CastMessagingDisplayBottomSheet: BaseBottomSheetDialogFragment() {

    private var _binding: CastMessageDisplayBottomsheetBinding? = null
    private val binding get() = _binding!!

    private var nearByUserResponse: NearByUserResponse? = null
    companion object {
        val NEARBY_USER_INFO = "NEARBY_USER_INFO"
        @JvmStatic
        fun newInstance(broadCastMessage: NearByUserResponse): CastMessagingDisplayBottomSheet {
            var castMessagingBottomSheet = CastMessagingDisplayBottomSheet()

            if(broadCastMessage != null) {
                var bundle = Bundle()
                bundle.putParcelable(NEARBY_USER_INFO, broadCastMessage)
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
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CastMessageDisplayBottomsheetBinding.inflate(inflater, container, false)
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
    }

    private fun listenToViewEvent() {
        arguments?.let {
            nearByUserResponse = it.getParcelable<NearByUserResponse>(NEARBY_USER_INFO)
        }

        binding.tvUsername.text = nearByUserResponse?.username ?: nearByUserResponse?.name
        binding.ivVerified.isVisible = nearByUserResponse?.profileVerified == 1
        binding.castMessageAppCompatTextView.text = nearByUserResponse?.broadcastMessage

        Glide.with(requireContext())
            .load(nearByUserResponse?.avatar ?: "")
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .error(R.drawable.ic_chat_user_placeholder)
            .into(binding.ivUserProfile)
    }

}