package com.outgoer.ui.posttags.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.PostTagsItem
import com.outgoer.api.post.model.PostTaggedPeopleState
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewTaggedPeopleBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class PostTaggedPeopleView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val postTaggedPeopleClickSubject: PublishSubject<PostTaggedPeopleState> = PublishSubject.create()
    val postTaggedPeopleClick: Observable<PostTaggedPeopleState> = postTaggedPeopleClickSubject.hide()

    private lateinit var binding: ViewTaggedPeopleBinding
    private lateinit var postTagsItem: PostTagsItem

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
                if (loggedInUserId != postTagsItem.userId) {
                    postTaggedPeopleClickSubject.onNext(PostTaggedPeopleState.UserProfileClick(postTagsItem))
                }
            }.autoDispose()

            btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
                postTagsItem.followStatus = !postTagsItem.followStatus
                postTaggedPeopleClickSubject.onNext(PostTaggedPeopleState.Follow(postTagsItem))
                updateFollowUnfollowButton()
            }.autoDispose()

            btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
                postTagsItem.followStatus = !postTagsItem.followStatus
                postTaggedPeopleClickSubject.onNext(PostTaggedPeopleState.Unfollow(postTagsItem))
                updateFollowUnfollowButton()
            }.autoDispose()
        }
    }

    fun bind(postTagsItem: PostTagsItem) {
        this.postTagsItem = postTagsItem
        binding.apply {
            Glide.with(context)
                .load(postTagsItem.user?.avatar ?: "")
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .centerCrop()
                .into(ivUserProfile)

            tvUsername.text = postTagsItem.user?.username ?: ""
            tvDescription.text = postTagsItem.user?.about ?: ""
            updateFollowUnfollowButton()
        }
    }

    private fun updateFollowUnfollowButton() {
        binding.apply {
            if (loggedInUserId != postTagsItem.userId) {
                if (postTagsItem.followStatus) {
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