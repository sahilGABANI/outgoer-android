package com.outgoer.ui.home.newmap

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MapTabAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity) {
    private var fragmentList:List<Fragment> = listOf()
    override fun getItemCount() = fragmentList.size

    override fun createFragment(position: Int): Fragment {
        return fragmentList[position]
    }

    fun addFragment(fragments: List<Fragment>){
        fragmentList = fragments
    }
}