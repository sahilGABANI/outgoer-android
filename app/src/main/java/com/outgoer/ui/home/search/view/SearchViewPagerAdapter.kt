package com.outgoer.ui.home.search.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.ui.home.search.account.SearchAccountsFragment
import com.outgoer.ui.home.search.place.SearchPlacesFragment
import com.outgoer.ui.home.search.top.SearchTopFragment

class SearchViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                SearchTopFragment.getInstance()
            }
            1 -> {
                SearchAccountsFragment.getInstance()
            }
            2 -> {
                SearchPlacesFragment.getInstance()
            }
            else -> {
                SearchTopFragment.getInstance()
            }
        }
    }
}