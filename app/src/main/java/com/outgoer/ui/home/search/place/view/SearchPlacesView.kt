package com.outgoer.ui.home.search.place.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowActionState
import com.outgoer.api.follow.model.PlaceFollowActionState
import com.outgoer.api.venue.model.VenueListInfo
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewSearchPlacesBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.math.RoundingMode
import java.text.DecimalFormat
import javax.inject.Inject
import kotlin.properties.Delegates

class SearchPlacesView(
    context: Context
) : ConstraintLayoutWithLifecycle(context) {

    private val searchPlaceClickSubject: PublishSubject<VenueListInfo> = PublishSubject.create()
    val searchPlaceClick: Observable<VenueListInfo> = searchPlaceClickSubject.hide()

    private val followActionStateSubject: PublishSubject<PlaceFollowActionState> = PublishSubject.create()
    val followActionState: Observable<PlaceFollowActionState> = followActionStateSubject.hide()

    private lateinit var binding: ViewSearchPlacesBinding
    private lateinit var venueListInfo: VenueListInfo

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()


    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        val view = View.inflate(context, R.layout.view_search_places, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        binding = ViewSearchPlacesBinding.bind(view)
        binding.apply {
            cardContainer.throttleClicks().subscribeAndObserveOnMainThread {
                searchPlaceClickSubject.onNext(venueListInfo)
            }.autoDispose()

            btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
                followActionStateSubject.onNext(PlaceFollowActionState.FollowClick(venueListInfo))
            }.autoDispose()

            btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
                followActionStateSubject.onNext(PlaceFollowActionState.FollowingClick(venueListInfo))
            }.autoDispose()
        }
    }

    fun bind(venueListInfo: VenueListInfo) {
        this.venueListInfo = venueListInfo
        binding.apply {

            Glide.with(context)
                .load(venueListInfo.avatar)
                .placeholder(R.drawable.venue_placeholder)
                .into(binding.profileVenueAppCompatImageView)
            tvPlaceName.text = venueListInfo.name
            tvPlaceRatingCount.text = venueListInfo.reviewAvg.toString()


            val df = DecimalFormat("####0.00")

            distanceAppCompatTextView.text = "${df.format(venueListInfo?.distance).plus(" miles ")}"
            updateFollowStatus()

            if(venueListInfo?.isLive?: 0 > 0) {
                profileVenueAppCompatImageView.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                liveAppCompatTextView.visibility = View.VISIBLE

            } else {
                if(venueListInfo?.reelCount ?: 0 ?: 0 > 0) {
                    profileVenueAppCompatImageView.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                    liveAppCompatTextView.visibility = View.GONE
                } else if(venueListInfo?.postCount ?: 0 > 0) {
                    profileVenueAppCompatImageView.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                    liveAppCompatTextView.visibility = View.GONE
                } else if(venueListInfo?.spontyCount ?: 0 > 0) {
                    profileVenueAppCompatImageView.background = resources.getDrawable(R.drawable.ring_blue_gredient_color, null)
                    liveAppCompatTextView.visibility = View.GONE
                } else {
                    profileVenueAppCompatImageView.background = null
                    liveAppCompatTextView.visibility = View.GONE
                }
            }
            ivVerified.visibility = if(venueListInfo.profileVerified == 1) View.VISIBLE else View.GONE
        }
    }

    private fun updateFollowStatus() {
        binding.apply {
            if (loggedInUserId != venueListInfo.id) {
                if (venueListInfo.followStatus == null) {
                    btnFollow.visibility = View.VISIBLE
                    btnFollowing.visibility = View.GONE
                } else {
                    if (venueListInfo.followStatus == 1) {
                        btnFollow.visibility = View.GONE
                        btnFollowing.visibility = View.VISIBLE
                    } else {
                        btnFollow.visibility = View.VISIBLE
                        btnFollowing.visibility = View.GONE
                    }
                }
            } else {
                btnFollow.visibility = View.INVISIBLE
                btnFollowing.visibility = View.GONE
            }
        }
    }
}