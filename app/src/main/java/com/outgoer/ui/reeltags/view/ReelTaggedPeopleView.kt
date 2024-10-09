package com.outgoer.ui.reeltags.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.reels.model.ReelTaggedPeopleState
import com.outgoer.api.reels.model.ReelsTagsItem
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewTaggedPeopleBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class ReelTaggedPeopleView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val reelTaggedPeopleClickSubject: PublishSubject<ReelTaggedPeopleState> = PublishSubject.create()
    val reelTaggedPeopleClick: Observable<ReelTaggedPeopleState> = reelTaggedPeopleClickSubject.hide()

    private lateinit var binding: ViewTaggedPeopleBinding
    private lateinit var reelsTagsItem: ReelsTagsItem

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        val view = View.inflate(context, R.layout.view_tagged_people, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewTaggedPeopleBinding.bind(view)

        binding.apply {
            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                reelTaggedPeopleClickSubject.onNext(ReelTaggedPeopleState.UserProfileClick(reelsTagsItem))
            }.autoDispose()

            btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
                reelsTagsItem.followStatus = !reelsTagsItem.followStatus
                reelTaggedPeopleClickSubject.onNext(ReelTaggedPeopleState.Follow(reelsTagsItem))
                updateFollowUnfollowButton()
            }.autoDispose()

            btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
                reelsTagsItem.followStatus = !reelsTagsItem.followStatus
                reelTaggedPeopleClickSubject.onNext(ReelTaggedPeopleState.Unfollow(reelsTagsItem))
                updateFollowUnfollowButton()
            }.autoDispose()
        }
    }

    fun bind(reelsTagsItem: ReelsTagsItem) {
        this.reelsTagsItem = reelsTagsItem
        binding.apply {
            Glide.with(context).load(reelsTagsItem.user?.avatar ?: "").placeholder(R.drawable.ic_chat_user_placeholder).centerCrop()
                .into(ivUserProfile)

            tvUsername.text = reelsTagsItem.user?.username ?: ""
            tvDescription.text = reelsTagsItem.user?.about ?: ""
            updateFollowUnfollowButton()
        }
    }

    private fun updateFollowUnfollowButton() {
        binding.apply {
            if (loggedInUserId != reelsTagsItem.userId) {
                if (reelsTagsItem.followStatus) {
                    btnFollow.visibility = View.GONE
                    btnFollowing.visibility = View.VISIBLE
                } else {
                    btnFollow.visibility = View.VISIBLE
                    btnFollowing.visibility = View.GONE
                }
            } else {
                btnFollow.visibility = View.GONE
                btnFollowing.visibility = View.GONE
            }
        }
    }
}