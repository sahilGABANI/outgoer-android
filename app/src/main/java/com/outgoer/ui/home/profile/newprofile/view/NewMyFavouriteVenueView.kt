package com.outgoer.ui.home.profile.newprofile.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.venue.model.VenueListInfo
import com.outgoer.api.venue.model.VenueViewClickState
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.roundDoubleVal
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewNewMyFavouriteVenueBinding
import com.outgoer.utils.Utility
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.math.RoundingMode
import javax.inject.Inject

class NewMyFavouriteVenueView(context: Context) : ConstraintLayoutWithLifecycle(context)  {

    private val venueViewClickSubject: PublishSubject<VenueViewClickState> = PublishSubject.create()
    val venueViewClick: Observable<VenueViewClickState> = venueViewClickSubject.hide()

    private lateinit var binding: ViewNewMyFavouriteVenueBinding
    private lateinit var venueListInfo: VenueListInfo
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_new_my_favourite_venue, this)
        OutgoerApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewMyFavouriteVenueBinding.bind(view)

        binding.apply {
            cardContainer.throttleClicks().subscribeAndObserveOnMainThread {
                venueViewClickSubject.onNext(VenueViewClickState.VenueViewClick(venueListInfo))
            }.autoDispose()
            llLike.throttleClicks().subscribeAndObserveOnMainThread {
                venueViewClickSubject.onNext(VenueViewClickState.AddRemoveVenueFavClick(venueListInfo))
            }.autoDispose()
        }
    }

    fun bind(venueListInfo: VenueListInfo) {
        this.venueListInfo = venueListInfo
        binding.apply {
            Glide.with(context)
                .load(venueListInfo.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(binding.profileVenueAppCompatImageView)
            tvPlaceName.text = venueListInfo.name
            tvPlaceRatingCount.text = venueListInfo.reviewAvg?.toBigDecimal()?.setScale(2).toString()
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                distanceAppCompatTextView.text = if (venueListInfo.distance != 0.00) {
                    venueListInfo.distance?.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_miles))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_miles))
                }
            } else {
                distanceAppCompatTextView.text = if (venueListInfo.distance != 0.00) {
                    venueListInfo.distance?.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_kms))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_kms))
                }
            }

            val storyCount = (venueListInfo.storyCount ?: 0) > 0
            profileVenueAppCompatImageView.background = when {
                storyCount -> Utility.ringGradientColor
                else -> null
            }

/*            if(venueListInfo?.isLive?: 0 > 0) {
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
            }*/
        }
    }
}