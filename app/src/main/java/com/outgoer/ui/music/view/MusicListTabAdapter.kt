package com.outgoer.ui.music.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.api.music.model.MusicCategoryResponse
import com.outgoer.ui.music.MusicListFragment

class MusicListTabAdapter(
    fragmentActivity: FragmentActivity,
    private val size: Int,
    private val mediaType: String,
    private val listOfMusic: ArrayList<MusicCategoryResponse>,
    private val videoPath: String? = null,
    private val postType: String
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = size

    override fun createFragment(position: Int): Fragment {
        return MusicListFragment.newInstance(position, mediaType, listOfMusic, videoPath)
    }
}