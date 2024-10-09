package com.outgoer.ui.followdetail.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.ui.followdetail.FollowersFragment
import com.outgoer.ui.followdetail.FollowingFragment
import com.outgoer.ui.followdetail.MutualFragment

class FollowViewPagerAdapter(fragmentActivity: FragmentActivity, private val userId: Int, private val count: Int) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = count

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                FollowersFragment(userId)
            }
            1 -> {
                FollowingFragment(userId)
            }
            2 -> {
                MutualFragment(userId)
            }
            else -> {
                FollowersFragment(userId)
            }
        }
    }
}