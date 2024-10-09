package com.outgoer.ui.otherprofile.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.ui.home.profile.newprofile.NewMyReelFragment
import com.outgoer.ui.otherprofile.NewOtherUserPostFragment


class NewOtherUserProfileFragmentTabAdapter(fragmentActivity: FragmentActivity, private val userId: Int) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                NewOtherUserPostFragment.newInstanceWithData(userId)
            }
            1 -> {
                NewMyReelFragment.newInstanceWithData(userId)
            }
            else -> {
                NewOtherUserPostFragment.newInstanceWithData(userId)
            }
        }
    }

}