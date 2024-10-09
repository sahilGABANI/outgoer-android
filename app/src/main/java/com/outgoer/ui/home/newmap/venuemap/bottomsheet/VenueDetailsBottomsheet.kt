package com.outgoer.ui.home.newmap.venuemap.bottomsheet

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.chat.model.ChatConversationInfo
import com.outgoer.api.friend_venue.model.CheckInVenueResponse
import com.outgoer.api.friend_venue.model.VenueDetails
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.BottomsheetVenueDetailsBinding
import com.outgoer.ui.chat.NewChatActivity
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.view.FriendsVenueAdapter
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.text.DecimalFormat
import javax.inject.Inject

class VenueDetailsBottomsheet: BaseBottomSheetDialogFragment() {

    private var _binding: BottomsheetVenueDetailsBinding? = null
    private val binding get() = _binding!!

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueDetailsViewModel>
    private lateinit var venueDetailsViewModel: VenueDetailsViewModel
    private var venueDetails: VenueDetails? = null

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        val TAG: String = "VenueDetailsBottomSheet"
        val VENUE_ID: String = "VENUE_ID"

        @JvmStatic
        fun newInstance(venueId: Int): VenueDetailsBottomsheet {
            val venueDetailsBottomsheet =  VenueDetailsBottomsheet()

            val bundle = Bundle()
            bundle.putInt(VENUE_ID, venueId)

            venueDetailsBottomsheet.arguments = bundle

            return venueDetailsBottomsheet
        }
    }

    private var checkInClickSubscribe: PublishSubject<String> = PublishSubject.create()
    val checkInClick: Observable<String> = checkInClickSubscribe.hide()

    private lateinit var friendsVenueAdapter: FriendsVenueAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)
        venueDetailsViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetVenueDetailsBinding.inflate(inflater, container, false)
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
        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        friendsVenueAdapter = FriendsVenueAdapter(requireContext()).apply {
            messageClick.subscribeAndObserveOnMainThread {
//                startActivity(ConversationAc)/

                startActivityWithDefaultAnimation(
                    NewChatActivity.getIntent(
                    requireContext(), ChatConversationInfo(senderId = loggedInUserCache.getUserId() ?: 0, receiverId = it.id, name = it.name, profileUrl = it.avatar, chatType = resources.getString(R.string.label_chat))
                ))
            }
            profileClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(NewOtherUserProfileActivity.getIntent(requireContext(), it.id))
            }
        }

        binding.friendsVenueRecyclerView.apply {
            adapter = friendsVenueAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        arguments?.let {
            venueDetailsViewModel.getVenueCategoryList(CheckInVenueResponse(it.getInt(VENUE_ID)))
        }

        binding.ivPlaceImage.throttleClicks().subscribeAndObserveOnMainThread {
            venueDetails?.let {
                startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(requireContext(), 0, it.id))
            }
        }
    }

    private fun listenToViewModel() {
        venueDetailsViewModel.venueDetailsState.subscribeAndObserveOnMainThread {
            when(it) {
                is VenueDetailsViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                    dismissBottomSheet()
                }
                is VenueDetailsViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is VenueDetailsViewState.LoadingState -> {
                    if(it.isLoading) {
                        binding.venueBottomSheetLinearLayout.visibility = View.GONE
                        binding.loadingProgressBar.visibility = View.VISIBLE
                    } else {
                        binding.venueBottomSheetLinearLayout.visibility = View.VISIBLE
                        binding.loadingProgressBar.visibility = View.GONE
                    }
                }
                is VenueDetailsViewState.VenueDetailsInfo -> {
                    venueDetails = it.venueDetails

                    binding.tvPlaceName.text = it.venueDetails.name
                    binding.tvPlaceRatingCount.text = it.venueDetails.reviewAvg.toString()
                    if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                        binding.distanceAppCompatTextView.text = if (it.venueDetails.distance != 0.00) {
                            it.venueDetails.distance.roundDoubleVal().plus(" ").plus(getString(R.string.label_miles))
                        } else {
                            "0 ".plus(getString(R.string.label_miles))
                        }
                    } else {
                        binding.distanceAppCompatTextView.text = if (it.venueDetails.distance != 0.00) {
                            it.venueDetails.distance.roundDoubleVal().plus(" ").plus(getString(R.string.label_kms))
                        } else {
                            "0 ".plus(getString(R.string.label_kms))
                        }
                    }

                    Glide.with(requireContext())
                        .load(it.venueDetails.avatar)
                        .error(R.drawable.venue_placeholder)
                        .into(binding.ivPlaceImage)
                }
                is VenueDetailsViewState.VenueCheckInUserInfo -> {
                    binding.titleAppCompatTextView.visibility = if(it.venueMapInfoList.size == 0) View.GONE else View.VISIBLE
                    friendsVenueAdapter.listOfDataItems = it.venueMapInfoList
                }
            }
        }
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}