package com.outgoer.ui.tag_venue.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.ui.tag_venue.VenueTaggedPostFragment
import com.outgoer.ui.tag_venue.VenueTaggedReelFragment
import com.outgoer.ui.tag_venue.VenueTaggedSpontyFragment

class VenueAllTaggedAdapter(fragmentActivity: FragmentActivity, private val venueId: Int): FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                VenueTaggedReelFragment.newInstance(venueId = venueId)
            }
            1 -> {
                VenueTaggedPostFragment.newInstance(venueId = venueId)
            }
            2 -> {
                VenueTaggedSpontyFragment.newInstance(venueId = venueId)
            }
            else -> {
                VenueTaggedReelFragment.newInstance(venueId = venueId)
            }
        }
    }
}

class VenueTwoTaggedAdapter(fragmentActivity: FragmentActivity, private val venueId: Int, private val reelCount: Int, private val postCount: Int, private val spontyCount: Int): FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                if(reelCount > 0)
                    VenueTaggedReelFragment.newInstance(venueId = venueId)
                else if(postCount > 0)
                    VenueTaggedPostFragment.newInstance(venueId = venueId)
                else
                    VenueTaggedSpontyFragment.newInstance(venueId = venueId)
            }
            1 -> {
                if(spontyCount > 0)
                    VenueTaggedSpontyFragment.newInstance(venueId = venueId)
                else if(postCount > 0)
                    VenueTaggedPostFragment.newInstance(venueId = venueId)
                else
                    VenueTaggedReelFragment.newInstance(venueId = venueId)
            }
            else -> {
                VenueTaggedReelFragment.newInstance(venueId = venueId)
            }
        }
    }
}

class VenueOneTaggedAdapter(fragmentActivity: FragmentActivity, private val venueId: Int, private val reelCount: Int, private val postCount: Int, private val spontyCount: Int): FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 1

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                if(reelCount > 0)
                    VenueTaggedReelFragment.newInstance(venueId = venueId)
                else if(postCount > 0)
                    VenueTaggedPostFragment.newInstance(venueId = venueId)
                else
                    VenueTaggedSpontyFragment.newInstance(venueId = venueId)
            }
            else -> {
                VenueTaggedReelFragment.newInstance(venueId = venueId)
            }
        }
    }
}