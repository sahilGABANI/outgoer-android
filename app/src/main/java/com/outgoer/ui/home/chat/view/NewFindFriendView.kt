package com.outgoer.ui.home.chat.view

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.LoggedInUserCache
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.application.OutgoerApplication
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewNewFindFriendBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject
import kotlin.properties.Delegates

class NewFindFriendView(context: Context) : ConstraintLayoutWithLifecycle(context)  {

    private val findFriendItemClickStateSubject: PublishSubject<OutgoerUser> = PublishSubject.create()
    val findFriendItemClickState: Observable<OutgoerUser> = findFriendItemClickStateSubject.hide()

    private val followClickStateSubject: PublishSubject<OutgoerUser> = PublishSubject.create()
    val followClickState: Observable<OutgoerUser> = followClickStateSubject.hide()

    private val followingClickStateSubject: PublishSubject<OutgoerUser> = PublishSubject.create()
    val followingClickState: Observable<OutgoerUser> = followingClickStateSubject.hide()

    private var binding: ViewNewFindFriendBinding? = null
    private lateinit var chatConversationInfo: OutgoerUser

    @Inject
    lateinit var loggedInUserCache: LoggedInUserCache
    private var loggedInUserId by Delegates.notNull<Int>()

    init {
        inflateUi()
    }

    private fun inflateUi() {
        OutgoerApplication.component.inject(this)
        loggedInUserId = loggedInUserCache.getLoggedInUser()?.loggedInUser?.id ?: 0

        val view = View.inflate(context, R.layout.view_new_find_friend, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewNewFindFriendBinding.bind(view)

        binding?.apply {
            btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
                followClickStateSubject.onNext(chatConversationInfo)
            }
            btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
                followingClickStateSubject.onNext(chatConversationInfo)
            }
            userContainer.throttleClicks().subscribeAndObserveOnMainThread {
                findFriendItemClickStateSubject.onNext(chatConversationInfo)
            }
        }
    }

    fun bind(userInfo: OutgoerUser) {
        this.chatConversationInfo = userInfo
        val icLogoPlaceholder = ContextCompat.getDrawable(context, R.drawable.ic_chat_user_placeholder)

/*        val gradientDrawableBlue = GradientDrawable(
            GradientDrawable.Orientation.BL_TR, // 135 degrees (bottom-left to top-right)
            intArrayOf(
                resources.getColor(R.color.color_76c1ed),
                resources.getColor(R.color.color_4152c1),
                resources.getColor(R.color.color_4152c1)
            )
        ).apply {
            cornerRadius = resources.getDimension(com.intuit.sdp.R.dimen._200sdp)
        }*/

        val gradientDrawablePurple = GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT, // Left to Right gradient
            intArrayOf(
                resources.getColor(R.color.color_FD8AFF), // Start color
                resources.getColor(R.color.color_B421FF)  // End color
            )
        ).apply {
            cornerRadius = resources.getDimension(com.intuit.sdp.R.dimen._200sdp)
        }

        binding?.apply {
            Glide.with(context)
                .load(userInfo.avatar)
                .centerCrop()
                .placeholder(icLogoPlaceholder)
                .into(ivUserProfileImage)

            tvUserName.text = userInfo.username
            val storyCount = (userInfo.storyCount ?: 0) > 0
            ivUserProfileImage.background = when {
                storyCount -> gradientDrawablePurple
                else -> null
            }
/*            if((userInfo.isLive ?: 0) > 0) {
                ivUserProfileImage.background = gradientDrawablePurple
                liveProfileAppCompatTextView.visibility = View.VISIBLE
            } else {
                if((userInfo.reelCount ?: 0) > 0) {
                    ivUserProfileImage.background = gradientDrawablePurple
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else if((userInfo.postCount ?: 0) > 0) {
                    ivUserProfileImage.background = gradientDrawablePurple
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else if((userInfo.spontyCount ?: 0) > 0) {
                    ivUserProfileImage.background = gradientDrawableBlue
                    liveProfileAppCompatTextView.visibility = View.GONE
                } else {
                    ivUserProfileImage.background = null
                    liveProfileAppCompatTextView.visibility = View.GONE
                }
            }*/
            updateFollowStatus()
            ivVerified.isVisible = userInfo.profileVerified == 1
        }

    }

    private fun updateFollowStatus() {
        binding?.apply {
            if (loggedInUserId != chatConversationInfo.id) {
                if (chatConversationInfo.followStatus == null) {
                    btnFollow.visibility = View.VISIBLE
                    btnFollowing.visibility = View.GONE
                } else {
                    if (chatConversationInfo.followStatus == 1) {
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

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}