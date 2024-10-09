package com.outgoer.ui.chat.newview

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.profile.model.NearByUserResponse
import com.outgoer.api.venue.model.MapVenueUserType
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.NewMessageFriendViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class NewMessageFollowingView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val profileItemClickStateSubject: PublishSubject<NearByUserResponse> = PublishSubject.create()
    val profileItemClickState: Observable<NearByUserResponse> = profileItemClickStateSubject.hide()

    private val followClickStateSubject: PublishSubject<NearByUserResponse> = PublishSubject.create()
    val followClickState: Observable<NearByUserResponse> = followClickStateSubject.hide()

    private var binding: NewMessageFriendViewBinding? = null
    private lateinit var chatConversationInfo: NearByUserResponse

    init {
        inflateUi()
    }

    private fun inflateUi() {

        val view = View.inflate(context, R.layout.new_message_friend_view, this)
        layoutParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        binding = NewMessageFriendViewBinding.bind(view)

        binding?.apply {
            addProfile.throttleClicks().subscribeAndObserveOnMainThread {
                followClickStateSubject.onNext(chatConversationInfo)
            }

            ivUserProfileImage.throttleClicks().subscribeAndObserveOnMainThread {
                profileItemClickStateSubject.onNext(chatConversationInfo)
            }
        }
    }

    fun bind(userInfo: NearByUserResponse) {
        this.chatConversationInfo = userInfo
        binding?.apply {
            Glide.with(context)
                .load(userInfo.avatar)
                .centerCrop()
                .placeholder(resources.getDrawable(R.drawable.ic_chat_user_placeholder, null))
                .into(ivUserProfileImage)
            tvUserName.text = if (userInfo.userType == MapVenueUserType.VENUE_OWNER.type) userInfo.name ?: "" else userInfo.username ?: ""

            ivVerified.isVisible = userInfo.profileVerified == 1
            updateFollowStatus()
        }
    }

    private fun updateFollowStatus() {
        binding?.apply {
            if (chatConversationInfo.followStatus == 1) {
                addProfile.visibility = View.GONE
            } else {
                addProfile.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}