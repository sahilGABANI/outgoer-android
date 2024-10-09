package com.outgoer.ui.home.newReels

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cn.jzvd.Jzvd
import com.google.android.material.tabs.TabLayout
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseFragment
import com.outgoer.base.RxBus
import com.outgoer.base.RxEvent
import com.outgoer.base.ViewModelFactory
import com.outgoer.base.extension.*
import com.outgoer.databinding.FragmentReelsBinding
import com.outgoer.ui.home.chat.NewChatConversationActivity
import com.outgoer.ui.home.newReels.view.ReelsFragmentTabAdapter
import com.outgoer.ui.home.newReels.viewmodel.MainReelViewState
import com.outgoer.ui.home.newReels.viewmodel.MainReelsViewModel
import com.outgoer.ui.home.search.SearchActivity
import com.outgoer.ui.newnotification.NewNotificationActivity
import com.outgoer.utils.TabLayoutMediatorLocal
import com.outgoer.utils.Utility.isSpontyOpen
import timber.log.Timber
import javax.inject.Inject


class NewReelsFragment : BaseFragment() {

    companion object {
        private var _binding: FragmentReelsBinding? = null
        val binding get() = _binding!!

        private var SPONTY_OPEN_HERE = "SPONTY_OPEN_HERE"
        private var selectedTab: String = "Feed"

        @JvmStatic
        fun newInstance(isSponty: Boolean): NewReelsFragment {
            val newReelsFragment = NewReelsFragment()
            val bundle = Bundle()
            bundle.putBoolean(SPONTY_OPEN_HERE, isSponty)
            newReelsFragment.arguments = bundle
            return newReelsFragment
        }
    }

    @Inject
    internal lateinit var viewModelFactory: ViewModelFactory<MainReelsViewModel>
    private lateinit var mainReelsViewModel: MainReelsViewModel



    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    private lateinit var reelsFragmentTabAdapter: ReelsFragmentTabAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OutgoerApplication.component.inject(this)
        mainReelsViewModel = getViewModelFromFactory(viewModelFactory)

        listenToViewEvents()
        listenToViewModel()

        binding.ivNotification.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(NewNotificationActivity.getIntent(requireContext()))
        }
    }

    fun updateSpontyViewNavigation() {
        binding.viewPager.setCurrentItem(2, false)
    }

    fun refreshPages() {
        RxBus.publish(RxEvent.DataReload(selectedTab))
    }

    @SuppressLint("NewApi")
    private fun listenToViewEvents() {

        reelsFragmentTabAdapter = ReelsFragmentTabAdapter(requireActivity())
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.adapter = reelsFragmentTabAdapter
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1))
        TabLayoutMediatorLocal(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.live)
                }
                1 -> {
                    tab.text = getString(R.string.label_feed)
                }
                2 -> {
                    tab.text = getString(R.string.label_sponty)
                }
            }
        }.attach()

        if (arguments?.getBoolean(SPONTY_OPEN_HERE) == true) {
            isSpontyOpen = false
            binding.viewPager.setCurrentItem(2, false)
        } else {
            binding.viewPager.setCurrentItem(1, false)
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedTab = tab?.text.toString()
                if (tab?.text == getString(R.string.label_feed)) {
                    RxBus.publish(RxEvent.RefreshHomePagePostPlayVideo(isResumed))
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                if (tab?.text == getString(R.string.label_feed)) {
                    Jzvd.goOnPlayOnPause()
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })

        binding.ivSearch.setOnClickListener {
            startActivityWithDefaultAnimation(SearchActivity.getIntent(requireContext()))
        }

        binding.ivChat.throttleClicks().subscribeAndObserveOnMainThread {
            startActivityWithDefaultAnimation(NewChatConversationActivity.getIntent(requireContext()))
        }.autoDispose()
    }

    private fun listenToViewModel() {
        mainReelsViewModel.mainReelsViewState.subscribeAndObserveOnMainThread {
            when (it) {
                is MainReelViewState.ErrorMessage -> {
                    showLongToast(it.errorMessage)
                }
                is MainReelViewState.LoadingState -> {
                }
                is MainReelViewState.SuccessMessage -> {
                    showLongToast(it.successMessage)
                }
                is MainReelViewState.MyProfileData -> {
                    displayUnreadCount(it.outgoerUser.notificationCount, it.outgoerUser.messageCount)

                    println("New Height: " + binding.viewPager.height)
                    println("New Height:headerTab " + binding.headerTab.height)
                    println("New Height:tabBar " + binding.tabBar.height)
                }

                is MainReelViewState.LoadVenueDetail -> {
                    displayUnreadCount(it.venueDetail.notificationCount, it.venueDetail.messageCount)
                }
            }
        }.autoDispose()
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("NewReelsFragment").i("onPause")
        //RxBus.publish(RxEvent.RefreshDiscoverChangeFragment)
        RxBus.publish(RxEvent.RefreshHomePagePost)
        RxBus.publish(RxEvent.RefreshHomePagePostPlayVideo(false))
        Jzvd.goOnPlayOnPause()
    }

    override fun onResume() {
        super.onResume()
        if (isResumed) {
//            RxBus.publish(RxEvent.RefreshDiscoverChangeAutoPlayFragment)
//            RxBus.publish(RxEvent.RefreshHomePagePost)
//            RxBus.publish(RxEvent.RefreshHomePagePostPlayVideo(isResumed))
            if (binding.viewPager.currentItem == 1) {
                RxBus.publish(RxEvent.RefreshHomePagePostPlayVideo(isResumed))
            }
        }
        RxBus.listen(RxEvent.UpdateNotificationBadge::class.java).subscribeAndObserveOnMainThread {
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.userType == "venue_owner") mainReelsViewModel.getVenueDetail(
                loggedInUserCache.getUserId() ?: 0
            ) else mainReelsViewModel.myProfile()
        }.autoDispose()

        RxBus.listen(RxEvent.CheckHomeFragmentIsVisible::class.java).subscribeAndObserveOnMainThread {
            if (binding.viewPager.currentItem == 1) {
                RxBus.publish(RxEvent.RefreshHomePagePostPlayVideo(isResumed))
            }
        }.autoDispose()
    }

    override fun onDestroy() {
        // RxBus.publish(RxEvent.RefreshDiscoverChangeFragment)
        RxBus.publish(RxEvent.RefreshHomePagePost)
        super.onDestroy()
    }

    private fun displayUnreadCount(notificationUnreadCount: Int?, messageCount: Int?) {

        val notificationCount = notificationUnreadCount ?: 0
        val messageUnreadCount = messageCount ?: 0
        if (notificationCount > 0) {
            binding.notifCountAppCompatTextView.visibility = View.VISIBLE

            if (notificationCount < 100) {
                binding.notifCountAppCompatTextView.text = "" + notificationCount
            } else {
                binding.notifCountAppCompatTextView.text = "99+"
            }
        } else {
            binding.notifCountAppCompatTextView.visibility = View.GONE
        }

        if (messageUnreadCount > 0) {
            binding.messageCountAppCompatTextView.visibility = View.VISIBLE

            if (messageUnreadCount < 100) {
                binding.messageCountAppCompatTextView.text = "" + messageUnreadCount
            } else {
                binding.messageCountAppCompatTextView.text = "99+"
            }
        } else {
            binding.messageCountAppCompatTextView.visibility = View.GONE
        }
    }
}