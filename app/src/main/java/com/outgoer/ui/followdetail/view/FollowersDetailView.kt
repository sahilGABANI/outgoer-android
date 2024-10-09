package com.outgoer.ui.followdetail.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowActionState
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.FollowersFollowingDetailViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class FollowersDetailView(
    context: Context
) : ConstraintLayoutWithLifecycle(context) {

    private val followActionStateSubject: PublishSubject<FollowActionState> = PublishSubject.create()
    val followActionState: Observable<FollowActionState> = followActionStateSubject.hide()

    private lateinit var binding: FollowersFollowingDetailViewBinding
    private lateinit var followUser: FollowUser

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        val view = View.inflate(context, R.layout.followers_following_detail_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        binding = FollowersFollowingDetailViewBinding.bind(view)
        binding.apply {
            btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
                followActionStateSubject.onNext(FollowActionState.FollowClick(followUser))
            }.autoDispose()

            btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
                followActionStateSubject.onNext(FollowActionState.FollowingClick(followUser))
            }.autoDispose()

            userContainer.throttleClicks().subscribeAndObserveOnMainThread {
                followActionStateSubject.onNext(FollowActionState.UserProfileClick(followUser))
            }.autoDispose()
        }
    }

    fun bind(followUser: FollowUser) {
        this.followUser = followUser
        binding.apply {
            if (followUser.userType == MapVenueUserType.VENUE_OWNER.type) {
                tvUsername.text = followUser.name ?: ""
            } else {
                tvUsername.text = followUser.username ?: ""
            }

            if (followUser.totalFollowers != 0) {
                tvTotalCount.isVisible = true
                tvTotalCount.text = followUser.totalFollowers.toString().plus(" ").plus(context.getString(R.string.label_followers))
            } else {
                tvTotalCount.isVisible = false
            }

            Glide.with(context)
                .load(followUser.avatar)
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(ivUserProfile)

            updateFollowStatus()
            ivVerified.isVisible = followUser.profileVerified == 1
        }
    }

    private fun updateFollowStatus() {
        binding.apply {
            if (loggedInUserId != followUser.id) {
                if (followUser.followStatus == null) {
                    btnFollow.visibility = View.VISIBLE
                    btnFollowing.visibility = View.GONE
                } else {
                    if (followUser.followStatus == 1) {
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