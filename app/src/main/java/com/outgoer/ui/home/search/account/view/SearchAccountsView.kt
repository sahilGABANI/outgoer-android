package com.outgoer.ui.home.search.account.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.follow.model.FollowActionState
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewSearchAccountsBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class SearchAccountsView(
    context: Context
) : ConstraintLayoutWithLifecycle(context) {

    private val searchAccountsStateSubject: PublishSubject<FollowActionState> = PublishSubject.create()
    val searchAccountsState: Observable<FollowActionState> = searchAccountsStateSubject.hide()

    private lateinit var binding: ViewSearchAccountsBinding
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

        val view = View.inflate(context, R.layout.view_search_accounts, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        binding = ViewSearchAccountsBinding.bind(view)
        binding.apply {
            userContainer.throttleClicks().subscribeAndObserveOnMainThread {
                searchAccountsStateSubject.onNext(FollowActionState.UserProfileClick(followUser))
            }.autoDispose()

            btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
                followUser.followStatus = if (followUser.followStatus == null) {
                    0
                } else {
                    if (followUser.followStatus == 1) {
                        0
                    } else {
                        1
                    }
                }
                searchAccountsStateSubject.onNext(FollowActionState.FollowClick(followUser))
                updateFollowUnfollowButton()
            }.autoDispose()

            btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
                followUser.followStatus = if (followUser.followStatus == null) {
                    0
                } else {
                    if (followUser.followStatus == 1) {
                        0
                    } else {
                        1
                    }
                }
                searchAccountsStateSubject.onNext(FollowActionState.FollowingClick(followUser))
                updateFollowUnfollowButton()
            }.autoDispose()
        }
    }

    fun bind(followUser: FollowUser) {
        this.followUser = followUser
        binding.apply {

            Glide.with(context).load(followUser.avatar ?: "").centerCrop().placeholder(R.drawable.ic_chat_user_placeholder).into(ivUserProfile)

            tvUsername.text = followUser.name ?: ""
            tvDescription.text = followUser.username ?: ""

            updateFollowUnfollowButton()
            ivVerified.visibility = if(followUser.profileVerified == 1) View.VISIBLE else View.GONE
        }
    }

    private fun updateFollowUnfollowButton() {
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