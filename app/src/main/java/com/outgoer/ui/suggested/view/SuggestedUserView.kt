package com.outgoer.ui.suggested.view

import android.content.Context
import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.outgoer.R
import com.outgoer.api.authentication.model.OutgoerUser
import com.outgoer.api.profile.model.SuggestedUserActionState
import com.outgoer.base.extension.subscribeAndObserveOnMainThread
import com.outgoer.base.extension.throttleClicks
import com.outgoer.base.view.ConstraintLayoutWithLifecycle
import com.outgoer.databinding.SuggestedUserViewBinding
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

class SuggestedUserView(context: Context) : ConstraintLayoutWithLifecycle(context) {

    private val suggestedUserActionStateSubject: PublishSubject<SuggestedUserActionState> = PublishSubject.create()
    val suggestedUserActionState: Observable<SuggestedUserActionState> = suggestedUserActionStateSubject.hide()

    private lateinit var binding: SuggestedUserViewBinding
    private lateinit var outgoerUser: OutgoerUser

    init {
        inflateUi()
    }

    private fun inflateUi() {
        val view = View.inflate(context, R.layout.suggested_user_view, this)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        binding = SuggestedUserViewBinding.bind(view)

        binding.apply {
            btnFollow.throttleClicks().subscribeAndObserveOnMainThread {
                outgoerUser.followStatus = if (outgoerUser.followStatus == null) {
                    1
                } else {
                    if (outgoerUser.followStatus == 1) {
                        0
                    } else {
                        1
                    }
                }
                updateFollowStatus()
                suggestedUserActionStateSubject.onNext(SuggestedUserActionState.FollowButtonClick(outgoerUser))
            }

            btnFollowing.throttleClicks().subscribeAndObserveOnMainThread {
                outgoerUser.followStatus = if (outgoerUser.followStatus == null) {
                    1
                } else {
                    if (outgoerUser.followStatus == 1) {
                        0
                    } else {
                        1
                    }
                }
                updateFollowStatus()
                suggestedUserActionStateSubject.onNext(SuggestedUserActionState.FollowButtonClick(outgoerUser))
            }
        }
    }

    fun bind(outgoerUser: OutgoerUser) {
        this.outgoerUser = outgoerUser
        binding.apply {
            Glide.with(context)
                .load(outgoerUser.avatar)
                .centerCrop()
                .placeholder(R.drawable.ic_chat_user_placeholder)
                .into(ivUserProfile)

            tvUsername.text = outgoerUser.name
            nameAppCompatTextView.text = outgoerUser.username

            ivVerified.isVisible = outgoerUser.profileVerified == 1

            updateFollowStatus()
        }
    }

    private fun updateFollowStatus() {
        binding.apply {
            if (outgoerUser.followStatus == null) {
                btnFollow.visibility = View.VISIBLE
                btnFollowing.visibility = View.GONE
            } else {
                if (outgoerUser.followStatus == 1) {
                    btnFollow.visibility = View.GONE
                    btnFollowing.visibility = View.VISIBLE
                } else {
                    btnFollow.visibility = View.VISIBLE
                    btnFollowing.visibility = View.GONE
                }
            }
        }
    }
}