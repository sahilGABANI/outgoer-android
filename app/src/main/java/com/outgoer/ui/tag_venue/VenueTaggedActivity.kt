package com.outgoer.ui.tag_venue

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivityVenueTaggedBinding
import com.outgoer.ui.tag_venue.view.VenueAllTaggedAdapter
import com.outgoer.ui.tag_venue.view.VenueOneTaggedAdapter
import com.outgoer.ui.tag_venue.view.VenueTwoTaggedAdapter

class VenueTaggedActivity : BaseActivity() {

    private lateinit var binding: ActivityVenueTaggedBinding

    private var venueId: Int = 0
    private var reelCount: Int = 0
    private var postCount: Int = 0
    private var spontyCount: Int = 0

    companion object {
        private val VENUE_ID = "VENUE_ID"
        private val VENUE_REEL_SPONTY = "VENUE_REEL_SPONTY"
        private val VENUE_POST_SPONTY = "VENUE_POST_SPONTY"
        private val VENUE_SPONTY_COUNT = "VENUE_SPONTY_COUNT"
        fun getIntent(
            context: Context,
            venueId: Int,
            reelCount: Int,
            postCount: Int,
            spontyCount: Int
        ): Intent {
            val intent = Intent(context, VenueTaggedActivity::class.java)
            intent.putExtra(VENUE_ID, venueId)
            intent.putExtra(VENUE_REEL_SPONTY, reelCount)
            intent.putExtra(VENUE_POST_SPONTY, postCount)
            intent.putExtra(VENUE_SPONTY_COUNT, spontyCount)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityVenueTaggedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initUI()
        binding.backBtn.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()
    }

    private fun initUI() {
        venueId = intent?.getIntExtra(VENUE_ID, -1) ?: -1
        reelCount = intent?.getIntExtra(VENUE_REEL_SPONTY, -1) ?: -1
        postCount = intent?.getIntExtra(VENUE_POST_SPONTY, -1) ?: -1
        spontyCount = intent?.getIntExtra(VENUE_SPONTY_COUNT, -1) ?: -1

        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 3
        binding.viewPager.adapter = if (reelCount > 0 && postCount > 0 && spontyCount > 0) {
            VenueAllTaggedAdapter(this@VenueTaggedActivity, venueId)
        } else if ((reelCount > 0 && postCount > 0) || (postCount > 0 && spontyCount > 0) || (reelCount > 0 && spontyCount > 0)) {
            VenueTwoTaggedAdapter(
                this@VenueTaggedActivity,
                venueId,
                reelCount,
                postCount,
                spontyCount
            )
        } else {
            VenueOneTaggedAdapter(
                this@VenueTaggedActivity,
                venueId,
                reelCount,
                postCount,
                spontyCount
            )
        }
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            if (reelCount > 0 && postCount > 0 && spontyCount > 0) {
                when (position) {
                    0 -> {
                        tab.text = getString(R.string.label_reel)
                    }
                    1 -> {
                        tab.text = getString(R.string.label_feeds)
                    }
                    2 -> {
                        tab.text = getString(R.string.label_sponty)
                    }
                }
            } else if ((reelCount > 0 && postCount > 0) || (postCount > 0 && spontyCount > 0) || (reelCount > 0 && spontyCount > 0)) {
                when (position) {
                    0 -> {
                        tab.text = if(postCount > 0) getString(R.string.label_feeds) else getString(R.string.label_reel)
                    }
                    1 -> {
                        tab.text = if(spontyCount > 0) getString(R.string.label_sponty) else getString(R.string.label_feeds)
                    }
                }
            } else {
                when (position) {
                    0 -> {
                        tab.text = if(reelCount > 0) getString(R.string.label_reel) else if(postCount > 0) getString(R.string.label_feeds) else getString(R.string.label_sponty)
                    }
                }
            }
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })
    }
}