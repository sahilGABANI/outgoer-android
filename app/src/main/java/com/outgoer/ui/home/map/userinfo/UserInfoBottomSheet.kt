package com.outgoer.ui.home.map.userinfo

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.VenueMapInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseBottomSheetDialogFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.UserInfoBottomSheetBinding
import com.outgoer.ui.home.map.userinfo.viewmodel.UserInfoViewModel
import com.outgoer.ui.newvenuedetail.NewVenueDetailActivity
import com.outgoer.ui.otherprofile.NewOtherUserProfileActivity
import javax.inject.Inject

class UserInfoBottomSheet(
    private val venueMapInfo: VenueMapInfo
) : BaseBottomSheetDialogFragment() {

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<UserInfoViewModel>
    private lateinit var userInfoViewModel: UserInfoViewModel

    @Inject
    lateinit var loggedInUserCache : LoggedInUserCache

    private var _binding: UserInfoBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BSDialogTheme)

        OutgoerApplication.component.inject(this)
        userInfoViewModel = getViewModelFromFactory(viewModelFactory)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.user_info_bottom_sheet, container, false)
        _binding = UserInfoBottomSheetBinding.bind(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheet = (view.parent as View)
        bottomSheet.backgroundTintMode = PorterDuff.Mode.CLEAR
        bottomSheet.setBackgroundColor(Color.TRANSPARENT)

        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        binding.tvUsername.text = venueMapInfo.username ?: ""
        binding.tvAbout.text = venueMapInfo.about

        if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
            binding.tvDistance.text = if (venueMapInfo.distance != null) {
                venueMapInfo.distance.roundDoubleVal().plus(" ").plus(getString(R.string.label_miles))
            } else {
                "0 ".plus(getString(R.string.label_miles))
            }
        } else {
            binding.tvDistance.text = if (venueMapInfo.distance != null) {
                venueMapInfo.distance.roundDoubleVal().plus(" ").plus(getString(R.string.label_kms))
            } else {
                "0 ".plus(getString(R.string.label_kms))
            }
        }

        Glide.with(this)
            .load(venueMapInfo.avatar)
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .error(R.drawable.ic_chat_user_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .into(binding.ivUserProfile)

        if (venueMapInfo.followStatus == null) {
            binding.btnFollow.visibility = View.VISIBLE
            binding.btnFollowing.visibility = View.GONE
        } else {
            if (venueMapInfo.followStatus == 1) {
                binding.btnFollow.visibility = View.GONE
                binding.btnFollowing.visibility = View.VISIBLE
            } else {
                binding.btnFollow.visibility = View.VISIBLE
                binding.btnFollowing.visibility = View.GONE
            }
        }

        binding.llUserInfo.throttleClicks().subscribeAndObserveOnMainThread {
            if(venueMapInfo.userType == MapVenueUserType.VENUE_OWNER.type) {
                if(loggedInUserCache.getUserId() ==  venueMapInfo.userId ) {
                    RxBus.publish(RxEvent.OpenVenueUserProfile)
                }else {
                    startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(requireContext(),0,venueMapInfo.userId  ?: 0))
                }
            } else {
                startActivityWithDefaultAnimation(NewOtherUserProfileActivity.getIntent(requireContext(), venueMapInfo.userId ?: 0))
            }
            dismissBottomSheet()
        }.autoDispose()

        binding.ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
            if(venueMapInfo.userType == MapVenueUserType.VENUE_OWNER.type) {
                if(loggedInUserCache.getUserId() == venueMapInfo.userId ) {
                    RxBus.publish(RxEvent.OpenVenueUserProfile)
                }else {
                    startActivityWithDefaultAnimation(NewVenueDetailActivity.getIntent(requireContext(),0,venueMapInfo.userId ?: 0))
                }
            } else {
            startActivityWithDefaultAnimation(NewOtherUserProfileActivity.getIntent(requireContext(), venueMapInfo.userId ?: 0))
        }
            dismissBottomSheet()
        }.autoDispose()

        binding.btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
            binding.btnFollow.visibility = View.GONE
            binding.btnFollowing.visibility = View.VISIBLE
            callFollowAPI()
        }

        binding.btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
            binding.btnFollow.visibility = View.VISIBLE
            binding.btnFollowing.visibility = View.GONE
            callFollowAPI()
        }

        binding.ivDirection.throttleClicks().subscribeAndObserveOnMainThread {
            openGoogleMapWithProvidedLatLng(requireContext(), venueMapInfo.latitude, venueMapInfo.longitude)
            dismissBottomSheet()
        }.autoDispose()
    }

    private fun callFollowAPI() {
        if (venueMapInfo.followStatus == null) {
            venueMapInfo.followStatus = 1
        } else {
            if (venueMapInfo.followStatus == 1) {
                venueMapInfo.followStatus = 0
            } else {
                venueMapInfo.followStatus = 1
            }
        }
        userInfoViewModel.followUnfollow(venueMapInfo.userId ?: 0)
    }

    fun dismissBottomSheet() {
        dismiss()
    }
}