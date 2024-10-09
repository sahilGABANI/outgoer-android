package com.outgoer.ui.newnotification.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.ui.newnotification.ActivityNotificationFragment
import com.outgoer.ui.newnotification.GeneralNotificationFragment

class NewNotificationFragmentTabAdapter(fragmentActivity: FragmentActivity): FragmentStateAdapter(fragmentActivity)  {
    override fun getItemCount() = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                ActivityNotificationFragment()
            }
            1 -> {
                GeneralNotificationFragment()

            }

            else -> {
                ActivityNotificationFragment()
            }
        }
    }
}