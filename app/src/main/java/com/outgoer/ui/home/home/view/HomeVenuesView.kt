package com.outgoer.ui.home.home.view

import android.content.Context
import android.view.View
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.HomePageStoryInfoState
import com.outgoer.api.post.model.PostInfo
import com.outgoer.api.post.model.SpontyActionState
import com.outgoer.api.story.model.StoryListResponse
import com.outgoer.api.venue.model.VenueDetail
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.HomeSpontyItemBinding
import com.outgoer.databinding.StoryListBinding
import com.outgoer.ui.sponty.view.SpontyListAdapter
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class HomeVenuesView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val venueDetailActionStateSubject: PublishSubject<VenueDetail> = PublishSubject.create()
    val venueDetailActionState: Observable<VenueDetail> = venueDetailActionStateSubject.hide()

    private var binding: HomeSpontyItemBinding? = null
    private lateinit var postInfo: PostInfo
    private lateinit var homeVenueAdapter: HomeVenueAdapter
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.home_sponty_item, this)

        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = HomeSpontyItemBinding.bind(view)
        binding?.apply {

        }
    }

    fun bind(postInfoResponse: PostInfo) {
        this.postInfo = postInfoResponse

        homeVenueAdapter = HomeVenueAdapter(context).apply {
            venueDetailActionState.subscribeAndObserveOnMainThread {
                venueDetailActionStateSubject.onNext(it)
            }
        }

        binding?.apply {
            titleVerticalTextView.text = resources.getString(R.string.trending_venues)
            titleVerticalTextView.background = resources.getDrawable(R.drawable.home_blue_gredient_color, null)
            spontyRecyclerView.adapter = homeVenueAdapter
            homeVenueAdapter.listOfVenue = postInfoResponse.venues
        }
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}