package com.outgoer.ui.home.newmap.venueevents.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.api.event.model.EventData
import com.outgoer.ui.home.newmap.venueevents.EventMediaFragment
import com.outgoer.ui.home.newmap.venueevents.about.AboutFragment
import com.outgoer.ui.home.newmap.venueevents.joinrequests.JoinRequestFragment
import com.outgoer.ui.home.newmap.venueevents.location.LocationFragment

class EventTabAdapter(fragmentActivity: FragmentActivity, private val eventData: EventData) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                AboutFragment.newInstanceWithData(eventData)
            }
            1 -> {
                LocationFragment.newInstanceWithData(eventData)
            }
            2 -> {
                EventMediaFragment.newInstanceWithData(eventData)
            }
            else -> {
                AboutFragment.newInstance()
            }
        }
    }
}

class AdminEventTabAdapter(fragmentActivity: FragmentActivity, private val eventData: EventData) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                AboutFragment.newInstanceWithData(eventData)
            }
            1 -> {
                LocationFragment.newInstanceWithData(eventData)
            }
            2 -> {
                JoinRequestFragment.newInstanceWithData(eventData)
            }
            3 -> {
                EventMediaFragment.newInstanceWithData(eventData)
            }
            else -> {
                AboutFragment.newInstance()
            }
        }
    }
}