package com.outgoer.ui.save_post_reels

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.outgoer.R
import com.outgoer.base.BaseActivity
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.databinding.ActivitySavePostReelsBinding
import com.outgoer.ui.save_post_reels.view.SavedReelsAndPostTabAdapter

class SavePostReelsActivity : BaseActivity() {
    companion object {
        fun getIntent(context: Context): Intent {
            return Intent(context, SavePostReelsActivity::class.java)
        }
    }

    private lateinit var binding: ActivitySavePostReelsBinding
    private lateinit var savedReelsAndPostTabAdapter: SavedReelsAndPostTabAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavePostReelsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        listenToViewEvents()
    }


    @SuppressLint("NewApi")
    private fun listenToViewEvents() {
        binding.ivBack.throttleClicks().subscribeAndObserveOnMainThread {
            onBackPressed()
        }.autoDispose()
        savedReelsAndPostTabAdapter = SavedReelsAndPostTabAdapter(this@SavePostReelsActivity)
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.adapter = savedReelsAndPostTabAdapter
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.label_posts)
                    tab.icon = resources.getDrawable(R.drawable.ic_posts_icon, null)
                }
                1 -> {
                    tab.text = getString(R.string.label_reels)
                    tab.icon = resources.getDrawable(R.drawable.ic_reels_icon, null)
                }
            }
        }.attach()
        binding.viewPager.setCurrentItem(0, false)

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