package com.outgoer.ui.create_story.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.ui.create_story.OutgoerVenueFragment
import com.outgoer.ui.home.home.HomeFragment
import com.outgoer.ui.sponty.SpontyListFragment
import com.outgoer.ui.videorooms.VideoRoomFragment

class LocationSelectionTabAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                OutgoerVenueFragment.newInstance("1")
            }
            1 -> {
                OutgoerVenueFragment.newInstance("2")
            }
            else -> {
                OutgoerVenueFragment.newInstance("1")
            }
        }
    }
}