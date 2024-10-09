package com.outgoer.ui.followdetail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.tabs.TabLayoutMediator
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityFollowDetailBinding
import com.outgoer.ui.followdetail.view.FollowViewPagerAdapter
import javax.inject.Inject

class FollowDetailActivity : BaseActivity() {

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    companion object {
        private const val INTENT_USER_ID = "INTENT_USER_ID"
        private const val INTENT_FOLLOWER = "INTENT_FOLLOWER"
        private const val INTENT_FOLLOWING = "INTENT_FOLLOWING"
        private const val INTENT_MUTUAL = "INTENT_MUTUAL"

        fun getIntent(
            context: Context,
            userId: Int,
            isFollower: Boolean,
            isFollowing: Boolean = false,
            isMutual: Boolean = false
        ): Intent {
            val intent = Intent(context, FollowDetailActivity::class.java)
            intent.putExtra(INTENT_USER_ID, userId)
            intent.putExtra(INTENT_FOLLOWER, isFollower)
            intent.putExtra(INTENT_FOLLOWING, isFollowing)
            if (isMutual)
                intent.putExtra(INTENT_MUTUAL, isMutual)
            return intent
        }
    }

    private lateinit var binding: ActivityFollowDetailBinding
    private var isFollowing: Boolean = false
    private var isMutual: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OutgoerApplication.component.inject(this)
        binding = ActivityFollowDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isFollowing = intent.getBooleanExtra(INTENT_FOLLOWING, false)
        isMutual = intent.getBooleanExtra(INTENT_MUTUAL, false)
        listenToViewEvents()
    }

    private fun listenToViewEvents() {
        val userId = intent?.getIntExtra(INTENT_USER_ID, -1) ?: -1

        if (userId == -1) {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            finish()
        }.autoDispose()

        val followViewPagerAdapter = FollowViewPagerAdapter(this, userId, if(loggedInUserCache.getUserId() == userId) 2 else 3)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.adapter = followViewPagerAdapter

        if(loggedInUserCache.getUserId() == userId) {
            binding.viewPager.offscreenPageLimit = 1
            when {
                isFollowing -> binding.viewPager.setCurrentItem(1, false)
                else -> binding.viewPager.setCurrentItem(0, false)
            }
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                when (position) {
                    0 -> {
                        tab.text = getString(R.string.label_followers)
                    }
                    1 -> {
                        tab.text = getString(R.string.label_following)
                    }
                }
            }.attach()
        } else {
            binding.viewPager.offscreenPageLimit = 1
            when {
                isFollowing -> binding.viewPager.setCurrentItem(1, false)
                isMutual -> binding.viewPager.setCurrentItem(2, false)
                else -> binding.viewPager.setCurrentItem(0, false)
            }
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                when (position) {
                    0 -> {
                        tab.text = getString(R.string.label_followers)
                    }
                    1 -> {
                        tab.text = getString(R.string.label_following)
                    }
                    2 -> {
                        tab.text = getString(R.string.label_mutual)
                    }
                }
            }.attach()
        }
    }
}