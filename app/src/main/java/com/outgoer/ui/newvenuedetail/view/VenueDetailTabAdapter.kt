package com.outgoer.ui.newvenuedetail.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.ui.newvenuedetail.VenueDetailAboutFragment
import com.outgoer.ui.newvenuedetail.VenueDetailPhotosFragment
import com.outgoer.ui.newvenuedetail.VenueDetailReviewFragment
import com.outgoer.ui.newvenuedetail.VenueDetailsEventFragment
import com.outgoer.ui.otherprofile.NewOtherUserPostFragment

class VenueDetailTabAdapter(
    fragmentActivity: FragmentActivity,
    private val venueDetail: VenueDetail
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount() = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                VenueDetailAboutFragment.newInstanceWithData(venueDetail)
            }
            1 -> {
                VenueDetailReviewFragment.newInstanceWithData(venueDetail)
            }
            2 -> {
                VenueDetailPhotosFragment.newInstanceWithData(venueDetail)
            }
            3 -> {
                NewOtherUserPostFragment.newInstanceWithData(venueDetail.id)
            }
            4 -> {
                VenueDetailsEventFragment.newInstanceWithData(venueDetail)
            }
            else -> {
                VenueDetailAboutFragment.newInstance()
            }
        }
    }
}