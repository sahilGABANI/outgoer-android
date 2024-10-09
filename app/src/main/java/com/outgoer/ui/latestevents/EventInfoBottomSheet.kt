package com.outgoer.ui.latestevents

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.venue.model.VenueEventInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.EventInfoBottomSheetBinding
import com.outgoer.ui.latestevents.viewmodel.LatestEventsViewModel
import com.outgoer.ui.latestevents.viewmodel.LatestEventsViewState
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class EventInfoBottomSheet(
    private var venueEventInfo: VenueEventInfo
) : BaseBottomSheetDialogFragment() {

    private val refreshEventsSubject: PublishSubject<Unit> = PublishSubject.create()
    val refreshEvents: Observable<Unit> = refreshEventsSubject.hide()

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<LatestEventsViewModel>
    private lateinit var latestEventsViewModel: LatestEventsViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var outgoerUser: OutgoerUser
    private var loggedInUserId by Delegates.notNull<Int>()

    private var _binding: EventInfoBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        latestEventsViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.event_info_bottom_sheet, container, false)
        _binding = EventInfoBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        context?.let {
            (view.parent as View).setBackgroundColor(ContextCompat.getColor(it, R.color.colorFullTransparent))
        }
        listenToViewModel()
        listenToViewEvent()

        dialog?.apply {
            val bottomSheetDialog = this as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior: BottomSheetBehavior<*> = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
                behavior.addBottomSheetCallback(mBottomSheetBehaviorCallback)
            }
        }
    }

    private val mBottomSheetBehaviorCallback: BottomSheetBehavior.BottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED || newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss()
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val bottomSheet = (view?.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun listenToViewModel() {
        latestEventsViewModel.latestEventsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is LatestEventsViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is LatestEventsViewState.LoadingState -> {
                    buttonVisibility(it.isLoading)
                }
                is LatestEventsViewState.RequestJoin -> {
                    showLongToast(it.successMessage)
                    refreshEventsSubject.onNext(Unit)
                    dismiss()
                }

                else -> {}
            }
        }.autoDispose()
    }

    private fun listenToViewEvent() {
        outgoerUser = loggedInUserCache.getLoggedInUser()?.loggedInUser ?: return
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        if (venueEventInfo.userId == loggedInUserId) {
            binding.rlFooter.visibility = View.GONE
        } else {
            binding.btnRequestJoin.visibility = View.VISIBLE
        }

        binding.tvEventName.text = venueEventInfo.eventName
        binding.tvEventDescriptions.text = venueEventInfo.eventDetails
        binding.tvEventDate.text = getFormattedDateForEvent(venueEventInfo.eventStartDate)
        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
            binding.tvDistance.text = if (venueEventInfo.distance != null) {
                venueEventInfo.distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_miles))
            } else {
                "0 ".plus(getString(R.string.label_miles))
            }
        } else {
            binding.tvDistance.text = if (venueEventInfo.distance != null) {
                venueEventInfo.distance?.roundDoubleVal().plus(" ").plus(getString(R.string.label_kms))
            } else {
                "0 ".plus(getString(R.string.label_kms))
            }
        }

        Glide.with(this)
            .load(venueEventInfo.eventImage)
            .placeholder(R.drawable.venue_placeholder)
            .error(R.drawable.venue_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.eventImageView)

        val eventRequestStatus = venueEventInfo.eventRequestStatus
        if (eventRequestStatus != null) {
            if (eventRequestStatus) {
                binding.btnRequestJoin.text = getString(R.string.label_requested_for_join)
            } else {
                binding.btnRequestJoin.text = getString(R.string.label_request_to_join)
            }
        } else {
            binding.btnRequestJoin.text = getString(R.string.label_request_to_join)
        }

        binding.btnRequestJoin.throttleClicks().subscribeAndObserveOnMainThread {
            latestEventsViewModel.requestJoinEvent(venueEventInfo.id)
        }.autoDispose()

        binding.ivDirection.throttleClicks().subscribeAndObserveOnMainThread {
            venueEventInfo.apply {
                openGoogleMapWithProvidedLatLng(requireContext(), latitude, longitude)
            }
        }.autoDispose()
    }

    private fun buttonVisibility(isLoading: Boolean) {
        if (isLoading) {
            binding.btnRequestJoin.visibility = View.INVISIBLE
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnRequestJoin.visibility = View.VISIBLE
            binding.progressBar.visibility = View.INVISIBLE
        }
    }
}