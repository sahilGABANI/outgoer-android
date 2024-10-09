package com.outgoer.ui.home.profile.newprofile.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.ui.home.profile.newprofile.NewMyFavouriteVenueFragment
import com.outgoer.ui.home.profile.newprofile.NewMyPostsFragment
import com.outgoer.ui.home.profile.newprofile.NewMyReelFragment

class NewMyProfileFragmentTabAdapter(
    fragmentActivity: FragmentActivity,
    private val isVenueUser: Boolean,
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                NewMyPostsFragment.newInstance()
            }
            1 -> {
                NewMyReelFragment.newInstance()
            }
            2 -> {
                NewMyFavouriteVenueFragment.newInstance()
            }

            else -> {
                NewMyReelFragment.newInstance()
            }
        }
    }
}