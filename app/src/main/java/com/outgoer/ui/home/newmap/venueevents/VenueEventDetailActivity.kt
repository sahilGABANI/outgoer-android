package com.outgoer.ui.home.newmap.venueevents

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.event.model.EventData
import com.outgoer.api.event.model.JoinRequest
import com.outgoer.api.post.model.PostMoreOption
import com.outgoer.api.profile.model.BlockUserRequest
import com.outgoer.api.profile.model.ReportEventRequest
import com.outgoer.api.profile.model.ReportUserRequest
import com.outgoer.api.slider.SliderItem
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.getViewModelFromFactory
import com.outgoer.base.extension.showToast
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityVenueEventDetailBinding
import com.outgoer.mediapicker.utils.DateUtils.getVideoDurationInHourMinFormat
import com.outgoer.mediapicker.utils.DateUtils.getVideoDurationInHourMinSecFormat
import com.outgoer.ui.home.home.PostMoreOptionBottomSheet
import com.outgoer.ui.home.newReels.view.NewReelsHashtagAdapter
import com.outgoer.ui.home.newmap.venueevents.view.AdminEventTabAdapter
import com.outgoer.ui.home.newmap.venueevents.view.EventCategoryAdapter
import com.outgoer.ui.home.newmap.venueevents.view.EventTabAdapter
import com.outgoer.ui.home.newmap.venueevents.view.SliderAdapter
import com.outgoer.ui.home.newmap.venueevents.viewmodel.EventViewState
import com.outgoer.ui.home.newmap.venueevents.viewmodel.VenueEventViewModel
import com.outgoer.ui.newvenuedetail.BlockUserVenueBottomSheet
import com.outgoer.ui.report.ReportBottomSheet
import com.smarteist.autoimageslider.IndicatorView.animation.type.IndicatorAnimationType
import com.smarteist.autoimageslider.SliderAnimations
import com.smarteist.autoimageslider.SliderView
import javax.inject.Inject


class VenueEventDetailActivity : BaseActivity() {

    companion object {
        const val EVENT_INFO = "EVENT_INFO"
        const val EVENT_ID = "EVENT_ID"

        fun getIntent(context: Context, venueEventInfo: EventData): Intent {
            val intent = Intent(context, VenueEventDetailActivity::class.java)
            intent.putExtra(EVENT_INFO, venueEventInfo)

            return intent
        }

        fun getIntentWithId(context: Context, eventId: Int): Intent {
            val intent = Intent(context, VenueEventDetailActivity::class.java)
            intent.putExtra(EVENT_ID, eventId)

            return intent
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<VenueEventViewModel>
    private lateinit var venueEventViewModel: VenueEventViewModel

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var binding: ActivityVenueEventDetailBinding
    private lateinit var sliderAdapter: SliderAdapter
    private lateinit var newReelsHashtagAdapter: NewReelsHashtagAdapter
    private lateinit var eventAdminTabAdapter: AdminEventTabAdapter
    private lateinit var eventTabAdapter: EventTabAdapter
    private lateinit var eventCategoryAdapter: EventCategoryAdapter
    private var eventData: EventData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)

        binding = ActivityVenueEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        venueEventViewModel = getViewModelFromFactory(viewModelFactory)
        initSlideShow()
        initUI()
        listenToViewModel()
    }

    private fun listenToViewModel() {
        venueEventViewModel.eventsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is EventViewState.LoadingState -> {}
                is EventViewState.ErrorMessage -> {
                    showToast(it.errorMessage)
                }
                is EventViewState.SuccessMessage -> {
                    showToast(it.successMessage)
                }
                is EventViewState.EventDetails -> {
                    eventData = it.listofevent
                    eventData?.let {

                        it.category?.let { category ->
                            eventCategoryAdapter.listOfDataItems = arrayListOf(category)
                        }
                        binding.nameAppCompatTextView.text = it.name ?: ""
                        binding.enameAppCompatTextView.text = it.name ?: ""
                        binding.dateAppCompatTextView.text =
                            getVideoDurationInHourMinSecFormat(it.dateTime ?: "")
                        binding.eventEndDateAppCompatTextView.text =
                            getVideoDurationInHourMinSecFormat(it.endDateTime ?: "")
                        binding.timeAppCompatTextView.text =
                            getVideoDurationInHourMinFormat(it.dateTime ?: "")

                        binding.ivMore.isVisible = !(it.userId.equals(loggedInUserCache.getUserId()))

                        binding.joinRequestBannerAppCompatTextView.visibility = if (eventData?.joinRequestStatus ?: false && eventData?.eventRequest?.status == 1) View.VISIBLE else View.GONE

                        if (eventData?.joinRequestStatus ?: false) {
                            binding.joinEventMaterialButton.isEnabled = false
                            binding.joinEventMaterialButton.text =
                                resources.getString(R.string.label_requested)
                        } else {
                            binding.joinEventMaterialButton.isEnabled = true
                            binding.joinEventMaterialButton.text =
                                resources.getString(R.string.label_join_Request)
                        }

                        val imageList = arrayListOf(SliderItem("", it.firstMedia?.image ?: ""))
                        if(imageList.size > 1){
                            binding.imageSlider.visibility = View.VISIBLE
                            binding.anImageNoSlider.visibility = View.GONE
                        } else {
                            binding.anImageNoSlider.visibility = View.VISIBLE
                            binding.imageSlider.visibility = View.GONE
                            Glide.with(this)
                                .load(it.firstMedia?.image)
                                .into(binding.anImageNoSlider)
                        }
                        sliderAdapter.renewItems(imageList)

                        if ((loggedInUserCache.getUserId() ?: 0).equals(it.user?.id ?: 0)) {
                            eventData?.let {
                                eventAdminTabAdapter = AdminEventTabAdapter(this, it)
                                binding.viewPagerInfo.offscreenPageLimit = 5
                                binding.viewPagerInfo.adapter = eventAdminTabAdapter
                                binding.buttonLinearLayout.visibility = View.GONE
                            }
                        } else {
                            eventData?.let {
                                eventTabAdapter = EventTabAdapter(this, it)
                                binding.viewPagerInfo.offscreenPageLimit = 4
                                binding.viewPagerInfo.adapter = eventTabAdapter
                                binding.buttonLinearLayout.visibility = View.VISIBLE
                            }
                        }

                        initTabNext()

                        val icPlaceHolderProfile = ContextCompat.getDrawable(this, R.drawable.ic_chat_user_placeholder)
                        when (eventData?.mutal?.size ?: 0) {
                            0 -> {
                                binding.checkJoinMaterialButton.visibility = View.GONE
                            }
                            1 -> {
                                binding.checkJoinMaterialButton.visibility = View.VISIBLE
                                binding.firstRoundedImageView.visibility = View.VISIBLE
                                binding.secondRoundedImageView.visibility = View.GONE
                                binding.thirdRoundedImageView.visibility = View.GONE
                                binding.moreFrameLayout.visibility = View.GONE

                                Glide.with(this)
                                    .load(eventData?.mutal?.get(0)?.avatar)
                                    .placeholder(icPlaceHolderProfile)
                                    .centerCrop()
                                    .into(binding.firstRoundedImageView)
                            }
                            2 -> {
                                binding.checkJoinMaterialButton.visibility = View.VISIBLE
                                binding.firstRoundedImageView.visibility = View.VISIBLE
                                binding.secondRoundedImageView.visibility = View.VISIBLE
                                binding.thirdRoundedImageView.visibility = View.GONE
                                binding.moreFrameLayout.visibility = View.GONE
                                Glide.with(this)
                                    .load(eventData?.mutal?.get(0)?.avatar)
                                    .placeholder(icPlaceHolderProfile)
                                    .centerCrop()
                                    .into(binding.firstRoundedImageView)
                                Glide.with(this@VenueEventDetailActivity)
                                    .load(eventData?.mutal?.get(1)?.avatar)
                                    .placeholder(icPlaceHolderProfile)
                                    .centerCrop()
                                    .into(binding.secondRoundedImageView)
                            }
                            3 -> {
                                binding.checkJoinMaterialButton.visibility = View.VISIBLE
                                binding.firstRoundedImageView.visibility = View.VISIBLE
                                binding.secondRoundedImageView.visibility = View.VISIBLE
                                binding.thirdRoundedImageView.visibility = View.VISIBLE
                                binding.moreFrameLayout.visibility = View.GONE
                                Glide.with(this)
                                    .load(eventData?.mutal?.get(0)?.avatar)
                                    .placeholder(icPlaceHolderProfile)
                                    .centerCrop()
                                    .into(binding.firstRoundedImageView)
                                Glide.with(this)
                                    .load(eventData?.mutal?.get(1)?.avatar)
                                    .placeholder(icPlaceHolderProfile)
                                    .centerCrop()
                                    .into(binding.secondRoundedImageView)
                                Glide.with(this)
                                    .load(eventData?.mutal?.get(2)?.avatar)
                                    .placeholder(icPlaceHolderProfile)
                                    .centerCrop()
                                    .into(binding.thirdRoundedImageView)
                            }
                            else -> {
                                binding.checkJoinMaterialButton.visibility = View.VISIBLE
                                binding.firstRoundedImageView.visibility = View.VISIBLE
                                binding.secondRoundedImageView.visibility = View.VISIBLE
                                binding.moreFrameLayout.visibility = View.VISIBLE
                                binding.thirdRoundedImageView.visibility = View.VISIBLE
                                binding.maxRoundedImageView.visibility = View.VISIBLE
                                binding.maxRoundedImageView.text = eventData?.otherMutualFriend.toString().plus("+")

                                Glide.with(this)
                                    .load(eventData?.mutal?.get(0)?.avatar)
                                    .placeholder(icPlaceHolderProfile)
                                    .centerCrop()
                                    .into(binding.firstRoundedImageView)
                                Glide.with(this)
                                    .load(eventData?.mutal?.get(1)?.avatar)
                                    .placeholder(icPlaceHolderProfile)
                                    .centerCrop()
                                    .into(binding.secondRoundedImageView)
                                Glide.with(this)
                                    .load(eventData?.mutal?.get(2)?.avatar)
                                    .placeholder(icPlaceHolderProfile)
                                    .centerCrop()
                                    .into(binding.thirdRoundedImageView)

                                binding.maxRoundedImageView.text = eventData?.otherMutualFriend.toString().plus("+")

                            }
                        }
                    }
                }
                is EventViewState.AddRemoveEventDetails -> {
                }
                else -> {}
            }
        }
    }

    private fun initSlideShow() {
        sliderAdapter = SliderAdapter(this)
        binding.imageSlider.setSliderAdapter(sliderAdapter)
        binding.imageSlider.setIndicatorAnimation(IndicatorAnimationType.WORM) //set indicator animation by using SliderLayout.IndicatorAnimations. :WORM or THIN_WORM or COLOR or DROP or FILL or NONE or SCALE or SCALE_DOWN or SLIDE and SWAP!!

        binding.imageSlider.setSliderTransformAnimation(SliderAnimations.SIMPLETRANSFORMATION)
        binding.imageSlider.autoCycleDirection = SliderView.AUTO_CYCLE_DIRECTION_BACK_AND_FORTH
        binding.imageSlider.indicatorSelectedColor = Color.WHITE
        binding.imageSlider.indicatorUnselectedColor = Color.GRAY
        binding.imageSlider.scrollTimeInSec = 3
        binding.imageSlider.isAutoCycle = true
    }

    private fun initUI() {

        binding.ivMore.throttleClicks().subscribeAndObserveOnMainThread {
            var postMoreOptionBottomSheet: PostMoreOptionBottomSheet = PostMoreOptionBottomSheet.newInstanceWithData(true, true)
            postMoreOptionBottomSheet.postMoreOptionClick.subscribeAndObserveOnMainThread {
                when(it) {
                    is PostMoreOption.ReportClick -> {
                        val reportOptionBottomSheet = ReportBottomSheet()
                        reportOptionBottomSheet.reasonClick.subscribeAndObserveOnMainThread { reportId ->
                            venueEventViewModel.reportEvent(ReportEventRequest(eventId = eventData?.id ?: 0, reportId))
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
                    else -> {}
                }
            }

            postMoreOptionBottomSheet.show(supportFragmentManager, PostMoreOptionBottomSheet.javaClass.name)

        }

        eventCategoryAdapter = EventCategoryAdapter(this@VenueEventDetailActivity).apply {  }

        binding.taggedUesrsRecyclerView.apply {
            adapter = eventCategoryAdapter
        }

        intent?.let {
            if(it.hasExtra(EVENT_INFO)) {
                eventData = it.getParcelableExtra(EVENT_INFO)

                eventData?.let {

                    it.category?.let { category ->
                        eventCategoryAdapter.listOfDataItems = arrayListOf(category)
                    }
                    binding.nameAppCompatTextView.text = it.name ?: ""
                    binding.enameAppCompatTextView.text = it.name ?: ""
                    binding.dateAppCompatTextView.text =
                        getVideoDurationInHourMinSecFormat(it.dateTime ?: "")
                    binding.eventEndDateAppCompatTextView.text =
                        getVideoDurationInHourMinSecFormat(it.endDateTime ?: "")
                    binding.timeAppCompatTextView.text =
                        getVideoDurationInHourMinFormat(it.dateTime ?: "")

                    binding.ivMore.isVisible = !(it.userId.equals(loggedInUserCache.getUserId()))

                    binding.joinRequestBannerAppCompatTextView.visibility = if (eventData?.joinRequestStatus ?: false && eventData?.eventRequest?.status == 1) View.VISIBLE else View.GONE

                    if (eventData?.joinRequestStatus ?: false) {
                        binding.joinEventMaterialButton.isEnabled = false
                        binding.joinEventMaterialButton.text =
                            resources.getString(R.string.label_requested)
                    } else {
                        binding.joinEventMaterialButton.isEnabled = true
                        binding.joinEventMaterialButton.text =
                            resources.getString(R.string.label_join_Request)
                    }

                    val imageList = arrayListOf(SliderItem("", it.firstMedia?.image ?: ""))
                    if(imageList.size > 1){
                        binding.imageSlider.visibility = View.VISIBLE
                        binding.anImageNoSlider.visibility = View.GONE
                    } else {
                        binding.anImageNoSlider.visibility = View.VISIBLE
                        binding.imageSlider.visibility = View.GONE
                        Glide.with(this)
                            .load(it.firstMedia?.image)
                            .into(binding.anImageNoSlider)
                    }
                    sliderAdapter.renewItems(imageList)

                    if ((loggedInUserCache.getUserId() ?: 0).equals(it.user?.id ?: 0)) {
                        eventData?.let {
                            eventAdminTabAdapter = AdminEventTabAdapter(this, it)
                            binding.viewPagerInfo.offscreenPageLimit = 5
                            binding.viewPagerInfo.adapter = eventAdminTabAdapter
                            binding.buttonLinearLayout.visibility = View.GONE
                        }
                    } else {
                        eventData?.let {
                            eventTabAdapter = EventTabAdapter(this, it)
                            binding.viewPagerInfo.offscreenPageLimit = 4
                            binding.viewPagerInfo.adapter = eventTabAdapter
                            binding.buttonLinearLayout.visibility = View.VISIBLE
                        }
                    }

                    val icPlaceHolderProfile = ContextCompat.getDrawable(this, R.drawable.ic_chat_user_placeholder)
                    when (eventData?.mutal?.size ?: 0) {
                        0 -> {
                            binding.checkJoinMaterialButton.visibility = View.GONE
                        }
                        1 -> {
                            binding.checkJoinMaterialButton.visibility = View.VISIBLE
                            binding.firstRoundedImageView.visibility = View.VISIBLE
                            binding.secondRoundedImageView.visibility = View.GONE
                            binding.thirdRoundedImageView.visibility = View.GONE
                            binding.moreFrameLayout.visibility = View.GONE

                            Glide.with(this)
                                .load(eventData?.mutal?.get(0)?.avatar)
                                .placeholder(icPlaceHolderProfile)
                                .centerCrop()
                                .into(binding.firstRoundedImageView)
                        }
                        2 -> {
                            binding.checkJoinMaterialButton.visibility = View.VISIBLE
                            binding.firstRoundedImageView.visibility = View.VISIBLE
                            binding.secondRoundedImageView.visibility = View.VISIBLE
                            binding.thirdRoundedImageView.visibility = View.GONE
                            binding.moreFrameLayout.visibility = View.GONE
                            Glide.with(this)
                                .load(eventData?.mutal?.get(0)?.avatar)
                                .placeholder(icPlaceHolderProfile)
                                .centerCrop()
                                .into(binding.firstRoundedImageView)
                            Glide.with(this@VenueEventDetailActivity)
                                .load(eventData?.mutal?.get(1)?.avatar)
                                .placeholder(icPlaceHolderProfile)
                                .centerCrop()
                                .into(binding.secondRoundedImageView)
                        }
                        3 -> {
                            binding.checkJoinMaterialButton.visibility = View.VISIBLE
                            binding.firstRoundedImageView.visibility = View.VISIBLE
                            binding.secondRoundedImageView.visibility = View.VISIBLE
                            binding.thirdRoundedImageView.visibility = View.VISIBLE
                            binding.moreFrameLayout.visibility = View.GONE
                            Glide.with(this)
                                .load(eventData?.mutal?.get(0)?.avatar)
                                .placeholder(icPlaceHolderProfile)
                                .centerCrop()
                                .into(binding.firstRoundedImageView)
                            Glide.with(this)
                                .load(eventData?.mutal?.get(1)?.avatar)
                                .placeholder(icPlaceHolderProfile)
                                .centerCrop()
                                .into(binding.secondRoundedImageView)
                            Glide.with(this)
                                .load(eventData?.mutal?.get(2)?.avatar)
                                .placeholder(icPlaceHolderProfile)
                                .centerCrop()
                                .into(binding.thirdRoundedImageView)
                        }
                        else -> {
                            binding.checkJoinMaterialButton.visibility = View.VISIBLE
                            binding.firstRoundedImageView.visibility = View.VISIBLE
                            binding.secondRoundedImageView.visibility = View.VISIBLE
                            binding.moreFrameLayout.visibility = View.VISIBLE
                            binding.thirdRoundedImageView.visibility = View.VISIBLE
                            binding.maxRoundedImageView.visibility = View.VISIBLE
                            binding.maxRoundedImageView.text = eventData?.otherMutualFriend.toString().plus("+")

                            Glide.with(this)
                                .load(eventData?.mutal?.get(0)?.avatar)
                                .placeholder(icPlaceHolderProfile)
                                .centerCrop()
                                .into(binding.firstRoundedImageView)
                            Glide.with(this)
                                .load(eventData?.mutal?.get(1)?.avatar)
                                .placeholder(icPlaceHolderProfile)
                                .centerCrop()
                                .into(binding.secondRoundedImageView)
                            Glide.with(this)
                                .load(eventData?.mutal?.get(2)?.avatar)
                                .placeholder(icPlaceHolderProfile)
                                .centerCrop()
                                .into(binding.thirdRoundedImageView)

                            binding.maxRoundedImageView.text = eventData?.otherMutualFriend.toString().plus("+")

                        }
                    }
                }
                initTabNext()
            } else if(it.hasExtra(EVENT_ID)) {
                venueEventViewModel.getEventsDetails(it.getIntExtra(EVENT_ID, 0))
            } else {}
        }

        when(eventData?.isPrivate){
           1 -> {
               binding.privateAppCompatTextView.visibility = View.VISIBLE
           }
            0 -> {
                binding.privateAppCompatTextView.visibility = View.GONE
            }
            else -> {
                binding.privateAppCompatTextView.visibility = View.GONE
            }
        }

        binding.checkJoinMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            eventData?.mutal?.let {
                val mutualFriendsBottomSheet = MutualFriendsBottomSheet(it)
                mutualFriendsBottomSheet.show(supportFragmentManager, "MutualFriendsBottomSheet")
            }
        }

        binding.joinEventMaterialButton.throttleClicks().subscribeAndObserveOnMainThread {
            venueEventViewModel.addRemoveEventRequest(JoinRequest(eventId = eventData?.id ?: 0))

            eventData?.joinRequestStatus = !(eventData?.joinRequestStatus ?: false)

            if (eventData?.joinRequestStatus ?: false) {
                binding.joinEventMaterialButton.isEnabled = false
                binding.joinEventMaterialButton.text =
                    resources.getString(R.string.label_requested)
            } else {
                binding.joinEventMaterialButton.isEnabled = true
                binding.joinEventMaterialButton.text =
                    resources.getString(R.string.label_join_Request)
            }
        }



        //changeSelectedTabIconColor()
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                //changeSelectedTabIconColor()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })

        binding.backAppCompatImageView.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }
    }

    private fun initTabNext() {
        binding.viewPagerInfo.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if ((loggedInUserCache.getUserId() ?: 0).equals(eventData?.user?.id ?: 0)) {
                    eventAdminTabAdapter.notifyDataSetChanged()
                } else {
                    eventTabAdapter.notifyDataSetChanged()
                }
            }
        })

        TabLayoutMediator(binding.tabLayout, binding.viewPagerInfo) { tab, position ->
            if ((loggedInUserCache.getUserId() ?: 0).equals(eventData?.user?.id ?: 0)) {
                when (position) {
                    0 -> {
                        tab.text = getString(R.string.label_about)
                    }
                    1 -> {
                        tab.text = getString(R.string.label_location)
                    }
                    2 -> {
                        tab.text = getString(R.string.label_request)
                    }
                    3 -> {
                        tab.text = getString(R.string.media)
                    }
                }
            }
            else{
                when (position) {
                    0 -> {
                        tab.text = getString(R.string.label_about)
                    }
                    1 -> {
                        tab.text = getString(R.string.label_location)
                    }
                    2 ->{
                        tab.text = getString(R.string.media)
                    }
                }
            }

        }.attach()
    }

//    Currently \(checkedInCount) friends are checking in this venue and \(taggedCount) friends are tagging this venue in reels/posts.

}