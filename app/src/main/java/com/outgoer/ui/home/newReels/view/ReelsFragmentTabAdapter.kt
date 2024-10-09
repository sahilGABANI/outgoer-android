package com.outgoer.ui.home.newReels.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.ui.home.home.HomeFragment
import com.outgoer.ui.home.newReels.DiscoverReelsFragment
import com.outgoer.ui.sponty.SpontyListFragment
import com.outgoer.ui.videorooms.VideoRoomFragment

class ReelsFragmentTabAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                VideoRoomFragment.newInstance()
            }
            1 -> {
                HomeFragment.newInstance()
            }
            2 -> {
                SpontyListFragment.newInstance()
            }
            else -> {
                VideoRoomFragment.newInstance()
            }
        }
    }
}