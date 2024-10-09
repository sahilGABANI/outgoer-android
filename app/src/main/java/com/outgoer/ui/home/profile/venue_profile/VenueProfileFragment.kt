package com.outgoer.ui.home.profile.venue_profile

import android.app.Activity
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayoutMediator
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.RegisterVenueRequest
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.FragmentVenueProfileBinding
import com.outgoer.ui.createevent.AddMediaEventActivity
import com.outgoer.ui.createevent.CreateEventsActivity
import com.outgoer.ui.home.HomeActivity
import com.outgoer.ui.home.profile.newprofile.SwitchAccountBottomSheet
import com.outgoer.ui.home.profile.newprofile.setting.NewProfileSettingActivity
import com.outgoer.ui.home.profile.venue_profile.view.VenueTaggedAdapter
import com.outgoer.ui.home.profile.viewmodel.ProfileViewModel
import com.outgoer.ui.newvenuedetail.view.VenueDetailTabAdapter
import com.outgoer.ui.newvenuedetail.view.VenueReelsAdapter
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import com.outgoer.ui.venue.update.VenueInfoUpdateActivity
import com.outgoer.ui.venue.update.VenueUpdateActivity
import com.outgoer.utils.FileUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates

class VenueProfileFragment : BaseFragment() {

    companion object {
        private const val MEDIA_TYPE_IMAGE = "POST_TYPE_IMAGE"

        @JvmStatic
        fun newInstance() = VenueProfileFragment()
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<ProfileViewModel>
    private lateinit var profileViewModel: ProfileViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private lateinit var outgoerUser: OutgoerUser
    private var loggedInUserId by Delegates.notNull<Int>()

    private var _binding: FragmentVenueProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var venueDetailTabAdapter: VenueDetailTabAdapter

    private var avatar = ""
    private lateinit var venueReelsAdapter: VenueReelsAdapter
    private lateinit var venueTaggedAdapter: VenueTaggedAdapter
    private var venueDetail: VenueDetail? = null
    var isReload: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVenueProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        OutgoerApplication.component.inject(this)
        profileViewModel = getViewModelFromFactory(viewModelFactory)

        outgoerUser = loggedInUserCache.getLoggedInUser()?.loggedInUser ?: return
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        profileViewModel.getCloudFlareConfig()
        profileViewModel.getVenueDetail(loggedInUserId)

        initUI()
        listenToViewModel()

        RxBus.listen(RxEvent.DataReload::class.java).subscribeOnIoAndObserveOnMainThread({
            if (it.selectedTab == "VenueProfileFragmentTag") {
                lifecycleScope.launch {
                    delay(1000)
                    profileViewModel.myProfile()
                    profileViewModel.getVenueDetail(loggedInUserId)
                }
                binding.dataNestedScrollView.smoothScrollTo(0, 0)
            }
        }, {
            Timber.e(it)
        }).autoDispose()
    }

    override fun onResume() {
        super.onResume()

        if(isResumed) {
            profileViewModel.myProfile()
            profileViewModel.getVenueDetail(loggedInUserId)
        }
    }

    private fun initUI() {

        binding.downAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            var switchAccountBottomSheet: SwitchAccountBottomSheet = SwitchAccountBottomSheet.newInstance()
            switchAccountBottomSheet.switchAccount.subscribeAndObserveOnMainThread {
                startActivityWithFadeInAnimation(HomeActivity.getIntent(requireContext()))
            }
            switchAccountBottomSheet.show(childFragmentManager, SwitchAccountBottomSheet.javaClass.name)
        }.autoDispose()

        binding.ivSetting.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(NewProfileSettingActivity.getIntent(requireContext()))
        }

        binding.editAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val registerVenueRequest = RegisterVenueRequest(
                venueDetail?.name,
                venueDetail?.username,
                venueDetail?.email,
                venueDetail?.phone,
                null,
                venueDetail?.description,
                venueDetail?.venueAddress,
                venueDetail?.latitude,
                venueDetail?.longitude,
                venueDetail?.avatar,
                outgoerUser.venueCategories,
                outgoerUser.availibility ?: arrayListOf(),
                outgoerUser.gallery ?: arrayListOf(),
                venueTags = venueDetail?.venueTags,
                phoneCode = outgoerUser.phoneCode
            )

            startActivity(VenueUpdateActivity.getIntent(requireContext(), registerVenueRequest = registerVenueRequest))
        }

        binding.addressEditAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            val registerVenueRequest = RegisterVenueRequest(
                outgoerUser.name,
                outgoerUser.username,
                outgoerUser.email,
                outgoerUser.phone,
                null,
                outgoerUser.description,
                outgoerUser.venueAddress,
                outgoerUser.latitude,
                outgoerUser.longitude,
                outgoerUser.avatar,
                outgoerUser.venueCategories,
                outgoerUser.availibility ?: arrayListOf(),
                outgoerUser.gallery ?: arrayListOf(),
                phoneCode = outgoerUser.phoneCode
            )
            startActivity(VenueInfoUpdateActivity.getIntent(requireContext(), registerVenueRequest = registerVenueRequest))
        }

        venueReelsAdapter = VenueReelsAdapter(requireContext()).apply {
            venueDetailReelsViewClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(
                    ReelsDetailActivity.getIntent(
                        requireContext(), it.id
                    )
                )
            }.autoDispose()
        }

        binding.rvVenueReels.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding.rvVenueReels.adapter = venueReelsAdapter

        venueTaggedAdapter = VenueTaggedAdapter(requireContext())

        binding.venueTaggedRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
            adapter = venueTaggedAdapter
        }
    }

    private fun setVenueReelsData(reels: ArrayList<ReelInfo>?) {
        if (!reels.isNullOrEmpty()) {
            venueReelsAdapter.listofAvailable = reels
            binding.rvVenueReels.isVisible = true
        } else {
            binding.rvVenueReels.isVisible = false
        }
    }

    @Suppress("SameParameterValue")
    private fun checkPermission(type: String) {
        XXPermissions.with(this).permission(
                listOf(
                    Permission.CAMERA, Permission.RECORD_AUDIO, Permission.READ_MEDIA_IMAGES, Permission.READ_MEDIA_VIDEO
                )
            ).request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    if (all) {
                        @Suppress("DEPRECATION")
                        startActivityForResult(
                            AddMediaEventActivity.getIntentWithData(requireContext(), type), FileUtils.PICK_IMAGE_1
                        )
                    } else {
                        showToast(getString(R.string.msg_some_permission_denied))
                    }
                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)
                    showToast(getString(R.string.msg_permission_denied))
                }
            })
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == FileUtils.PICK_IMAGE_1) && (resultCode == Activity.RESULT_OK)) {
            val image = data?.getStringArrayListExtra("MEDIA_URL")
            avatar = image?.firstOrNull() ?: ""
            io.reactivex.Observable.timer(1000, TimeUnit.MILLISECONDS).subscribeAndObserveOnMainThread {
                profileViewModel.updateVenue(RegisterVenueRequest(coverImage = avatar))
            }.autoDispose()
        }
    }

    private fun listenToViewEvents() {
        binding.fabStart.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithFadeInAnimation(
                CreateEventsActivity.getIntent(requireContext(),
                    VenueDetail(
                        outgoerUser.id,
                        name = outgoerUser.name,
                        venueAddress = outgoerUser.venueAddress,
                        distance = 0.0,
                        reviewAvg = outgoerUser.reviewAvg,
                        latitude = outgoerUser.latitude,
                        longitude = outgoerUser.longitude,
                        avatar = outgoerUser.avatar
                    )
                )
            )
        }
        binding.ivCamera.throttleClicks().subscribeAndObserveOnMainThread {
            checkPermission(MEDIA_TYPE_IMAGE)
        }.autoDispose()
    }

    private fun listenToViewModel() {

/*        val gradientDrawableBlue = GradientDrawable(
            GradientDrawable.Orientation.BL_TR, // 135 degrees (bottom-left to top-right)
            intArrayOf(
                resources.getColor(R.color.color_76c1ed),
                resources.getColor(R.color.color_4152c1),
                resources.getColor(R.color.color_4152c1)
            )
        ).apply {
            cornerRadius = resources.getDimension(com.intuit.sdp.R.dimen._200sdp)
        }*/

        val gradientDrawablePurple = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, // Left to Right gradient
            intArrayOf(
                resources.getColor(R.color.color_FD8AFF), // Start color
                resources.getColor(R.color.color_B421FF)  // End color
            )
        ).apply {
            cornerRadius = resources.getDimension(com.intuit.sdp.R.dimen._200sdp)
        }

        profileViewModel.profileViewStates.subscribeAndObserveOnMainThread {
            when (it) {
                is ProfileViewModel.ProfileViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is ProfileViewModel.ProfileViewState.LoadingState -> {
                    //buttonVisibility(it.isLoading)
                }
                is ProfileViewModel.ProfileViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is ProfileViewModel.ProfileViewState.GetMyReelInfo -> {
                }
                is ProfileViewModel.ProfileViewState.CloudFlareConfigErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is ProfileViewModel.ProfileViewState.UploadMediaCloudFlareSuccess -> {
                    avatar = it.mediaUrl
                    profileViewModel.updateVenue(registerVenueRequest = RegisterVenueRequest(avatar))
                }
                is ProfileViewModel.ProfileViewState.UpdateVenueSuccess -> {
                    Glide.with(requireContext()).load(it.createVenueResponse.avatar ?: "").placeholder(R.drawable.venue_placeholder).error(R.drawable.venue_placeholder)
                        .into(binding.ivVenueImage)


                    Glide.with(requireContext()).load(it.createVenueResponse.coverImage ?: "").placeholder(R.drawable.venue_placeholder)
                        .error(R.drawable.venue_placeholder).into(binding.imageSlider)

                }
                is ProfileViewModel.ProfileViewState.MyProfileData -> {

                    outgoerUser = it.outgoerUser
                    listenToViewEvents()

//                    loadVenueDetail(it.outgoerUser)
//                    setVenueReelsData(it.outgoerUser.reels)

                    it.outgoerUser.venueTags?.let { data ->
                        venueTaggedAdapter.listOfVenueTag = data.split(",")
                    }

                    Glide.with(requireContext())
                        .load(it.outgoerUser.coverImage ?: "")
                        .placeholder(R.drawable.venue_placeholder)
                        .error(R.drawable.venue_placeholder)
                        .into(binding.imageSlider)
                }
                is ProfileViewModel.ProfileViewState.LoadVenueDetail -> {

                    if(!it.venueDetail.broadcastMessage.isNullOrEmpty()) {
                        binding.marqueeText.visibility = View.VISIBLE
                        binding.marqueeText.text = it.venueDetail.broadcastMessage
                        binding.marqueeText.isSelected = true
                    } else if ((it.venueDetail.tagCount ?: 0) > 0 && (it.venueDetail.checkInCount ?: 0) > 0) {
                        binding.marqueeText.visibility = View.VISIBLE
                        binding.marqueeText.text = resources.getString(
                            R.string.marquee_message,
                            it.venueDetail.checkInCount ?: 0,
                            it.venueDetail.tagCount ?: 0
                        )
                        binding.marqueeText.isSelected = true
                    } else if ((it.venueDetail.tagCount ?: 0) > 0) {
                        binding.marqueeText.visibility = View.VISIBLE
                        binding.marqueeText.text = resources.getString(
                            R.string.marquee_message_taggedCount,
                            it.venueDetail.tagCount ?: 0
                        )
                        binding.marqueeText.isSelected = true
                    } else if ((it.venueDetail.checkInCount ?: 0) > 0) {
                        binding.marqueeText.visibility = View.VISIBLE
                        binding.marqueeText.text = resources.getString(
                            R.string.marquee_message_checkedInCount,
                            it.venueDetail.checkInCount ?: 0
                        )
                        binding.marqueeText.isSelected = true
                    } else {
                        binding.marqueeText.visibility = View.GONE
                    }

                    venueDetail = it.venueDetail

                    if (!it.venueDetail.coverImage.isNullOrEmpty()) {
                        Glide.with(this).load(it.venueDetail.coverImage)
                            .placeholder(R.drawable.venue_placeholder).error(R.drawable.venue_placeholder)
                            .into(binding.imageSlider)
                    } else {
                        Glide.with(this)
                            .load(R.drawable.venue_placeholder)
                            .placeholder(R.drawable.venue_placeholder)
                            .error(R.drawable.venue_placeholder)
                            .into(binding.imageSlider)
                    }

                    venueDetail?.venueTags?.let { data ->
                        venueTaggedAdapter.listOfVenueTag = data.split(",")
                    }


                    loadVenueDetail(it.venueDetail)
                    setVenueReelsData(it.venueDetail.reels)
                    val storyCount = (it.venueDetail.storyCount ?: 0) > 0
                    binding.ivVenueImage.background = when {
                        storyCount -> gradientDrawablePurple
                        else -> null
                    }
/*                    if ((it.venueDetail.isLive ?: 0) > 0) {
                        binding.ivVenueImage.background = gradientDrawablePurple
                        binding.liveAppCompatTextView.visibility = View.VISIBLE
                    } else {
                        if ((it.venueDetail.reelCount ?: 0) > 0) {
                            binding.ivVenueImage.background = gradientDrawablePurple
                            binding.liveAppCompatTextView.visibility = View.GONE
                        } else if ((it.venueDetail.postCount ?: 0) > 0) {
                            binding.ivVenueImage.background = gradientDrawablePurple
                            binding.liveAppCompatTextView.visibility = View.GONE
                        } else if ((it.venueDetail.spontyCount ?: 0) > 0) {
                            binding.ivVenueImage.background = gradientDrawableBlue
                            binding.liveAppCompatTextView.visibility = View.GONE
                        } else {
                            binding.ivVenueImage.background = null
                            binding.liveAppCompatTextView.visibility = View.GONE
                        }
                    }*/
                    if (it.equals(null)) {
                        binding.dataNestedScrollView.visibility = View.GONE
//                        binding.venueDataProgressBar.visibility = View.VISIBLE
                    } else {
                        binding.dataNestedScrollView.visibility = View.VISIBLE
//                        binding.venueDataProgressBar.visibility = View.GONE
                    }
                    binding.fabStart.visibility =
                        if (MapVenueUserType.VENUE_OWNER.type.equals(loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType)) View.VISIBLE else View.GONE

                    binding.ivVerified.isVisible = it.venueDetail.profileVerified == 1
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun loadVenueDetail(venueDetail: VenueDetail) {

        val index = binding.tabLayout.selectedTabPosition

        if(!venueDetail.broadcastMessage.isNullOrEmpty()) {
            binding.marqueeText.visibility = View.VISIBLE
            binding.marqueeText.text = venueDetail.broadcastMessage
            binding.marqueeText.isSelected = true
        } else if ((venueDetail.tagCount ?: 0) > 0 && (venueDetail.checkInCount ?: 0) > 0) {
            binding.marqueeText.visibility = View.VISIBLE
            binding.marqueeText.text = resources.getString(
                R.string.marquee_message,
                venueDetail.checkInCount ?: 0,
                venueDetail.tagCount ?: 0
            )
            binding.marqueeText.isSelected = true

        } else if ((venueDetail.tagCount ?: 0) > 0) {
            binding.marqueeText.visibility = View.VISIBLE
            binding.marqueeText.text = resources.getString(
                R.string.marquee_message_taggedCount,
                venueDetail.tagCount ?: 0
            )
            binding.marqueeText.isSelected = true

        } else if ((venueDetail.checkInCount ?: 0) > 0) {
            binding.marqueeText.visibility = View.VISIBLE
            binding.marqueeText.text = resources.getString(
                R.string.marquee_message_checkedInCount,
                venueDetail.checkInCount ?: 0
            )
            binding.marqueeText.isSelected = true
        } else {
            binding.marqueeText.visibility = View.GONE
        }

        venueDetailTabAdapter = VenueDetailTabAdapter(
            requireActivity(), VenueDetail(
                venueDetail.id,
                venueDetail.name,
                venueDetail.username,
                venueDetail.userType,
                venueDetail.email,
                venueDetail.phone,
                venueDetail.emailVerified,
                venueDetail.avatar,
                venueDetail.about,
                "0",
                venueDetail.venueAddress,
                venueDetail.latitude,
                venueDetail.longitude,
                null,
                gallery = arrayListOf(),
                availibility = venueDetail.availibility,
                description = venueDetail.description,
                phoneCode = venueDetail.phoneCode
            )
        )
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 5
        binding.viewPager.adapter = venueDetailTabAdapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.label_about)
                }
                1 -> {
                    tab.text = getString(R.string.label_review)
                }
                2 -> {
                    tab.text = getString(R.string.media)
                }
                3 -> {
                    tab.text = getString(R.string.label_post)
                }
                4 -> {
                    tab.text = getString(R.string.label_events)
                }
            }
        }.attach()

        binding.tabLayout.getTabAt(index)?.select()

        if (venueDetail.reviewAvg != null) {
            binding.ratingLinearLayout.visibility = View.VISIBLE
            binding.venueRatingBar.rating = venueDetail?.reviewAvg?.toFloat() ?: 0.0f
            binding.tvVenueRatingCount.text = venueDetail.reviewAvg.toString()
            binding.tvTotalReview.text = "(".plus(venueDetail.totalReview.toString()).plus(")")
        } else {
            binding.ratingLinearLayout.visibility = View.GONE
        }

        binding.tvVenueName.text = venueDetail.name
        binding.tvVenueLocation.text = venueDetail.description

        Glide.with(requireContext())
            .load(venueDetail.avatar ?: "")
            .placeholder(R.drawable.ic_chat_user_placeholder)
            .error(R.drawable.ic_chat_user_placeholder)
            .into(binding.ivVenueImage)
    }

}