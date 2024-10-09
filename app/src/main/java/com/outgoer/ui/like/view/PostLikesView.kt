package com.outgoer.ui.like.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.post.model.PostLikesUser
import com.outgoer.api.post.model.PostLikesUserPageState
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewPostLikesBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class PostLikesView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val postLikesViewClickSubject: PublishSubject<PostLikesUserPageState> = PublishSubject.create()
    val postLikesViewClick: Observable<PostLikesUserPageState> = postLikesViewClickSubject.hide()

    private var binding: ViewPostLikesBinding? = null
    private lateinit var postLikesUser: PostLikesUser

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        val view = View.inflate(context, R.layout.view_post_likes, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewPostLikesBinding.bind(view)

        binding?.apply {
            ivUserProfile.throttleClicks().subscribeAndObserveOnMainThread {
                val userInfo = postLikesUser.user
                postLikesViewClickSubject.onNext(PostLikesUserPageState.UserProfileClick(postLikesUser))
            }.autoDispose()

            btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
                postLikesUser.followStatus = !postLikesUser.followStatus
                updateFollowStatus()
                postLikesViewClickSubject.onNext(PostLikesUserPageState.FollowUserClick(postLikesUser))
            }.autoDispose()

            btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
                postLikesUser.followStatus = !postLikesUser.followStatus
                updateFollowStatus()
                postLikesViewClickSubject.onNext(PostLikesUserPageState.FollowUserClick(postLikesUser))
            }.autoDispose()
        }
    }

    fun bind(postLikesUser: PostLikesUser) {
        this.postLikesUser = postLikesUser

        binding?.apply {
            val userInfo = postLikesUser.user

            Glide.with(context)
                .load(userInfo?.avatar ?: "")
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .error(R.drawable.ic_chat_user_placeholder)
                .into(ivUserProfile)

            tvUsername.text = userInfo.name ?: ""
            tvDescription.text = userInfo.username ?: ""
            ivVerified.isVisible = userInfo.profileVerified == 1
//            if (!userInfo.about.isNullOrEmpty()) {
//                tvDescription.text = userInfo.about
//                tvDescription.visibility = View.VISIBLE
//            } else {
//                tvDescription.text = ""
//                tvDescription.visibility = View.INVISIBLE
//            }
        }
        updateFollowStatus()
    }

    private fun updateFollowStatus() {
        binding?.apply {
            val userInfo = postLikesUser.user
            if (loggedInUserId != userInfo.id) {
                if (postLikesUser.followStatus) {
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

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}