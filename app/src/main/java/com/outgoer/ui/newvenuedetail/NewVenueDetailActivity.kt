package com.outgoer.ui.newvenuedetail

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.api.profile.model.BlockUserRequest
import com.outgoer.api.profile.model.ReportUserRequest
import com.outgoer.api.reels.model.ReelInfo
import com.outgoer.api.venue.model.CheckInOutRequest
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.ActivityNewVenueDetailBinding
import com.outgoer.ui.createevent.CreateEventsActivity
import com.outgoer.ui.home.home.PostMoreOptionBottomSheet
import com.outgoer.ui.home.newmap.venuemap.bottomsheet.VenueDetailsBottomsheet
import com.outgoer.ui.home.profile.venue_profile.view.VenueTaggedAdapter
import com.outgoer.ui.newvenuedetail.view.VenueDetailTabAdapter
import com.outgoer.ui.newvenuedetail.view.VenueReelsAdapter
import com.outgoer.ui.reelsdetail.ReelsDetailActivity
import com.outgoer.ui.report.ReportBottomSheet
import com.outgoer.ui.tag_venue.VenueTaggedActivity
import com.outgoer.ui.venuedetail.viewmodel.VenueDetailViewModel
import com.outgoer.ui.venuedetail.viewmodel.VenueDetailViewState
import com.outgoer.ui.videorooms.VideoRoomsActivity
import javax.inject.Inject


class NewVenueDetailActivity : BaseActivity() {

    companion object {
        private const val INTENT_EXTRA_CATEGORY_ID = "INTENT_EXTRA_CATEGORY_ID"
        private const val INTENT_EXTRA_VENUE_ID = "INTENT_EXTRA_VENUE_ID"
        fun getIntent(context: Context, categoryId: Int, venueId: Int): Intent {
            val intent = Intent(context, NewVenueDetailActivity::class.java)
            intent.putExtra(INTENT_EXTRA_CATEGORY_ID, categoryId)
            intent.putExtra(INTENT_EXTRA_VENUE_ID, venueId)
            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueDetailViewModel>
    private lateinit var venueDetailViewModel: VenueDetailViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var binding: ActivityNewVenueDetailBinding

    private lateinit var venueDetailTabAdapter: VenueDetailTabAdapter
    private lateinit var venueReelsAdapter: VenueReelsAdapter


    private var categoryId = -1
    private var venueId = -1
    private var venueDetail: VenueDetail? = null

    private lateinit var venueTaggedAdapter: VenueTaggedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewVenueDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        OutgoerApplication.component.inject(this)
        venueDetailViewModel = getViewModelFromFactory(viewModelFactory)

        loadDataFromIntent()
    }

    private fun loadDataFromIntent() {
        intent?.let {
            categoryId = it.getIntExtra(INTENT_EXTRA_CATEGORY_ID, -1)
            venueId = it.getIntExtra(INTENT_EXTRA_VENUE_ID, -1)

            if (categoryId != -1 && venueId != -1) {
                listenToViewModel()
                listenToViewEvents()
            } else {
                onBackPressedDispatcher.onBackPressed()
            }
        } ?: onBackPressedDispatcher.onBackPressed()

        binding.ivMore.isVisible = !(loggedInUserCache.getUserId()?.equals(venueId) ?: false)

        venueTaggedAdapter = VenueTaggedAdapter(this@NewVenueDetailActivity)

        binding.venueTaggedRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NewVenueDetailActivity, RecyclerView.HORIZONTAL, false)
            adapter = venueTaggedAdapter
        }

//        binding.ivEventAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
//            startActivityWithFadeInAnimation(EventListActivity.newInstance(this@NewVenueDetailActivity))
//        }

        binding.ivMore.throttleClicks().subscribeAndObserveOnMainThread {
            val postMoreOptionBottomSheet: PostMoreOptionBottomSheet = PostMoreOptionBottomSheet.newInstanceWithData(true)
            postMoreOptionBottomSheet.postMoreOptionClick.subscribeAndObserveOnMainThread {
                when(it) {
                    is PostMoreOption.BlockClick -> {

                        val blockUserVenueBottomSheet = BlockUserVenueBottomSheet.newInstanceWithData(venueDetail?.avatar ?: "", venueDetail?.name ?: "")
                        blockUserVenueBottomSheet.blockOptionClick.subscribeAndObserveOnMainThread {
                            venueDetailViewModel.blockUserProfile(BlockUserRequest(venueId))
                            blockUserVenueBottomSheet.dismissBottomSheet()
                        }
                        blockUserVenueBottomSheet.show(supportFragmentManager, BlockUserVenueBottomSheet.Companion::class.java.name)
                        postMoreOptionBottomSheet.dismissBottomSheet()
                    }
                    is PostMoreOption.ReportClick -> {
                        val reportOptionBottomSheet = ReportBottomSheet()
                        reportOptionBottomSheet.reasonClick.subscribeAndObserveOnMainThread { reportId ->
                            venueDetailViewModel.reportUserVenue(ReportUserRequest(venueId, reportId))
                            reportOptionBottomSheet.dismiss()

                            postMoreOptionBottomSheet.dismissBottomSheet()
                        }.autoDispose()
                        reportOptionBottomSheet.show(
                            supportFragmentManager, ReportBottomSheet::class.java.name
                        )

                    }
                    is PostMoreOption.DismissClick -> {
                        postMoreOptionBottomSheet.dismissBottomSheet()
                    }
                    is PostMoreOption.DeleteClick -> {}
                }
            }

            postMoreOptionBottomSheet.show(supportFragmentManager, PostMoreOptionBottomSheet.Companion::class.java.name)
        }.autoDispose()

        binding.btnFollow.throttleClicks().subscribeAndObserveOnMainThread {

            venueDetail?.followStatus = if(venueDetail?.followStatus?.equals(0) == true) 1 else 0

            if(venueDetail?.followStatus == 1) {
                binding.btnFollowing.visibility = View.VISIBLE
                binding.btnFollow.visibility = View.GONE
            } else {
                binding.btnFollowing.visibility = View.GONE
                binding.btnFollow.visibility = View.VISIBLE
            }

            venueDetail?.id?.let { it1 -> venueDetailViewModel.followUnfollow(it1) }

        }

        binding.btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {

            venueDetail?.followStatus = if(venueDetail?.followStatus?.equals(0) == true) 1 else 0

            if(venueDetail?.followStatus == 1) {
                binding.btnFollowing.visibility = View.VISIBLE
                binding.btnFollow.visibility = View.GONE
            } else {
                binding.btnFollowing.visibility = View.GONE
                binding.btnFollow.visibility = View.VISIBLE
            }

            venueDetail?.id?.let { it1 -> venueDetailViewModel.followUnfollow(it1) }

        }

        binding.placeVenueFrameLayout.throttleClicks().subscribeAndObserveOnMainThread {
            if ((venueDetail?.isLive ?: 0) > 0) {
                startActivity(
                    VideoRoomsActivity.getIntentLive(
                        this@NewVenueDetailActivity,
                        venueDetail?.liveId
                    )
                )
            } else if ((venueDetail?.postCount ?: 0) > 0 || (venueDetail?.reelCount
                    ?: 0) > 0 || (venueDetail?.spontyCount ?: 0) > 0
            ) {
                startActivityWithDefaultAnimation(
                    VenueTaggedActivity.getIntent(
                        this@NewVenueDetailActivity,
                        venueDetail?.id ?: 0,
                        venueDetail?.reelCount ?: -1,
                        venueDetail?.postCount ?: -1,
                        venueDetail?.spontyCount ?: -1
                    )
                )
            } else {
                val venueDetailsBottomSheet =
                    VenueDetailsBottomsheet.newInstance(venueDetail?.id ?: 0)
                venueDetailsBottomSheet.show(supportFragmentManager, VenueDetailsBottomsheet.TAG)
            }
        }
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
                ResourcesCompat.getColor(resources, R.color.color_FD8AFF, null),// Start color
                ResourcesCompat.getColor(resources, R.color.color_B421FF, null)// End color
            )
        ).apply {
            cornerRadius = resources.getDimension(com.intuit.sdp.R.dimen._200sdp)
        }

        venueDetailViewModel.venueDetailState.subscribeAndObserveOnMainThread {
            when (it) {
                is VenueDetailViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is VenueDetailViewState.LoadVenueDetail -> {

                    if(!it.venueDetail.broadcastMessage.isNullOrEmpty()) {
                        binding.marqueeText.visibility = View.VISIBLE
                        binding.marqueeText.text = it.venueDetail.broadcastMessage
                        binding.marqueeText.isSelected = true
                    }else if ((it.venueDetail.tagCount ?: 0) > 0 && (it.venueDetail.checkInCount ?: 0) > 0) {
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
                    (venueDetail)?.let { it1 -> updateViewWithData(it1) }
                    if(venueDetail?.followStatus == 1) {
                        binding.btnFollowing.visibility = View.VISIBLE
                        binding.btnFollow.visibility = View.GONE
                    } else {
                        binding.btnFollowing.visibility = View.GONE
                        binding.btnFollow.visibility = View.VISIBLE
                    }

//                    binding.likeAppCompatImageView.setImageDrawable(resources.getDrawable(if(venueDetail?.venueFavouriteStatus == 1) R.drawable.ic_like_heart else R.drawable.ic_like, null))

                    if (!it.venueDetail.coverImage.isNullOrEmpty()) {
                        Glide.with(this)
                            .load(it.venueDetail.coverImage)
                            .placeholder(R.drawable.venue_placeholder)
                            .error(R.drawable.venue_placeholder)
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


                    loadVenueDetail(this, it.venueDetail)
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
                        if (MapVenueUserType.VENUE_OWNER.type == loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType) View.VISIBLE else View.GONE

                    binding.ivVerified.isVisible = it.venueDetail.profileVerified == 1
                }
                VenueDetailViewState.SuccessCheckInOut -> {
                    venueDetailViewModel.getVenueDetail(venueId)
                }
                is VenueDetailViewState.SuccessCheckInOutMessage -> {
                    showToast(it.successMessage)
                }
                else -> {}
            }
        }.autoDispose()
    }

    private fun updateViewWithData(venueDetail: VenueDetail) {
        if (venueDetail.isCheckedIn == true) {
            binding.btnCheckIn.visibility = View.GONE
            binding.venueCheckOutLayout.visibility = View.VISIBLE
            if (venueDetail.checkInUser != null) {
                Glide.with(this)
                    .load(venueDetail.checkInUser.avatar)
                    .placeholder(R.drawable.ic_chat_user_placeholder)
                    .error(R.drawable.ic_chat_user_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(binding.ivVenueProfile)
                binding.tvVenue.text = venueDetail.checkInUser.name.toString()
                binding.btnCheckOut.throttleClicks().subscribeAndObserveOnMainThread {
                    if (venueId != -1) {
                        venueDetailViewModel.checkInOutVenue(CheckInOutRequest(venueId, 0))
                    }
                }.autoDispose()
            } else {
                binding.btnCheckIn.visibility = View.VISIBLE
                binding.venueCheckOutLayout.visibility = View.GONE
            }
        } else {
            binding.btnCheckIn.visibility = View.VISIBLE
            binding.venueCheckOutLayout.visibility = View.GONE
            binding.btnCheckIn.throttleClicks().subscribeAndObserveOnMainThread {
                if (venueId != -1) {
                    venueDetailViewModel.checkInOutVenue(CheckInOutRequest(venueId, 1))
                }
            }.autoDispose()
        }
    }

    private fun setVenueReelsData(reels: ArrayList<ReelInfo>?) {
        if (!reels.isNullOrEmpty()) {
            venueReelsAdapter.listofAvailable = reels
            binding.recentReelsAppCompatTextView.isVisible = true
            binding.rvVenueReels.isVisible = true
        } else {
            binding.recentReelsAppCompatTextView.isVisible = false
            binding.rvVenueReels.isVisible = false
        }
    }

    private fun listenToViewEvents() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }

//        binding.likeAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
//
//            venueDetail?.venueFavouriteStatus = if((venueDetail?.venueFavouriteStatus?: 0).equals(1)) 0 else 1
//            binding.likeAppCompatImageView.setImageDrawable(resources.getDrawable(if(venueDetail?.venueFavouriteStatus == 1) R.drawable.ic_like_heart else R.drawable.ic_like, null))
//            venueDetail?.id?.let { it1 -> venueDetailViewModel.addRemoveFavouriteVenue(it1) }
//        }

        binding.fabStart.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(CreateEventsActivity.getIntent(this, venueDetail))
        }.autoDispose()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })

        venueReelsAdapter = VenueReelsAdapter(this).apply {
            venueDetailReelsViewClick.subscribeAndObserveOnMainThread {
                startActivityWithDefaultAnimation(
                    ReelsDetailActivity.getIntent(
                        this@NewVenueDetailActivity,
                        it.id
                    )
                )
            }.autoDispose()
        }
        binding.rvVenueReels.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        binding.rvVenueReels.adapter = venueReelsAdapter


    }

    private fun loadVenueDetail(context: Context, venueDetail: VenueDetail) {

        binding.apply {

            venueDetailTabAdapter = VenueDetailTabAdapter(
                this@NewVenueDetailActivity, venueDetail
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

            binding.venueRatingBar.rating = venueDetail.reviewAvg?.toFloat()!!
            binding.tvVenueRatingCount.text = venueDetail.reviewAvg.toString()
            binding.tvTotalReview.text = "(".plus(venueDetail.totalReview.toString()).plus(")")

            binding.tvVenueName.text = venueDetail.name
            binding.tvVenueLocation.text = venueDetail.description

            Glide.with(context).load(venueDetail.avatar ?: "").placeholder(R.drawable.ic_chat_user_placeholder)
                .error(R.drawable.ic_chat_user_placeholder).into(binding.ivVenueImage)
        }
    }

    override fun onResume() {
        super.onResume()
        if (categoryId != -1 && venueId != -1) {
            venueDetailViewModel.getVenueDetail(venueId)
            venueDetailViewModel.getOtherNearVenue(categoryId, venueId)
        }
    }
}