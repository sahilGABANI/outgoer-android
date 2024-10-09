package com.outgoer.ui.invitefriends.view

import android.content.Context
import android.view.View
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.follow.model.FollowUser
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.ViewLiveStreamInviteFriendsBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class InviteFriendsLiveStreamView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val inviteButtonViewClicksSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val inviteButtonViewClicks: Observable<FollowUser> = inviteButtonViewClicksSubject.hide()

    private val invitedButtonViewClicksSubject: PublishSubject<FollowUser> = PublishSubject.create()
    val invitedButtonViewClicks: Observable<FollowUser> = invitedButtonViewClicksSubject.hide()

    private lateinit var binding: ViewLiveStreamInviteFriendsBinding
    private lateinit var followUser: FollowUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.view_live_stream_invite_friends, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = ViewLiveStreamInviteFriendsBinding.bind(view)

        binding.apply {
            btnInvite.throttleClicks().subscribeAndObserveOnMainThread {
                followUser.isInvited = true
                inviteButtonViewClicksSubject.onNext(followUser)
                updateButtonView()
            }.autoDispose()

            btnInvited.throttleClicks().subscribeAndObserveOnMainThread {
                if (!followUser.isAlreadyInvited) {
                    followUser.isInvited = false
                    invitedButtonViewClicksSubject.onNext(followUser)
                    updateButtonView()
                }
            }.autoDispose()
        }
    }

    fun bind(followUser: FollowUser) {
        this.followUser = followUser
        binding.apply {
            tvUsername.text = followUser.username ?: ""

            Glide.with(context)
                .load(followUser.avatar ?: "")
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(ivUserProfile)

            if (followUser.isAlreadyInvited) {
                btnInvite.visibility = View.GONE
                btnInvited.visibility = View.VISIBLE
            } else {
                if (followUser.isInvited) {
                    btnInvite.visibility = View.GONE
                    btnInvited.visibility = View.VISIBLE
                } else {
                    btnInvite.visibility = View.VISIBLE
                    btnInvited.visibility = View.GONE
                }
            }
        }
    }

    private fun updateButtonView() {
        binding.apply {
            if (followUser.isInvited) {
                btnInvite.visibility = View.GONE
                btnInvited.visibility = View.VISIBLE
            } else {
                btnInvite.visibility = View.VISIBLE
                btnInvited.visibility = View.GONE
            }
        }
    }
}