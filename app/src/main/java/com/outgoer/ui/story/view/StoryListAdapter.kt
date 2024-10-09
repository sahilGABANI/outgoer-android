package com.outgoer.ui.story.view

import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.ui.story.StoryListFragment
import java.util.ArrayList


class StoryListAdapter(
    private val onClick: (Int) -> Unit,
    private val onBackPress: (Int) -> Unit,
    private val size: Int,
    private val list: ArrayList<StoryListResponse>,
    fragmentActivity: FragmentActivity
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = size

    override fun createFragment(position: Int): StoryListFragment = StoryListFragment.newInstance({
        onClick(it)
    }, {
        onBackPress(it)
    }, position, list[position])
}