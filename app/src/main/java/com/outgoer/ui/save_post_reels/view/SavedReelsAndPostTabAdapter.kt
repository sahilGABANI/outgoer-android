package com.outgoer.ui.save_post_reels.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.api.post.model.MediaObjectType
import com.outgoer.ui.home.home.HomeFragment
import com.outgoer.ui.save_post_reels.SavePostListFragment
import com.outgoer.ui.videorooms.VideoRoomFragment

class SavedReelsAndPostTabAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                SavePostListFragment.newInstance(MediaObjectType.POST.type)
            }
            1 -> {
                SavePostListFragment.newInstance(MediaObjectType.Reel.type)
            }
            else -> {
                SavePostListFragment.newInstance(MediaObjectType.POST.type)
            }
        }
    }
}