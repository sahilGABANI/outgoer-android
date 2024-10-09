package com.outgoer.ui.home.chat.view

import android.content.Context
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.profile.model.NearByUserResponse
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.roundDoubleVal
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewNearbyPeopleBinding
import com.outgoer.utils.Utility
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class NearbyPeopleView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val nearbyPeopleItemClickStateSubject: PublishSubject<NearByUserResponse> = PublishSubject.create()
    val nearbyPeopleItemClickState: Observable<NearByUserResponse> = nearbyPeopleItemClickStateSubject.hide()

    private val castMessageClickStateSubject: PublishSubject<NearByUserResponse> = PublishSubject.create()
    val castMessageClickState: Observable<NearByUserResponse> = castMessageClickStateSubject.hide()

    private var binding: ViewNearbyPeopleBinding? = null
    private lateinit var chatConversationInfo: NearByUserResponse
    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_nearby_people, this)
        OutgoerApplication.component.inject(this)
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNearbyPeopleBinding.bind(view)

        binding?.apply {
            nearByUserLinearLayout.throttleClicks().subscribeAndObserveOnMainThread {
                nearbyPeopleItemClickStateSubject.onNext(chatConversationInfo)
            }.autoDispose()
            castingMessageFrameLayout.throttleClicks().subscribeAndObserveOnMainThread {
                castMessageClickStateSubject.onNext(chatConversationInfo)
            }
        }
    }

    fun bind(nearByUserResponse: NearByUserResponse) {
        this.chatConversationInfo = nearByUserResponse
        binding?.let {

            it.tvDistance.visibility = if (loggedInUserCache.getUserId() == nearByUserResponse.userId) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }

            val isVenueOwner = MapVenueUserType.VENUE_OWNER.type == nearByUserResponse.userType
            val isCurrentUser = loggedInUserCache.getUserId() == nearByUserResponse.userId

            val message = nearByUserResponse.broadcastMessage.orEmpty()
            val visibility = if (message.isNotEmpty() || isCurrentUser) View.VISIBLE else View.INVISIBLE

            val drawableId = if (isVenueOwner) R.drawable.blue_gredient_color else R.drawable.purple_gradient_color
            val markerDrawableId = if (isVenueOwner) R.drawable.half_circle_blue else R.drawable.half_circle

            val messageBackgroundId = if (isCurrentUser && message.isEmpty()) R.drawable.transparent_gradient_color else drawableId
            val markerBackgroundId = if (isCurrentUser && message.isEmpty()) R.drawable.half_circle_transparent else markerDrawableId

            it.castMessageAppCompatTextView.text = if (isCurrentUser && message.isEmpty()) context.getString(R.string.Cast_Message) else message
            it.castMessageAppCompatTextView.background = ResourcesCompat.getDrawable(resources, messageBackgroundId, null)
            it.markerEdgeAppCompatImageView.setImageDrawable(ResourcesCompat.getDrawable(resources, markerBackgroundId, null))
            it.castingMessageFrameLayout.visibility = visibility

            Glide.with(context)
                .load(nearByUserResponse.avatar)
                .centerCrop()
                .placeholder(resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                .into(it.ivUserProfileImage)

            it.tvUserName.text = nearByUserResponse.username
//            it.tvDistance.text = nearByUserResponse.distance.toString().plus(" miles")
            if (loggedInUserCache.getLoggedInUser()?.loggedInUser?.isMiles == 1) {
                it.tvDistance.text = if (nearByUserResponse.distance != 0.00) {
                    nearByUserResponse.distance?.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_miles))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_miles))
                }
            } else {
                it.tvDistance.text = if (nearByUserResponse.distance != 0.00) {
                    nearByUserResponse.distance?.roundDoubleVal().plus(" ").plus(context.resources.getString(R.string.label_kms))
                } else {
                    "0 ".plus(context.resources.getString(R.string.label_kms))
                }
            }
            it.ivVerified.isVisible = nearByUserResponse.profileVerified == 1
            val storyCount = (nearByUserResponse.storyCount ?: 0) > 0
            it.ivUserProfileImage.background = when {
                storyCount -> Utility.ringGradientColor
                else -> null
            }
/*            if(nearByUserResponse?.isLive?: 0 > 0) {
                it.ivUserProfileImage.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                it.liveAppCompatTextView.visibility = View.VISIBLE

            } else {
                if(nearByUserResponse?.reelCount ?: 0 ?: 0 > 0) {
                    it.ivUserProfileImage.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                    it.liveAppCompatTextView.visibility = View.GONE
                } else if(nearByUserResponse?.postCount ?: 0 > 0) {
                    it.ivUserProfileImage.background = resources.getDrawable(R.drawable.ring_gredient_color, null)
                    it.liveAppCompatTextView.visibility = View.GONE
                } else if(nearByUserResponse?.spontyCount ?: 0 > 0) {
                    it.ivUserProfileImage.background = resources.getDrawable(R.drawable.ring_blue_gredient_color, null)
                    it.liveAppCompatTextView.visibility = View.GONE
                } else {
                    it.ivUserProfileImage.background = null
                    it.liveAppCompatTextView.visibility = View.GONE
                }
            }*/
        }
    }
}